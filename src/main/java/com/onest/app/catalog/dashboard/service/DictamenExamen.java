package com.onest.app.catalog.dashboard.service;

import java.util.Optional;

/**
 * Los cuatro dictamenes de examen medico, y como se leen las etiquetas del Excel.
 *
 * <p>Existe porque el mismo concepto llega por dos caminos con nombres distintos. El WS de ORDS
 * entrega cuatro columnas ({@code APTO}, {@code NO_APTO}, {@code APTO_CONDICIONADO},
 * {@code APTO_RESTRINGIDO}) y los Excel del servicio medico entregan una sola columna marcada
 * dentro de un bloque cuyas etiquetas son {@code APTO (15)}, {@code CONDICIONADO (45)},
 * {@code NO APTO (30)} e {@code INCLUSION (60)}. La pantalla pinta los dos origenes en la misma
 * barra apilada, asi que la equivalencia tiene que estar en un solo lugar.
 *
 * <p><b>La regla de negocio, confirmada por servicio medico el 29-sep-2026:</b>
 * <ul>
 *   <li>{@code 15} apto</li>
 *   <li>{@code 45} apto condicionado</li>
 *   <li>{@code 60} apto restringido, que en los archivos se escribe <b>INCLUSION</b></li>
 *   <li>{@code 30} no apto</li>
 * </ul>
 *
 * <p><b>Solo el 30 descalifica.</b> El 60 es apto: &laquo;inclusion&raquo; no es una categoria de
 * personal, es el dictamen de quien entra con una restriccion. Una lista de no aptos se filtra
 * por {@code = 30}, nunca por &laquo;distinto de 15&raquo;, porque eso meteria a los
 * condicionados y a los de inclusion en la misma bolsa que los rechazados.
 */
enum DictamenExamen {

    APTO,
    NO_APTO,
    CONDICIONADO,
    RESTRINGIDO;

    /**
     * El dictamen que corresponde a una etiqueta de los Excel.
     *
     * <p>Se reconoce por el <b>numero entre parentesis</b> y no por el texto. El numero es el
     * codigo real del catalogo y no cambia; el texto si cambia de redaccion entre anios y entre
     * predios. Si llega algo que no trae un codigo conocido se devuelve vacio, para que quien
     * llame lo cuente aparte en lugar de forzarlo al bucket equivocado.
     */
    static Optional<DictamenExamen> deEtiquetaExcel(String etiqueta) {
        if (etiqueta == null) {
            return Optional.empty();
        }
        String e = etiqueta.toUpperCase(java.util.Locale.ROOT);
        if (e.contains("(15)")) {
            return Optional.of(APTO);
        }
        if (e.contains("(30)")) {
            return Optional.of(NO_APTO);
        }
        if (e.contains("(45)")) {
            return Optional.of(CONDICIONADO);
        }
        if (e.contains("(60)")) {
            return Optional.of(RESTRINGIDO);
        }
        // Sin codigo: se intenta por texto, que es el caso de un archivo viejo o mal capturado.
        // El orden importa: "NO APTO" tiene que probarse antes que "APTO".
        if (e.contains("NO APTO")) {
            return Optional.of(NO_APTO);
        }
        if (e.contains("CONDICIONADO")) {
            return Optional.of(CONDICIONADO);
        }
        if (e.contains("INCLUSION") || e.contains("RESTRINGIDO")) {
            return Optional.of(RESTRINGIDO);
        }
        if (e.contains("APTO")) {
            return Optional.of(APTO);
        }
        return Optional.empty();
    }
}
