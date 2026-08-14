package com.onest.app.catalog.file.etl;

/**
 * Deteccion de extension por firma binaria. Respaldo barato para los pocos
 * casos legacy con nombre truncado/sin extension (ver plan seccion 2, "conclusiones
 * del perfilado" punto 3: solo 3 de 21,089). El nombre sigue siendo la fuente
 * primaria; esto solo se consulta si el nombre no trae extension.
 */
final class MagicBytes {

    private MagicBytes() {
    }

    static String sniffExtension(byte[] content) {
        if (content == null || content.length < 4) {
            return null;
        }
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46)) {
            return "pdf";
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47)) {
            return "png";
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(content, 0x50, 0x4B, 0x03, 0x04)) {
            return "docx";
        }
        return null;
    }

    private static boolean startsWith(byte[] content, int... expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
