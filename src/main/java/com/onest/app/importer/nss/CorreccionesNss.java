package com.onest.app.importer.nss;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NSS que el servicio medico encontro a mano para renglones que llegaron sin NSS (2-nov-2026: los 7
 * "(en blanco)" de MACRO I en Primer1 ATENCIONES). El archivo original NO se toca: las correcciones
 * viven en un Excel aparte (renglon del Excel -> NSS) y se aplican al leer.
 *
 * <p>Los numeros de renglon solo valen para UN archivo, por eso la carga con correcciones exige que
 * la carpeta traiga un solo .xlsx (ver ExcelLoaderApplication). Ademas cada correccion repite PREDIO
 * y FECHA del renglon: si no coinciden (alguien movio renglones), el renglon se rechaza en vez de
 * quedar con el NSS de otra persona.
 */
public final class CorreccionesNss {

    /** Lo que dice el Excel de correcciones para un renglon. */
    public record Correccion(int fila, String nss, String predio, LocalDate fecha) {
    }

    private final Path archivo;
    private final Map<Integer, Correccion> porFila;

    private CorreccionesNss(Path archivo, Map<Integer, Correccion> porFila) {
        this.archivo = archivo;
        this.porFila = porFila;
    }

    /**
     * Lee la primera hoja: busca el renglon de encabezados que trae "RENGLON DEL EXCEL" y toma el NSS
     * de "NSS CORRECTO" si se lleno, si no de "NSS" (asi lo capturo el usuario el 2-nov-2026).
     */
    public static CorreccionesNss leer(Path archivo) throws Exception {
        Map<Integer, Correccion> porFila = new LinkedHashMap<>();
        Map<String, Integer> col = new LinkedHashMap<>();
        LectorXlsx.leer(archivo, null, f -> {
            if (col.isEmpty()) {
                for (int i = 0; i < f.valores().size(); i++) {
                    String h = f.valor(i);
                    if (h != null && !h.isBlank()) {
                        col.putIfAbsent(CatalogosNss.clave(h), i);
                    }
                }
                if (!col.containsKey("RENGLON DEL EXCEL")) {
                    col.clear();   // titulo o nota: el encabezado viene mas abajo
                }
                return;
            }
            String renglon = texto(f, col.get("RENGLON DEL EXCEL"));
            if (renglon == null) {
                return;
            }
            String nss = Optional.ofNullable(texto(f, col.get("NSS CORRECTO"))).orElse(texto(f, col.get("NSS")));
            int fila = Integer.parseInt(renglon.replaceAll("\\.0+$", ""));
            if (nss == null || CatalogosNss.clave(nss).equals("(EN BLANCO)")) {
                return;   // aun sin NSS: ese renglon se queda como venga
            }
            String fecha = texto(f, col.get("FECHA"));
            Correccion c = new Correccion(fila, ValidadorNss.nss(nss), texto(f, col.get("PREDIO")),
                    fecha == null ? null : ValidadorNss.fecha(fecha));
            if (porFila.put(fila, c) != null) {
                throw new IllegalArgumentException("el renglon " + fila + " viene dos veces en " + archivo);
            }
        });
        if (col.isEmpty()) {
            throw new IllegalArgumentException("no trae el encabezado 'Renglón del Excel': " + archivo);
        }
        return new CorreccionesNss(archivo, porFila);
    }

    private static String texto(LectorXlsx.Fila f, Integer i) {
        String v = i == null ? null : f.valor(i);
        return v == null || v.isBlank() ? null : v.trim();
    }

    public Path archivo() {
        return archivo;
    }

    public Optional<Correccion> para(int fila) {
        return Optional.ofNullable(porFila.get(fila));
    }

    public List<Correccion> todas() {
        return List.copyOf(porFila.values());
    }
}
