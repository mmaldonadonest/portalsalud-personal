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
                             @Value("${excel.anio.fuente:contenido}") String fuenteAnio,
                             org.springframework.beans.factory.ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> jdbcProvider,
                             org.springframework.beans.factory.ObjectProvider<javax.sql.DataSource> dsProvider,
                             @Value("${excel.nss.forzar:false}") boolean nssForzar,
                             @Value("${excel.nss.salida:}") String nssSalida,
                             @Value("${excel.nss.desde:2026-01-01}") String nssDesde,
                             @Value("${excel.nss.hasta:2026-06-30}") String nssHasta,
                             @Value("${excel.nss.biometrico:}") String nssBiometrico) {
        return args -> {
            String m = modo == null ? "none" : modo.trim().toLowerCase();

            if ("none".equals(m)) {
                log.info("excel.modo=none: no hay nada que hacer.");
                log.info("Modos: inventario | analizar (no tocan la base) | sample | full (escriben)");
                log.info("Layouts por NSS: nss-validar (no escribe) | nss-cargar (escribe en SERV_MED_XLS_*)");
                log.info("Pruebas internas: nss-generar-prueba (lee la carga vieja, escribe .xlsx de prueba)");
                log.info("                  nss-generar-excel (lee Excel originales sin base, escribe .xlsx de prueba)");
                return;
            }

            // SOLO PRUEBAS INTERNAS: genera layouts desde la carga vieja (no se entregan al servicio
            // medico, que es la unica fuente de verdad). Lee la base; escribe solo archivos .xlsx.
            if ("nss-generar-prueba".equals(m)) {
                var jdbc = jdbcProvider.getIfAvailable();
                if (jdbc == null) {
                    log.error("nss-generar-prueba lee la carga vieja y el biometrico: falta la conexion.");
                    return;
                }
                if (nssSalida == null || nssSalida.isBlank()) {
                    log.error("Falta --excel.nss.salida con la carpeta donde dejar los .xlsx de prueba.");
                    return;
                }
                java.time.LocalDate desde = java.time.LocalDate.parse(nssDesde);
                java.time.LocalDate hasta = java.time.LocalDate.parse(nssHasta);
                var res = new com.onest.excel.nss.PrellenadoNss(jdbc).generar(Path.of(nssSalida), desde, hasta);
                System.out.println(com.onest.excel.nss.PrellenadoNss.reporte(res));
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
                // Layouts nuevos por NSS (10-oct-2026). nss-validar no escribe nunca; si hay
                // conexion la usa SOLO para leer el biometrico. nss-cargar escribe.
                case "nss-validar", "nss-cargar" -> {
                    boolean escribir = "nss-cargar".equals(m);
                    var jdbc = jdbcProvider.getIfAvailable();
                    var ds = dsProvider.getIfAvailable();
                    if (escribir && (jdbc == null || ds == null)) {
                        log.error("nss-cargar escribe en la base y falta la conexion.");
                        log.error("Definir EXCEL_TARGET_URL, EXCEL_TARGET_USER y EXCEL_TARGET_PASSWORD.");
                        return;
                    }
                    var repo = escribir ? new com.onest.excel.nss.XlsNssRepository(jdbc, ds) : null;
                    if (repo != null) {
                        repo.verificarEsquema();   // si falta una tabla, se corta antes de leer
                    }
                    var bio = jdbc == null ? null : new com.onest.excel.nss.BiometricoNss(jdbc);
                    Path salida = nssSalida == null || nssSalida.isBlank() ? null : Path.of(nssSalida);
                    if (salida != null) {
                        Files.createDirectories(salida);
                    }
                    var servicio = new com.onest.excel.nss.CargaNssService(bio, repo, salida);
                    var lista = com.onest.excel.nss.CargaNssService.archivos(raiz);
                    log.info("{} archivo(s) .xlsx en {} | modo {}", lista.size(), raiz, m);
                    var resultados = new java.util.ArrayList<com.onest.excel.nss.CargaNssService.ResultadoArchivo>();
                    for (Path p : lista) {
                        resultados.add(servicio.procesar(p, escribir, nssForzar));
                    }
                    System.out.println(com.onest.excel.nss.ReporteNss.generar(resultados, escribir,
                            repo == null ? null : repo.conteos()));
                    boolean hayRechazos = resultados.stream().anyMatch(r -> r.motivoRechazo() != null);
                    boolean hayErrores = resultados.stream().anyMatch(r -> !r.errores().isEmpty());
                    if (hayRechazos || hayErrores) {
                        System.exit(4);   // el .bat avisa: hay renglones o archivos que no entraron
                    }
                }
                // SOLO PRUEBAS INTERNAS (12-oct-2026): layouts por NSS desde los Excel ORIGINALES de
                // un predio y un periodo (prueba TOLUCA enero 2025). No toca la base: el lector de la
                // carga vieja escribe en memoria y el NSS sale de un CSV del biometrico que el usuario
                // exporta con docs/script-prod/excel-nss-10-biometrico-nombres.sql.
                case "nss-generar-excel" -> {
                    if (nssSalida == null || nssSalida.isBlank()) {
                        log.error("Falta --excel.nss.salida con la carpeta donde dejar los .xlsx de prueba.");
                        return;
                    }
                    // Los .xlsm de 2025 traen hojas muy repetitivas y POI los toma por "zip bomb".
                    // Son archivos locales del propio servicio medico: se relaja el umbral solo aqui.
                    org.apache.poi.openxml4j.util.ZipSecureFile.setMinInflateRatio(0.0);
                    var memoria = new com.onest.excel.persistencia.BitacoraMemoria();
                    var lector = new com.onest.excel.carga.CargaService(memoria);
                    var enAlcance = inventario.inventariar(raiz).stream()
                            .filter(ArchivoInventariado::abrio)
                            .filter(a -> a.esDelAnio(anio))
                            .filter(a -> a.familia().enAlcance())
                            .toList();
                    for (ArchivoInventariado a : enAlcance) {
                        for (var r : lector.cargar(a, false)) {
                            log.info("[leer] {} | hoja {} | {} renglones{}", r.archivo(), r.hoja(), r.leidas(),
                                    r.error() == null ? "" : " | " + r.error());
                        }
                    }
                    Path dirSalida = Path.of(nssSalida);
                    Files.createDirectories(dirSalida);
                    java.time.LocalDate desde = java.time.LocalDate.parse(nssDesde);
                    java.time.LocalDate hasta = java.time.LocalDate.parse(nssHasta);
                    Path volcado = com.onest.excel.nss.PrellenadoNss.volcar(memoria, dirSalida, desde, hasta);
                    log.info("Lo leido de los Excel originales (para revisar contra el archivo): {}", volcado);
                    if (nssBiometrico == null || nssBiometrico.isBlank()) {
                        log.warn("Sin --excel.nss.biometrico: solo se volco lo leido; no se generan layouts.");
                        return;
                    }
                    var res = com.onest.excel.nss.PrellenadoNss.desdeMemoria(memoria, Path.of(nssBiometrico))
                            .generarTodos(dirSalida, desde, hasta);
                    System.out.println(com.onest.excel.nss.PrellenadoNss.reporte(res));
                }
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
                        "Modo desconocido: '{}'. Validos: none, inventario, analizar, sample, full, nss-validar, nss-cargar, nss-generar-excel", m);
            }
        };
    }
}
