package com.onest.app.catalog.formato.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
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
        aplanarHojas(doc);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withW3cDocument(new W3CDom().fromJsoup(doc), "classpath:/static/");
        builder.useUriResolver((base, uri) -> resolver(uri));
        builder.toStream(out);
        try {
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
        log.info("[pdf] impreso generado en {} ms ({} KB)", System.currentTimeMillis() - inicio, out.size() / 1024);
        return out.toByteArray();
    }

    /**
     * Los impresos van en una tabla (table.print-doc, una fila tr.hoja por hoja) para que el navegador repita
     * encabezado y pie; el convertidor no respeta el salto de pagina en filas de tabla. Aqui cada hoja pasa a
     * un div.hoja-pdf (el encabezado y pie van en los margenes de @page, fragments/impreso-pdf).
     */
    private static void aplanarHojas(Document doc) {
        for (org.jsoup.nodes.Element tabla : doc.select("table.print-doc")) {
            org.jsoup.select.Elements hojas = tabla.select("> tbody > tr.hoja");
            org.jsoup.nodes.Element ultima = null;
            for (org.jsoup.nodes.Element tr : hojas) {
                org.jsoup.nodes.Element div = new org.jsoup.nodes.Element("div").addClass("hoja-pdf");
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
