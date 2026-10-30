package com.onest.app.catalog.examen.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Un diente del odontograma impreso (29-oct-2026): el circulo de 5 caras de los formatos EMLE (imagen de la hoja de
 * dientes del FT-SO-04/16/17/18/19/20), pintado con lo capturado. Misma geometria y colores que la captura
 * (fragments/examen-seccion.html :: odontograma): centro = oclusal; arriba/abajo = vestibular y lingual segun la
 * arcada; lados = mesial hacia la linea media y distal hacia afuera. Es PNG y no SVG porque el PDF lo arma
 * OpenHTMLtoPDF, que sin el modulo de SVG no lo dibuja.
 */
public final class Odontograma {

    private static final int ESCALA = 3;                 // viewBox 40 -> 120 px: nitido al imprimir
    private static final Color BORDE = new Color(0x98A2B3);
    private static final Map<String, Color> HALLAZGO = Map.of(
            "C", new Color(0xF97066),   // caries
            "A", new Color(0x98A2B3),   // amalgama
            "E", new Color(0x9B8AFB));  // endodoncia
    private static final Pattern POR_CARA = Pattern.compile("(?i)^([CAE]):([OVLMD](?:,[OVLMD])*)$");
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private Odontograma() {
    }

    /** data: URI PNG del diente {@code numero} (11-48) con el valor guardado en DIENTE_nn ("C:O,M A:D", "X", "P"...). */
    public static String dataUri(int numero, String valor) {
        String v = valor == null ? "" : valor.trim();
        return CACHE.computeIfAbsent(numero / 10 + "|" + v, k -> "data:image/png;base64," + png(numero, v));
    }

    private static String png(int numero, String valor) {
        int q = numero / 10;
        boolean mesialDerecha = q == 1 || q == 4;   // cuadrantes a la izquierda de la linea media
        Map<String, String> caras = new HashMap<>();
        boolean ausente = false;
        boolean protesis = false;
        for (String token : valor.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            Matcher m = POR_CARA.matcher(token);
            if (m.matches()) {
                for (String c : m.group(2).toUpperCase(Locale.ROOT).split(",")) {
                    caras.put(c, m.group(1).toUpperCase(Locale.ROOT));
                }
            } else if ("X".equalsIgnoreCase(token)) {
                ausente = true;
            } else if ("P".equalsIgnoreCase(token)) {
                protesis = true;
            } else if (HALLAZGO.containsKey(token.toUpperCase(Locale.ROOT))) {
                // codigo del sistema anterior para el diente completo: se pinta en las 5 caras
                for (String c : new String[] {"O", "V", "L", "M", "D"}) {
                    caras.put(c, token.toUpperCase(Locale.ROOT));
                }
            }
        }
        // caras en la posicion del dibujo: arriba, derecha, abajo, izquierda, centro
        String arriba = q <= 2 ? "V" : "L";
        String abajo = q <= 2 ? "L" : "V";
        String derecha = mesialDerecha ? "M" : "D";
        String izquierda = mesialDerecha ? "D" : "M";

        int lado = 40 * ESCALA;
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(ESCALA, ESCALA);
            Ellipse2D centro = new Ellipse2D.Double(13, 13, 14, 14);
            Shape[] sectores = {sector(45), sector(-45), sector(225), sector(135)};
            String[] posiciones = {arriba, derecha, abajo, izquierda};
            Color borde = protesis ? new Color(0x0057A8) : BORDE;
            for (int i = 0; i < sectores.length; i++) {
                pintar(g, sectores[i], relleno(caras.get(posiciones[i]), protesis, ausente), borde);
            }
            pintar(g, centro, relleno(caras.get("O"), protesis, ausente), borde);
            // 29-oct-2026: se imprime en blanco y negro y caries, amalgama y endodoncia quedan del mismo gris; la
            // letra del formato en cada cara marcada (y P en la protesis) es lo que las distingue en papel
            if (!ausente) {
                double[][] centros = {{20, 7.6}, {32.4, 20}, {20, 32.4}, {7.6, 20}};
                for (int i = 0; i < posiciones.length; i++) {
                    letra(g, caras.get(posiciones[i]), centros[i][0], centros[i][1]);
                }
                letra(g, protesis ? "P" : caras.get("O"), 20, 20);
            }
            if (ausente) {
                g.setColor(new Color(0x111111));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(6, 6, 34, 34));
                g.draw(new Line2D.Double(34, 6, 6, 34));
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo dibujar el diente " + numero, ex);
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** Cara exterior: el cuarto del anillo entre el circulo r=18 y el centro r=7, de {@code inicio} a +90 grados. */
    private static Shape sector(double inicio) {
        Area a = new Area(new Arc2D.Double(2, 2, 36, 36, inicio, 90, Arc2D.PIE));
        a.subtract(new Area(new Ellipse2D.Double(13, 13, 14, 14)));
        return a;
    }

    private static Color relleno(String hallazgo, boolean protesis, boolean ausente) {
        if (ausente) {
            return new Color(0xF2F4F7);
        }
        if (protesis) {
            return new Color(0xD1E9FF);
        }
        return hallazgo == null ? Color.WHITE : HALLAZGO.get(hallazgo);
    }

    /** Letra del hallazgo centrada en (x, y), en negro y negrita para que se lea impresa en blanco y negro. */
    private static void letra(Graphics2D g, String hallazgo, double x, double y) {
        if (hallazgo == null) {
            return;
        }
        g.setColor(Color.BLACK);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 9));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(hallazgo, (float) (x - fm.stringWidth(hallazgo) / 2.0), (float) (y + fm.getAscent() / 2.0 - 1));
    }

    private static void pintar(Graphics2D g, Shape forma, Color fondo, Color borde) {
        g.setColor(fondo);
        g.fill(forma);
        g.setColor(borde);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(forma);
    }
}
