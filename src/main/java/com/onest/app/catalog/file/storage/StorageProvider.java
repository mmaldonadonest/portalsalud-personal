package com.onest.app.catalog.file.storage;

/**
 * Puerto de almacenamiento de binarios (ports-and-adapters del contrato de
 * arquitectura). La implementacion decide donde/como persistir; el dominio solo
 * conoce esta abstraccion. Hoy: {@link FilesystemStorageProvider}.
 */
public interface StorageProvider {

    /** Persiste el contenido y devuelve su ubicacion relativa + checksum + tamano. */
    StoredBinary store(byte[] content, String extension);

    /** Lee el binario dado su storagePath relativo. */
    byte[] read(String storagePath);

    /** Elimina el binario fisico. Devuelve true si existia y se borro. */
    boolean delete(String storagePath);

    /** Identificador del proveedor (se guarda en los metadatos). */
    String providerName();
}
