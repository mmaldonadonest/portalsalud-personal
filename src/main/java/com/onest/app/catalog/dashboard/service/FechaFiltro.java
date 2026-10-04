package com.onest.app.catalog.dashboard.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Convierte las fechas del filtro de pantalla a {@link LocalDate}, para poder acotar las
 * consultas al historico cargado de los Excel del servicio medico.
 *
 * <p>Vive aparte porque la necesitan los dos servicios que fusionan historico &mdash;consultas y
 * musculoesqueleticas&mdash; y tenerla duplicada garantizaba que algun dia difirieran.
 *
 * <p>Acepta los tres formatos que circulan: el filtro de la pantalla manda {@code yyyy-MM-dd} y
 * los WS de ORDS hablan {@code d/M/yy}, a veces con el anio de cuatro digitos. Son los mismos
 * que reconoce {@code mesDe} en cada servicio; la diferencia es que ahi se queria el mes y aqui
 * la fecha completa.
 *
 * <p>Ante algo que no reconoce devuelve vacio en vez de lanzar: una fecha ilegible significa
 * "sin corte por ese extremo", no que la pantalla falle.
 */
final class FechaFiltro {

    private FechaFiltro() {
    }

    static Optional<LocalDate> aFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return Optional.empty();
        }
        String valor = fecha.trim();
        try {
            if (valor.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                return Optional.of(LocalDate.parse(valor.substring(0, 10)));
            }
            if (valor.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
                return Optional.of(LocalDate.parse(valor, DateTimeFormatter.ofPattern("d/M/yyyy")));
            }
            if (valor.matches("^\\d{1,2}/\\d{1,2}/\\d{2}$")) {
                return Optional.of(LocalDate.parse(valor, DateTimeFormatter.ofPattern("d/M/yy")));
            }
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
