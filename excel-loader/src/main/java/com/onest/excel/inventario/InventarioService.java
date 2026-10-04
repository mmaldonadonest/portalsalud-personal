package com.onest.excel.inventario;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tarea 2 del plan: recorrer la carpeta del origen y decir con certeza QUE hay.
 *
 * <p>No lee datos todavia: identifica. Es la primera cosa que hay que correr, porque de aqui
 * sale la lista real de archivos a cargar y porque destapa las sorpresas antes de invertir dias
 * en el transpositor.
 *
 * <p><b>Tres reglas que este servicio existe para aplicar</b>, cada una nacida de un problema
 * real del origen (medido el 29-sep-2026):
 * <ol>
 *   <li><b>La familia sale del texto del nombre, no del prefijo numerico.</b> En ZARA 2026 el
 *       archivo 4 es un pos-incapacidad y el 5 un reporte de incapacidades, invertidos respecto
 *       a los demas predios.</li>
 *   <li><b>El anio sale del contenido, no de la carpeta.</b> Dentro de {@code MORBILIDAD 2026/}
 *       hay nueve reportes de maternidad de 2025 y un {@code CONTROL CAPA 2025 WORLD PARK}.</li>
 *   <li><b>El formato sale de los primeros bytes, no de la extension.</b> Hay archivos con doble
 *       extension ({@code ....xls.xlsx}).</li>
 * </ol>
 */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    /** Extensiones candidatas. Cual es de verdad lo decide {@link FormatoArchivo}. */
    private static final Pattern EXTENSIONES = Pattern.compile("(?i)\\.(xls|xlsx|xlsm)$");
    private static final Pattern ANIO = Pattern.compile("(20\\d{2})");
    /** Archivos temporales que Excel deja abiertos; no son datos. */
    private static final String PREFIJO_TEMPORAL = "~$";

    /** Cuantas filas del encabezado se miran buscando el anio del contenido. */
    private static final int FILAS_BUSQUEDA_ANIO = 8;
    private static final int COLUMNAS_BUSQUEDA_ANIO = 12;

    private FuenteAnio fuenteAnio = FuenteAnio.CONTENIDO;

    /**
     * Recorre {@code raiz} de forma recursiva y devuelve un renglon por archivo de Excel.
     * Nunca lanza por un archivo malo: lo devuelve con {@code error} lleno, porque un archivo
     * corrupto no debe abortar el inventario de los otros 131.
     */
    public List<ArchivoInventariado> inventariar(Path raiz) throws IOException {
        return inventariar(raiz, FuenteAnio.CONTENIDO);
    }

    /** Cual de los tres anios manda cuando no coinciden. */
    public enum FuenteAnio { CONTENIDO, NOMBRE }

    public List<ArchivoInventariado> inventariar(Path raiz, FuenteAnio fuente) throws IOException {
        this.fuenteAnio = fuente;
        List<ArchivoInventariado> resultado = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(raiz)) {
            List<Path> archivos = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> EXTENSIONES.matcher(p.getFileName().toString()).find())
                    .filter(p -> !p.getFileName().toString().startsWith(PREFIJO_TEMPORAL))
                    .sorted()
                    .toList();
            log.info("Inventario: {} archivos de Excel bajo {}", archivos.size(), raiz);
            for (Path p : archivos) {
                resultado.add(inspeccionar(raiz, p));
            }
        }
        return resultado;
    }

    private ArchivoInventariado inspeccionar(Path raiz, Path archivo) {
        String nombre = archivo.getFileName().toString();
        String carpeta = carpetaRelativa(raiz, archivo);
        Familia familia = Familia.deNombreArchivo(nombre);
        // El anio de la carpeta sale del nombre de la RAIZ ("MORBILIDAD 2026") y, si no lo
        // trae, de cualquier tramo de la ruta. Buscarlo solo en el primer tramo relativo era
        // un error: ahi esta el predio (TOLUCA), no el anio, y el resultado era siempre null
        // -> la comprobacion de "guardado en la carpeta de otro anio" nunca detectaba nada.
        Integer anioCarpeta = anioDeRuta(raiz, archivo);
        Integer anioNombre = anioDeTexto(nombre);

        FormatoArchivo porExtension = FormatoArchivo.segunExtension(nombre);
        FormatoArchivo real;
        try {
            real = FormatoArchivo.de(archivo);
        } catch (IOException e) {
            return fallido(archivo, nombre, carpeta, familia, anioNombre, anioCarpeta,
                    FormatoArchivo.DESCONOCIDO, porExtension, "No se pudo leer la cabecera: " + e.getMessage());
        }

        if (real == FormatoArchivo.DESCONOCIDO) {
            return fallido(archivo, nombre, carpeta, familia, anioNombre, anioCarpeta,
                    real, porExtension, "No es un libro de Excel: la cabecera no es OLE2 ni ZIP");
        }

        String checksum;
        try {
            checksum = sha256(archivo);
        } catch (Exception e) {
            checksum = null;
        }

        // Abrir el libro: de aqui salen las hojas y el anio del contenido
        List<String> hojas = new ArrayList<>();
        Integer anioContenido = null;
        try (Workbook libro = WorkbookFactory.create(archivo.toFile(), null, true)) {
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                hojas.add(libro.getSheetName(i));
            }
            anioContenido = anioDelContenido(libro);
        } catch (Exception e) {
            return fallido(archivo, nombre, carpeta, familia, anioNombre, anioCarpeta,
                    real, porExtension, e.getClass().getSimpleName() + ": " + resumir(e.getMessage()));
        }

        Integer anio = resolverAnio(anioContenido, anioNombre, anioCarpeta);

        return new ArchivoInventariado(archivo, nombre, carpeta, familia, anio,
                anioContenido, anioNombre, anioCarpeta,
                real, porExtension, checksum, List.copyOf(hojas), null);
    }

    /**
     * Cual de los tres anios gana. La carpeta es siempre el ultimo recurso: es la menos
     * confiable del origen.
     *
     * <p>Entre contenido y nombre no hay una respuesta universal, y por eso es configurable:
     * los nueve reportes de maternidad guardados en {@code MORBILIDAD 2026} dicen 2025 en el
     * nombre y en el contenido (el contenido acierta), pero los siete de NOM-035 se llaman 2026
     * y su titulo dice 2025 (ahi no esta claro cual acierta). El inventario reporta el conflicto
     * para que se decida con datos.
     */
    private Integer resolverAnio(Integer contenido, Integer nombre, Integer carpeta) {
        Integer primero = fuenteAnio == FuenteAnio.NOMBRE ? nombre : contenido;
        Integer segundo = fuenteAnio == FuenteAnio.NOMBRE ? contenido : nombre;
        if (primero != null) {
            return primero;
        }
        return segundo != null ? segundo : carpeta;
    }

    private ArchivoInventariado fallido(Path ruta, String nombre, String carpeta, Familia familia,
                                        Integer anio, Integer anioCarpeta, FormatoArchivo real,
                                        FormatoArchivo ext, String error) {
        log.warn("No se pudo inventariar {}: {}", nombre, error);
        return new ArchivoInventariado(ruta, nombre, carpeta, familia, anio,
                null, anio, anioCarpeta, real, ext, null, List.of(), error);
    }

    /** Busca un anio en cualquier tramo de la ruta, empezando por la carpeta raiz. */
    private Integer anioDeRuta(Path raiz, Path archivo) {
        Integer deRaiz = anioDeTexto(String.valueOf(raiz.getFileName()));
        if (deRaiz != null) {
            return deRaiz;
        }
        Path rel = raiz.relativize(archivo);
        for (int i = 0; i < rel.getNameCount() - 1; i++) {
            Integer a = anioDeTexto(rel.getName(i).toString());
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    /**
     * Busca un anio en las primeras filas de las primeras hojas.
     *
     * <p>Los archivos lo ponen en el bloque de titulo: la hoja de atenciones de 2026 trae
     * {@code REGISTRO DIARIO DE ATENCIONES} y debajo el anio. Se acepta cualquier celda que sea
     * un 20xx, ya venga como texto o como numero.
     *
     * <p>Se ignoran los anios que vengan dentro de una fecha completa: una fecha de captura no
     * dice de que anio es el reporte.
     */
    private Integer anioDelContenido(Workbook libro) {
        int hojasARevisar = Math.min(3, libro.getNumberOfSheets());
        for (int h = 0; h < hojasARevisar; h++) {
            Sheet hoja = libro.getSheetAt(h);
            int ultima = Math.min(hoja.getLastRowNum(), FILAS_BUSQUEDA_ANIO - 1);
            for (int r = 0; r <= ultima; r++) {
                Row fila = hoja.getRow(r);
                if (fila == null) {
                    continue;
                }
                int hastaCol = Math.min(fila.getLastCellNum(), COLUMNAS_BUSQUEDA_ANIO);
                for (int c = 0; c < hastaCol; c++) {
                    Integer anio = anioDeCelda(fila.getCell(c));
                    if (anio != null) {
                        return anio;
                    }
                }
            }
        }
        return null;
    }

    private Integer anioDeCelda(Cell celda) {
        if (celda == null) {
            return null;
        }
        try {
            return switch (celda.getCellType()) {
                case NUMERIC -> {
                    // Una fecha completa no sirve: es captura, no el anio del reporte
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(celda)) {
                        yield null;
                    }
                    double d = celda.getNumericCellValue();
                    int i = (int) d;
                    yield (d == i && i >= 2015 && i <= 2035) ? i : null;
                }
                case STRING -> anioDeTexto(celda.getStringCellValue());
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Integer anioDeTexto(String s) {
        if (s == null) {
            return null;
        }
        Matcher m = ANIO.matcher(s);
        while (m.find()) {
            int anio = Integer.parseInt(m.group(1));
            if (anio >= 2015 && anio <= 2035) {
                return anio;
            }
        }
        return null;
    }

    private String carpetaRelativa(Path raiz, Path archivo) {
        Path rel = raiz.relativize(archivo);
        return rel.getNameCount() > 1 ? rel.getName(0).toString() : "";
    }

    private String sha256(Path archivo) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = Files.newInputStream(archivo)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                md.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private String resumir(String mensaje) {
        if (mensaje == null) {
            return "(sin mensaje)";
        }
        return mensaje.length() > 120 ? mensaje.substring(0, 120) + "..." : mensaje;
    }
}
