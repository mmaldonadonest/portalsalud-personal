package com.onest.app.catalog.formato.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * HTML de un impreso -> PDF, en el servidor (28-oct-2026). En la red corporativa la vista previa de
 * impresion del navegador tardaba mucho en armar el expediente; con el PDF ya hecho el navegador solo
 * lo muestra. Usa las MISMAS plantillas del impreso: la plantilla pone {@code pdf=true} para no cargar
 * el CSS del tema (Bootstrap con flexbox, que OpenHTMLtoPDF no entiende) y usar una hoja de estilos simple.
 */
@Service
public class HtmlPdfService {

    private static final Logger log = LoggerFactory.getLogger(HtmlPdfService.class);

    /**
     * @param html        HTML ya procesado por Thymeleaf
     * @param ajuste      cambios al documento antes de convertir (lo que en el navegador hacia JavaScript)
     */
    public byte[] pdf(String html, Consumer<Document> ajuste) {
        long inicio = System.currentTimeMillis();
        Document doc = Jsoup.parse(html);
        doc.select("script").remove();
        if (ajuste != null) {
            ajuste.accept(doc);
        }
        List<Element> hojas = aplanarHojas(doc);
        byte[] pdf = render(doc, hojas);
        int vueltas = 1;
        // E-047 (20-nov-2026, pedido del usuario: no desperdiciar hojas, se imprimen varios examenes al dia): si una
        // hoja se paso de una pagina (texto capturado largo) se vuelve a generar compactando SOLO esa hoja
        // (impreso-pdf.html: apretada-1 y luego apretada-2). Si ni asi cabe, se deja como estaba: puede ser una
        // hoja que de por si ocupa mas de una pagina.
        if (hojas.size() > 1) {
            int[] nivel = new int[hojas.size()];
            boolean[] fija = new boolean[hojas.size()];
            byte[] valido = pdf;
            for (int intento = 0; intento < 3; intento++) {
                int[] paginas = paginasPorHoja(pdf, hojas.size());
                if (paginas == null) {
                    break;  // sin marcadores no se sabe que hoja se paso: queda el PDF tal cual
                }
                boolean cambio = false;
                for (int i = 0; i < hojas.size(); i++) {
                    if (fija[i] || paginas[i] <= 1) {
                        continue;
                    }
                    if (nivel[i] < 2) {
                        nivel[i]++;
                    } else {
                        nivel[i] = 0;  // ni compactada cabe: vuelve a su forma original
                        fija[i] = true;
                    }
                    cambio = true;
                }
                if (!cambio) {
                    valido = pdf;
                    break;
                }
                for (int i = 0; i < hojas.size(); i++) {
                    hojas.get(i).removeClass("apretada-1").removeClass("apretada-2");
                    if (nivel[i] > 0) {
                        hojas.get(i).addClass("apretada-" + nivel[i]);
                    }
                }
                pdf = render(doc, hojas);
                vueltas++;
                valido = pdf;
            }
            pdf = sinMarcadores(valido);
        }
        log.info("[pdf] impreso generado en {} ms ({} KB, {} vuelta(s))", System.currentTimeMillis() - inicio, pdf.length / 1024, vueltas);
        return pdf;
    }

