package com.onest.excel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.onest.excel.inventario.ArchivoInventariado;
import com.onest.excel.inventario.InventarioReporte;
import com.onest.excel.inventario.InventarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Carga de los Excel de MORBILIDAD 2026 al portal. Proceso de consola: corre y termina.
 *
 * <p><b>La autoconfiguracion del DataSource esta excluida a proposito.</b> El inventario solo
 * lee archivos del disco y no tiene por que poder conectarse a Oracle: asi es imposible que una
 * corrida de reconocimiento toque la base. La conexion se declara explicitamente y solo la usan
 * los modos que escriben.
 *
 * <p>Nada se ejecuta solo. Con {@code --excel.modo=none} (el default) arranca, no hace nada y
 * termina. Es la misma disciplina del proyecto {@code etl/}.
 *
 * <pre>
 *   # Reconocimiento: que hay en la carpeta. No toca la base.
 *   java -jar excel-loader/target/portal-salud-excel-loader.jar \
 *        --excel.modo=inventario \
 *        --excel.origen="C:/Users/Miguel/Downloads/MORBILIDAD/MORBILIDAD 2026"
 * </pre>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ExcelLoaderApplication {

    private static final Logger log = LoggerFactory.getLogger(ExcelLoaderApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ExcelLoaderApplication.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Bean
    CommandLineRunner runner(InventarioService inventario,
                             org.springframework.beans.factory.ObjectProvider<com.onest.excel.carga.CargaService> cargaProvider,
                             org.springframework.beans.factory.ObjectProvider<com.onest.excel.persistencia.BitacoraRepository> repoProvider,
                             @Value("${excel.modo:none}") String modo,
                             @Value("${excel.origen:}") String origen,
                             @Value("${excel.anio:2026}") int anio,
                             @Value("${excel.anio.fuente:contenido}") String fuenteAnio) {
        return args -> {
            String m = modo == null ? "none" : modo.trim().toLowerCase();

            if ("none".equals(m)) {
                log.info("excel.modo=none: no hay nada que hacer.");
                log.info("Modos: inventario · analizar (no tocan la base) · sample · full (escriben)");
                return;
            }

            if (origen == null || origen.isBlank()) {
                log.error("Falta --excel.origen con la carpeta a recorrer.");
                return;
            }
            Path raiz = Path.of(origen);
            if (!Files.isDirectory(raiz)) {
                log.error("No es una carpeta: {}", raiz);
                return;
            }

            switch (m) {
                case "inventario" -> {
                    InventarioService.FuenteAnio fuente = "nombre".equalsIgnoreCase(fuenteAnio)
                            ? InventarioService.FuenteAnio.NOMBRE
                            : InventarioService.FuenteAnio.CONTENIDO;
                    log.info("Inventariando {} (anio objetivo {}, manda el {})", raiz, anio, fuente);
                    List<ArchivoInventariado> archivos = inventario.inventariar(raiz, fuente);
                    // Va por System.out y no por el log: es un reporte para leer, no una traza
                    System.out.println(InventarioReporte.generar(archivos, anio));
                }
                case "analizar" -> {
                    log.info("Analizando el layout de {} (anio {})", raiz, anio);
                    List<ArchivoInventariado> archivos = inventario.inventariar(raiz);
                    System.out.println(com.onest.excel.layout.AnalisisLayout.generar(archivos, anio));
                }
                case "sample", "full" -> {
                    var carga = cargaProvider.getIfAvailable();
                    var repo = repoProvider.getIfAvailable();
                    if (carga == null || repo == null) {
                        log.error("Los modos sample y full escriben en la base y falta la conexion.");
                        log.error("Definir EXCEL_TARGET_URL, EXCEL_TARGET_USER y EXCEL_TARGET_PASSWORD.");
                        return;
                    }
                    // Si falta una tabla se corta aqui, antes de leer un solo archivo
                    repo.verificarEsquema();

                    boolean muestra = "sample".equals(m);
                    log.info("Carga {} sobre {} (anio {})", m, raiz, anio);
                    List<ArchivoInventariado> archivos = inventario.inventariar(raiz);
                    var enAlcance = archivos.stream()
                            .filter(ArchivoInventariado::abrio)
                            .filter(a -> a.esDelAnio(anio))
                            .filter(a -> a.familia().enAlcance())
                            .toList();
                    log.info("{} archivos en alcance; familias configuradas: {}",
                            enAlcance.size(), com.onest.excel.carga.MapeoFamilia.configuradas());

                    var resultados = new java.util.ArrayList<com.onest.excel.carga.CargaService.Resultado>();
                    for (ArchivoInventariado a : enAlcance) {
                        resultados.addAll(carga.cargar(a, muestra));
                    }
                    System.out.println(com.onest.excel.carga.CargaReporte.generar(
                            resultados, repo.conteos(), muestra));

                    // Una corrida que no inserto nada porque todo estaba ya cargado NO es un
                    // exito. El 30-sep-2026 el full se corrio sin vaciar QA, se saltaron las 66
                    // hojas, y tanto el reporte como el .bat remataron con "[ok]". Sale con
                    // codigo 2 para que el bat lo diga.
                    if (com.onest.excel.carga.CargaReporte.noHizoNada(resultados)) {
                        log.error("No se inserto ningun renglon: las hojas ya estaban cargadas.");
                        log.error("Vaciar con docs/script-prod/excel-99-reversa-bitacora.sql (v_forzar := 'S').");
                        System.exit(2);
                    }
                    // Una hoja caida tampoco es un exito, aunque las demas hayan entrado. Se
                    // puede volver a correr tal cual: el lote incompleto se limpia solo.
                    long fallidas = com.onest.excel.carga.CargaReporte.hojasConError(resultados);
                    if (fallidas > 0) {
                        log.error("{} hoja(s) fallaron. Ver la seccion CON ERROR del reporte.", fallidas);
                        log.error("Corregido el problema, volver a correr: solo se reintentan esas.");
                        System.exit(3);
                    }
                }
                default -> log.error(
                        "Modo desconocido: '{}'. Validos: none, inventario, analizar, sample, full", m);
            }
        };
    }
}
