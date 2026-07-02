package com.onest.app.catalog.nss.dto;

/**
 * Datos del empleado/usuario devueltos por la busqueda por NSS (rama "con registro").
 * Mapea los campos que searchEmploye.php extrae de $data->Datos e incluye la edad
 * calculada a partir del RFC (misma logica que examenMedico en php-old).
 */
public record EmpleadoDto(
        String nss,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String rfc,
        String cuenta,
        String fechaNacimiento,
        String estadoCivil,
        String sexo,
        String direccion,
        String celular,
        String turno,
        String telFijo,
        String completo,
        String nombrePuesto,
        String nombreEmpresa,
        Integer edad
) {
}
