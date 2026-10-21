package com.onest.app.catalog.firma.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Limpieza de la imagen de una firma antes de cifrarla (20-oct-2026). Volver a dibujar los pixeles en
 * una imagen nueva descarta cualquier dato oculto del archivo original (fecha, celular, ubicacion);
 * el fondo claro se vuelve transparente para no tapar el formato, se recorta al trazo y se reduce a
 * baja resolucion (suficiente para imprimir, mala para reutilizar). Siempre sale PNG.
 */
public final class ImagenFirma {

    public static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_ANCHO = 420;
    private static final int MAX_ALTO = 160;
    /** Pixeles con todos los canales por encima de esto se consideran papel (fondo). */
    private static final int CLARO = 200;

    private ImagenFirma() {
    }

    /** Devuelve el PNG limpio; IllegalArgumentException con el motivo para el usuario si no sirve. */
    public static byte[] normalizar(byte[] original) {
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("No llegó ninguna imagen.");
        }
        if (original.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen pesa más de 1 MB.");
        }
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException ex) {
            img = null;
        }
        if (img == null) {
            throw new IllegalArgumentException("El archivo no es una imagen PNG o JPG válida.");
        }
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage limpia = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xff;
                int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                boolean fondo = a < 40 || (r > CLARO && g > CLARO && b > CLARO);
                if (!fondo) {
                    limpia.setRGB(x, y, argb | 0xff000000);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0 || (maxX - minX) < 10 || (maxY - minY) < 5) {
            throw new IllegalArgumentException("No se encontró el trazo de la firma (¿imagen en blanco?).");
        }
        int margen = 4;
        minX = Math.max(0, minX - margen);
        minY = Math.max(0, minY - margen);
        maxX = Math.min(w - 1, maxX + margen);
        maxY = Math.min(h - 1, maxY + margen);
        BufferedImage recorte = limpia.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        double escala = Math.min(1.0, Math.min((double) MAX_ANCHO / recorte.getWidth(), (double) MAX_ALTO / recorte.getHeight()));
        int nw = Math.max(1, (int) Math.round(recorte.getWidth() * escala));
        int nh = Math.max(1, (int) Math.round(recorte.getHeight() * escala));
        BufferedImage salida = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        var g2 = salida.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(recorte, 0, 0, nw, nh, null);
        g2.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(salida, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo preparar la imagen de la firma", ex);
        }
    }
}
