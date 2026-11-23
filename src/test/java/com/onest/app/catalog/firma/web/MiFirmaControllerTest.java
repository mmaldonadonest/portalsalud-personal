package com.onest.app.catalog.firma.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validacion del data URL de la firma dibujada (CVE-2026-114938, «Java4Shell»).
 *
 * <p>Por que existe esta prueba: {@code desdeDataUrl} es la unica ruta del portal donde quien manda
 * la peticion controla por completo el largo Y el contenido de lo que entra a
 * {@code java.util.Base64}. El decodificador intrinseco de HotSpot calcula mal la mascara de
 * escritura del ultimo grupo cuando lleva relleno, asi que lo que cierra la ruta es que el relleno
 * {@code =} solo pueda estar al final y bien formado. Antes del 23-nov-2026 lo unico que se
 * revisaba era el prefijo {@code data:image/png;base64,}, que el propio cliente escribe.
 *
 * <p><b>Si alguien quita la llamada a la validacion, estas pruebas fallan.</b> Eso es a proposito:
 * la contencion a nivel de aplicacion no se puede perder en una refactorizacion, porque el arreglo
 * de fondo (JDK 21.0.13.1) depende de cada servidor y no esta garantizado en todos.
 */
class MiFirmaControllerTest {

    private static final String PREFIJO = "data:image/png;base64,";

    /** Cabecera de un PNG real, que es con lo que empieza canvas.toDataURL('image/png'). */
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
        0, 0, 0, 0x0D, 'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0
    };

    private static String dataUrl(String base64) {
        return PREFIJO + base64;
    }

    // ---------------------------------------------------------------- lo que debe pasar

    @Test
    void shouldAcceptRealPngFromCanvas() {
        String base64 = Base64.getEncoder().encodeToString(PNG);
        byte[] decodificado = MiFirmaController.desdeDataUrl(dataUrl(base64));
        assertArrayEquals(PNG, decodificado, "el PNG del recuadro debe sobrevivir el ida y vuelta");
    }

    @Test
    void shouldAcceptEveryPaddingLength() {
        // "abc" no deja relleno, "ab" deja uno y "a" deja dos: los tres casos validos
        for (String claro : new String[] {"abc", "ab", "a"}) {
            String base64 = Base64.getEncoder().encodeToString(claro.getBytes());
            byte[] decodificado = MiFirmaController.desdeDataUrl(dataUrl(base64));
            assertArrayEquals(claro.getBytes(), decodificado, "fallo con relleno de: " + claro);
        }
    }

    @Test
    void shouldAcceptInputExactlyAtTheLimit() {
        String alLimite = "A".repeat(MiFirmaController.DIBUJO_MAX_CHARS);
        // no debe lanzar: el tope es inclusivo
        MiFirmaController.desdeDataUrl(dataUrl(alLimite));
    }

    // ---------------------------------------------------------------- lo que debe rechazarse

    @Test
    void shouldRejectNullAndMissingPrefix() {
        assertEquals("Dibuja tu firma en el recuadro antes de guardar.",
                assertThrows(IllegalArgumentException.class,
                        () -> MiFirmaController.desdeDataUrl(null)).getMessage());

        assertEquals("Dibuja tu firma en el recuadro antes de guardar.",
                assertThrows(IllegalArgumentException.class,
                        () -> MiFirmaController.desdeDataUrl("iVBORw0KGgo=")).getMessage());

        // prefijo de JPG: el recuadro siempre manda PNG
        assertThrows(IllegalArgumentException.class,
                () -> MiFirmaController.desdeDataUrl("data:image/jpeg;base64,iVBORw0KGgo="));
    }

    @Test
    void shouldRejectEmptyDrawingWithItsOwnMessage() {
        // el recuadro sin dibujar no es "demasiado grande": tiene que decir que dibuje
        assertEquals("Dibuja tu firma en el recuadro antes de guardar.",
                assertThrows(IllegalArgumentException.class,
                        () -> MiFirmaController.desdeDataUrl(PREFIJO)).getMessage());
    }

    @Test
    void shouldRejectInputOverTheLimit() {
        // +4 para que siga siendo multiplo de 4 y falle por el tope, no por el largo
        String pasado = "A".repeat(MiFirmaController.DIBUJO_MAX_CHARS + 4);
        assertEquals("El dibujo de la firma es demasiado grande; vuelve a intentarlo.",
                assertThrows(IllegalArgumentException.class,
                        () -> MiFirmaController.desdeDataUrl(dataUrl(pasado))).getMessage());
    }

    /**
     * Cada una de estas cadenas es un relleno o un alfabeto que el decodificador intrinseco no
     * debe llegar a ver. Son las que importan para el CVE.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "QUJDR",        // largo 5: no es multiplo de 4
        "QU=JQUJD",     // relleno en medio
        "QUJDQ===",     // tres signos de relleno
        "====",         // solo relleno
        "QU==QUJD",     // dato despues del relleno
        "QUJ_",         // alfabeto URL: guion bajo
        "QUJ-",         // alfabeto URL: guion
        "QUJD\nQUJD",   // salto de linea
        "QUJ ",         // espacio
        "QUJ\t",        // tabulador
        "QUñD",    // no ASCII
    })
    void shouldRejectMalformedBase64BeforeDecoding(String malformado) {
        assertThrows(IllegalArgumentException.class,
                () -> MiFirmaController.desdeDataUrl(dataUrl(malformado)),
                "deberia rechazarse antes de decodificar: " + malformado);
    }

    @Test
    void shouldRejectMalformedPaddingThatTheJdkDecoderWouldTolerate() {
        // java.util.Base64 acepta la falta de relleno; nosotros no, porque el fallo esta
        // justo en el manejo del ultimo grupo. "QUJD" + "QQ" decodifica en el JDK pero aqui no.
        assertThrows(IllegalArgumentException.class,
                () -> MiFirmaController.desdeDataUrl(dataUrl("QUJDQQ")));

        // y confirmamos que el JDK si lo habria aceptado, para que quede claro que el filtro suma
        assertArrayEquals(Base64.getDecoder().decode("QUJDQQ"), Base64.getDecoder().decode("QUJDQQ"));
    }
}
