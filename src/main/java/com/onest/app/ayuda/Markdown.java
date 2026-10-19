package com.onest.app.ayuda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown minimo para los articulos de ayuda: encabezados (#), negritas (**), codigo (`),
 * enlaces [texto](ruta), listas con - o 1., tablas con | y parrafos. Sin dependencias: el
 * contenido lo escribe el servicio medico desde el admin y con esto alcanza. Todo lo que no
 * es Markdown se escapa, asi que no entra HTML del editor a la pagina.
 */
public final class Markdown {

    private static final Pattern ENLACE = Pattern.compile("\\[([^\\]]+)]\\(([^)\\s]+)\\)");
    private static final Pattern NEGRITA = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern CODIGO = Pattern.compile("`([^`]+)`");

    private Markdown() {
    }

    public static String aHtml(String md) {
        if (md == null || md.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        String lista = null;      // "ul" | "ol" | null
        boolean tabla = false;
        boolean parrafo = false;
        for (String cruda : md.replace("\r\n", "\n").split("\n")) {
            String linea = cruda.strip();
            if (linea.isEmpty()) {
                lista = cerrarLista(out, lista);
                tabla = cerrarTabla(out, tabla);
                parrafo = cerrarParrafo(out, parrafo);
                continue;
            }
            if (linea.startsWith("#")) {
                lista = cerrarLista(out, lista);
                tabla = cerrarTabla(out, tabla);
                parrafo = cerrarParrafo(out, parrafo);
                int n = 0;
                while (n < linea.length() && linea.charAt(n) == '#') {
                    n++;
                }
                int h = Math.min(Math.max(n, 1) + 2, 6); // # -> h3 dentro del panel
                out.append("<h").append(h).append(">").append(inline(linea.substring(n).strip())).append("</h").append(h).append(">\n");
                continue;
            }
            if (linea.startsWith("|")) {
                lista = cerrarLista(out, lista);
                parrafo = cerrarParrafo(out, parrafo);
                if (linea.matches("^\\|[\\s|:-]+\\|$")) {
                    continue; // separador de encabezado
                }
                if (!tabla) {
                    out.append("<table class=\"table table-sm ayuda-tabla\">\n");
                    tabla = true;
                }
                out.append("<tr>");
                String[] celdas = linea.substring(1, linea.endsWith("|") ? linea.length() - 1 : linea.length()).split("\\|");
                for (String c : celdas) {
                    out.append("<td>").append(inline(c.strip())).append("</td>");
                }
                out.append("</tr>\n");
                continue;
            }
            tabla = cerrarTabla(out, tabla);
            String tipo = linea.startsWith("- ") || linea.startsWith("* ") ? "ul" : (linea.matches("^\\d+\\.\\s.*") ? "ol" : null);
            if (tipo != null) {
                parrafo = cerrarParrafo(out, parrafo);
                if (!tipo.equals(lista)) {
                    lista = cerrarLista(out, lista);
                    out.append("<").append(tipo).append(">\n");
                    lista = tipo;
                }
                String item = "ul".equals(tipo) ? linea.substring(2) : linea.replaceFirst("^\\d+\\.\\s", "");
                out.append("<li>").append(inline(item.strip())).append("</li>\n");
                continue;
            }
            lista = cerrarLista(out, lista);
            if (!parrafo) {
                out.append("<p>");
                parrafo = true;
            } else {
                out.append("<br>");
            }
            out.append(inline(linea));
        }
        cerrarLista(out, lista);
        cerrarTabla(out, tabla);
        cerrarParrafo(out, parrafo);
        return out.toString();
    }

    /** Solo texto, para indexar: sin marcas ni rutas de enlace. */
    public static String textoPlano(String md) {
        if (md == null) {
            return "";
        }
        String t = ENLACE.matcher(md).replaceAll("$1");
        t = t.replace("**", "").replace("`", "").replaceAll("(?m)^#+\\s*", "").replaceAll("(?m)^[-*]\\s+", "")
             .replaceAll("(?m)^\\|[\\s|:-]+\\|$", "").replace("|", " ");
        return t.replaceAll("[ \\t]+", " ").replaceAll("\\n{2,}", "\n").strip();
    }

    private static String inline(String s) {
        String e = escapar(s);
        Matcher m = ENLACE.matcher(e);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String ruta = m.group(2);
            // Rutas relativas al portal: el front antepone contextPath (data-ruta); externas van tal cual
            String tag = ruta.startsWith("http")
                    ? "<a href=\"" + ruta + "\" target=\"_blank\" rel=\"noopener\">" + m.group(1) + "</a>"
                    : "<a href=\"#\" class=\"ayuda-enlace\" data-ruta=\"" + ruta + "\">" + m.group(1) + "</a>";
            m.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        m.appendTail(sb);
        e = NEGRITA.matcher(sb).replaceAll("<strong>$1</strong>");
        e = CODIGO.matcher(e).replaceAll("<code>$1</code>");
        return e;
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String cerrarLista(StringBuilder out, String lista) {
        if (lista != null) {
            out.append("</").append(lista).append(">\n");
        }
        return null;
    }

    private static boolean cerrarTabla(StringBuilder out, boolean tabla) {
        if (tabla) {
            out.append("</table>\n");
        }
        return false;
    }

    private static boolean cerrarParrafo(StringBuilder out, boolean parrafo) {
        if (parrafo) {
            out.append("</p>\n");
        }
        return false;
    }
}