    private byte[] render(Document doc, List<Element> hojas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        org.w3c.dom.Document w3c = new W3CDom().fromJsoup(doc);
        marcarHojas(w3c, hojas.size());
        builder.withW3cDocument(w3c, "classpath:/static/");
        builder.useUriResolver((base, uri) -> resolver(uri));
        builder.toStream(out);
        try {
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }

    /**
     * Un marcador (bookmark de OpenHTMLtoPDF) al inicio de cada hoja: en el PDF dice en que pagina empieza cada una.
     * Se agrega al DOM W3C y no al de jsoup porque el parser HTML sacaria el elemento {@code bookmarks} del head.
     */
    private static void marcarHojas(org.w3c.dom.Document w3c, int total) {
        if (total < 2) {
            return;
        }
        org.w3c.dom.NodeList heads = w3c.getElementsByTagName("head");
        if (heads.getLength() == 0) {
            return;
        }
        org.w3c.dom.Element head = (org.w3c.dom.Element) heads.item(0);
        org.w3c.dom.Element marcas = w3c.createElementNS(head.getNamespaceURI(), "bookmarks");
        for (int i = 0; i < total; i++) {
            org.w3c.dom.Element b = w3c.createElementNS(head.getNamespaceURI(), "bookmark");
            b.setAttribute("name", MARCA + i);
            b.setAttribute("href", "#" + MARCA + i);
            marcas.appendChild(b);
        }
        head.appendChild(marcas);
    }

    /** Paginas que ocupa cada hoja segun los marcadores; null si el PDF no los trae completos. */
    private static int[] paginasPorHoja(byte[] pdf, int total) {
        try (org.apache.pdfbox.pdmodel.PDDocument d = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            var outline = d.getDocumentCatalog().getDocumentOutline();
            if (outline == null) {
                return null;
            }
            int[] inicio = new int[total];
            java.util.Arrays.fill(inicio, -1);
            for (var item : outline.children()) {
                String t = item.getTitle();
                if (t == null || !t.startsWith(MARCA)) {
                    continue;
                }
                int i = Integer.parseInt(t.substring(MARCA.length()));
                var pagina = item.findDestinationPage(d);
                if (i >= 0 && i < total && pagina != null) {
                    inicio[i] = d.getPages().indexOf(pagina);
                }
            }
            int[] paginas = new int[total];
            for (int i = 0; i < total; i++) {
                int fin = i + 1 < total ? inicio[i + 1] : d.getNumberOfPages();
                if (inicio[i] < 0 || fin < 0) {
                    return null;
                }
                paginas[i] = fin - inicio[i];
            }
            return paginas;
        } catch (IOException | RuntimeException ex) {
            log.warn("[pdf] no se pudo revisar si alguna hoja se paso de pagina: {}", ex.getMessage());
            return null;
        }
    }

    /** Quita los marcadores internos del PDF final (no deben verse en el panel del visor). */
    private static byte[] sinMarcadores(byte[] pdf) {
        try (org.apache.pdfbox.pdmodel.PDDocument d = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            if (d.getDocumentCatalog().getDocumentOutline() == null) {
                return pdf;
            }
            d.getDocumentCatalog().setDocumentOutline(null);
            d.getDocumentCatalog().setPageMode(org.apache.pdfbox.pdmodel.PageMode.USE_NONE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            d.save(out);
            return out.toByteArray();
        } catch (IOException | RuntimeException ex) {
            log.warn("[pdf] no se pudieron quitar los marcadores: {}", ex.getMessage());
            return pdf;
        }
    }

    private static final String MARCA = "hoja-pdf-";

    /**
     * Los impresos van en una tabla (table.print-doc, una fila tr.hoja por hoja) para que el navegador repita
     * encabezado y pie; el convertidor no respeta el salto de pagina en filas de tabla. Aqui cada hoja pasa a
     * un div.hoja-pdf (el encabezado y pie van en los margenes de @page, fragments/impreso-pdf).
     */
    private static List<Element> aplanarHojas(Document doc) {
        List<Element> todas = new ArrayList<>();
        for (org.jsoup.nodes.Element tabla : doc.select("table.print-doc")) {
            org.jsoup.select.Elements hojas = tabla.select("> tbody > tr.hoja");
            org.jsoup.nodes.Element ultima = null;
            for (org.jsoup.nodes.Element tr : hojas) {
                org.jsoup.nodes.Element div = new org.jsoup.nodes.Element("div").addClass("hoja-pdf")
                        .attr("id", MARCA + todas.size());
                todas.add(div);
                org.jsoup.nodes.Element td = tr.selectFirst("> td");
                if (td != null) {
                    div.appendChildren(td.childNodesCopy());
                }
                tabla.before(div);
                ultima = div;
            }
            if (ultima != null) {
                ultima.addClass("ultima");
            }
            tabla.remove();
        }
        return todas;
    }

    /**
     * Las imagenes del impreso vienen con la ruta del portal ("/portal-salud/img/examen/Imagen1.png"): se leen
     * del classpath (static/) en vez de pedirlas por HTTP, que en el servidor pasaria por el login.
     * Las firmas vienen como data: URI y pasan tal cual.
     */
    private static String resolver(String uri) {
        if (uri == null || uri.startsWith("data:")) {
            return uri;
        }
        int i = uri.indexOf("/img/");
        if (i < 0) {
            return null;
        }
        // con el perfil prod las URLs llevan la huella del contenido ("Imagen1-edea6265....png", cache del
        // navegador); el archivo en static/ no la tiene
        String ruta = uri.substring(i).replaceFirst("\\?.*$", "").replaceFirst("-[0-9a-f]{32}(\\.[A-Za-z0-9]+)$", "$1");
        ClassPathResource r = new ClassPathResource("static" + ruta);
        try {
            return r.exists() ? r.getURL().toExternalForm() : null;
        } catch (IOException ex) {
            return null;
        }
    }
}
