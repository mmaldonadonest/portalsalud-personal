package com.onest.excel.inventario;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Formato REAL de un archivo, leido de sus primeros bytes y no de la extension.
 *
 * <p>La extension miente con frecuencia en este origen: hay archivos llamados
 * {@code ...2025 CHARCON.xls.xlsx} (doble extension) y en la migracion del legacy aparecieron
 * {@code .xls} que en realidad eran ZIP. Confiar en la extension y pasarle el archivo al lector
 * equivocado produce un error confuso a mitad de una corrida de horas.
 *
 * <p>Verificado el 29-sep-2026 sobre los 132 archivos de 2026: ninguna extension miente ahi
 * (60 {@code .xls} son OLE2 de verdad, 64 {@code .xlsx} y 8 {@code .xlsm} son ZIP). Se comprueba
 * igual, porque es barato y porque el origen sigue creciendo.
 */
public enum FormatoArchivo {

    /** OLE2 / BIFF8: los .xls de Excel 97-2003. Los lee HSSF. */
    OLE2,
    /** ZIP: .xlsx y .xlsm (OOXML). Los lee XSSF. */
    OOXML,
    /** Ni uno ni otro: no es un libro de Excel. */
    DESCONOCIDO;

    private static final byte[] FIRMA_OLE2 =
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] FIRMA_ZIP = {0x50, 0x4B, 0x03, 0x04};

    public static FormatoArchivo de(Path archivo) throws IOException {
        byte[] cabecera = new byte[8];
        int leidos;
        try (InputStream in = Files.newInputStream(archivo)) {
            leidos = in.readNBytes(cabecera, 0, 8);
        }
        if (leidos >= 8 && Arrays.equals(cabecera, FIRMA_OLE2)) {
            return OLE2;
        }
        if (leidos >= 4 && Arrays.equals(Arrays.copyOf(cabecera, 4), FIRMA_ZIP)) {
            return OOXML;
        }
        return DESCONOCIDO;
    }

    /** Lo que la extension del nombre <em>promete</em>, para poder contrastarlo con la realidad. */
    public static FormatoArchivo segunExtension(String nombre) {
        String n = nombre.toLowerCase();
        if (n.endsWith(".xls")) {
            return OLE2;
        }
        if (n.endsWith(".xlsx") || n.endsWith(".xlsm")) {
            return OOXML;
        }
        return DESCONOCIDO;
    }
}
