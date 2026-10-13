package com.onest.app.ayuda;

import java.time.LocalDateTime;

/** Fila de SERV_MED_AYUDA_ARTICULO. {@code cuerpo} es Markdown; {@code modulo} es un CODE de SERV_MED_MENU o GENERAL. */
public record AyudaArticulo(long id, String clave, String modulo, String titulo, String pregunta, String palabrasClave,
                            String cuerpo, String enlace, int orden, boolean activo,
                            LocalDateTime updatedAt, String updatedBy) {

    public static final String GENERAL = "GENERAL";
}
