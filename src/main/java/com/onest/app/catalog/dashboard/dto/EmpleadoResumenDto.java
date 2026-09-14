package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * Expediente unificado de UNA persona para el modulo Empleados del dashboard analitico:
 * ficha + contadores de cada dominio + timeline consolidado. Se arma en Java cruzando los
 * servicios por NSS que ya existen (consultas, incapacidades, accidentes, antidoping,
 * maternidad, examenes) - cero WS nuevo.
 */
public record EmpleadoResumenDto(
        Ficha ficha,
        Contadores contadores,
        List<Evento> timeline
) {

    public record Ficha(
            String nss,
            String nombreCompleto,
            String cuenta,
            String predio,
            String puesto,
            String empresa,
            String sexo,
            String fechaNacimiento,
            Integer edad,
            String estadoCivil,
            String rfc,
            String turno,
            String celular,
            String telFijo,
            String direccion
    ) {
    }

    public record Contadores(
            long atenciones,
            long incapacidades,
            long diasPerdidos,
            long accidentes,
            long examenes,
            long antidoping,
            long maternidad
    ) {
    }

    /** Un evento del timeline. {@code fecha} en ISO yyyy-MM-dd para ordenar; {@code dominio} para el color. */
    public record Evento(String fecha, String dominio, String titulo, String detalle) {
    }
}
