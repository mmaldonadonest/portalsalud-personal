package com.onest.app.catalog.file.storage;

/**
 * Resultado de persistir un binario: ruta relativa a la raiz, checksum y tamano.
 * {@code storagePath} es RELATIVO (portable si cambia la raiz por entorno).
 */
public record StoredBinary(
        String storagePath,
        String checksumSha256,
        long sizeBytes,
        String storageProvider
) {
}
