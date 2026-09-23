package com.onest.app.catalog.file.storage;

import java.time.LocalDate;

/**
 * Puerto de almacenamiento de binarios (ports-and-adapters del contrato de
 * arquitectura). La implementacion decide donde/como persistir; el dominio solo
 * conoce esta abstraccion. Hoy: {@link FilesystemStorageProvider}.
 */
public interface StorageProvider {

    /** Persiste el contenido (sharding por fecha de hoy) y devuelve su ubicacion relativa + checksum + tamano. */
    default StoredBinary store(byte[] content, String extension) {
        return store(content, extension, LocalDate.now());
    }

    /** Persiste el contenido bajo el sharding de una fecha dada (migraciones historicas: {@code files.date_upload}). */
    StoredBinary store(byte[] content, String extension, LocalDate fecha);

    /** Lee el binario dado su storagePath relativo. */
    byte[] read(String storagePath);

    /** Elimina el binario fisico. Devuelve true si existia y se borro. */
    boolean delete(String storagePath);

    /** Identificador del proveedor (se guarda en los metadatos). */
    String providerName();
}
