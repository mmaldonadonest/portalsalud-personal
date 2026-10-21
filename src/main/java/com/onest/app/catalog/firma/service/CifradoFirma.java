package com.onest.app.catalog.firma.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifrado de las firmas digitalizadas (AES-256-GCM, 20-oct-2026).
 *
 * <p>La llave sale de la variable de ambiente PORTAL_FIRMAS_LLAVE (Spring la expone como
 * portal.firmas.llave), decision del usuario: nunca en el WAR, el repo ni un properties. NO tiene valor
 * por omision a proposito (regla "cuidado con las configuraciones"): si falta o no son 32 bytes en
 * Base64, el portal arranca normal con las firmas APAGADAS (los impresos salen con la linea para firmar
 * a mano) y el log lo avisa sin mostrar la llave.
 *
 * <p>Formato guardado: [12 bytes IV][texto cifrado + etiqueta GCM]. El usuario due&ntilde;o va como dato
 * asociado (AAD): un archivo cifrado de una persona no se puede hacer pasar por el de otra.
 */
@Component
public class CifradoFirma {

    private static final Logger log = LoggerFactory.getLogger(CifradoFirma.class);
    private static final int IV = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey llave;
    private final SecureRandom azar = new SecureRandom();

    public CifradoFirma(@Value("${portal.firmas.llave:}") String llaveBase64) {
        this.llave = leer(llaveBase64);
        if (llave == null) {
            log.warn("[firmas] sin llave valida en PORTAL_FIRMAS_LLAVE: las firmas digitalizadas quedan APAGADAS");
        } else {
            log.info("[firmas] llave de cifrado cargada desde PORTAL_FIRMAS_LLAVE: firmas digitalizadas disponibles");
        }
    }

    private static SecretKey leer(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] b = Base64.getDecoder().decode(base64.trim());
            return b.length == 32 ? new SecretKeySpec(b, "AES") : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** true si hay llave valida; sin ella no se registra ni se imprime ninguna firma. */
    public boolean habilitado() {
        return llave != null;
    }

    public byte[] cifrar(byte[] claro, String usuario) {
        exigirLlave();
        try {
            byte[] iv = new byte[IV];
            azar.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(TAG_BITS, iv));
            c.updateAAD(aad(usuario));
            byte[] cifrado = c.doFinal(claro);
            return ByteBuffer.allocate(IV + cifrado.length).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar la firma", ex);
        }
    }

    public byte[] descifrar(byte[] guardado, String usuario) {
        exigirLlave();
        if (guardado == null || guardado.length <= IV) {
            throw new IllegalStateException("Archivo de firma vacio o danado");
        }
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(TAG_BITS, guardado, 0, IV));
            c.updateAAD(aad(usuario));
            return c.doFinal(guardado, IV, guardado.length - IV);
        } catch (GeneralSecurityException ex) {
            // llave distinta, archivo alterado o de otro usuario: se trata como "sin firma"
            throw new IllegalStateException("No se pudo descifrar la firma", ex);
        }
    }

    private void exigirLlave() {
        if (llave == null) {
            throw new IllegalStateException("Firmas apagadas: falta la llave PORTAL_FIRMAS_LLAVE");
        }
    }

    private static byte[] aad(String usuario) {
        return ("firma:" + (usuario == null ? "" : usuario.trim())).getBytes(StandardCharsets.UTF_8);
    }
}
