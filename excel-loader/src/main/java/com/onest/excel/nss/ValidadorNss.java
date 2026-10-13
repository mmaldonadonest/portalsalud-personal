package com.onest.excel.nss;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Convierte un renglon del Excel en un registro listo para insertar, o en la lista de motivos
 * por los que no puede entrar. Un renglon con cualquier error NO entra: se reporta al predio con
 * su numero de renglon para que lo corrija y vuelva a mandar el archivo.
 */
public final class ValidadorNss {

    /** Un renglon que paso: columna destino -> valor ya tipado (String, LocalDate, BigDecimal, Integer). */
    public record Registro(int fila, Map<String, Object> valores) {
        public String texto(String columna) {
            Object v = valores.get(columna);
            return v == null ? null : v.toString();
        }

        public LocalDate fecha(String columna) {
            return (LocalDate) valores.get(columna);
        }
    }

    /** Un motivo de rechazo, en terminos del Excel (renglon, encabezado, lo que traia). */
    public record Error(int fila, String encabezado, String valor, String motivo) {
    }

    /**
     * Resultado de un renglon: o registro, o errores. Un renglon valido puede traer avisos: entra a
     * la base, pero el predio lo ve en su reporte para revisarlo (hoy: costo negativo de accidentes).
     */
    public record Resultado(Registro registro, List<Error> errores, List<Error> avisos) {
        public boolean valido() {
            return errores.isEmpty();
        }
    }

    /** Mismo orden que el catalogo MES de CatalogosNss: el indice + 1 es el numero de mes. */
    static final List<String> MESES = List.of("ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO",
            "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");

    static final String AVISO_NEGATIVO = "negativo: entra tal cual; revisar (en el Excel viejo suele ser "
            + "una formula sin fechas, -3 dias x pago 60%)";

    // STRICT: sin esto "31/02/2026" se "ajustaba" en silencio a 28/02 (visto en la prueba del 10-oct)
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // lo que entrega LectorXlsx
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uu").withResolverStyle(java.time.format.ResolverStyle.STRICT));

    private final LayoutNss layout;
    /** encabezado del layout -> indice de columna en el archivo */
    private final Map<CampoNss, Integer> indices;

    private ValidadorNss(LayoutNss layout, Map<CampoNss, Integer> indices) {
        this.layout = layout;
        this.indices = indices;
    }

    /**
     * Reconoce el layout por el renglon de encabezados: gana el layout cuyos encabezados estan
     * TODOS en el archivo, y entre varios, el de mas columnas (el periodico es subconjunto del de
     * nuevo ingreso en casi todo, por eso hace falta el desempate).
     */
    public static Optional<ValidadorNss> paraEncabezados(List<String> encabezados) {
        Map<String, Integer> posicion = new LinkedHashMap<>();
        for (int i = 0; i < encabezados.size(); i++) {
            String h = encabezados.get(i);
            if (h != null && !h.isBlank()) {
                posicion.putIfAbsent(CatalogosNss.clave(h), i);
            }
        }
        ValidadorNss mejor = null;
        for (LayoutNss l : LayoutNss.values()) {
            Map<CampoNss, Integer> idx = new LinkedHashMap<>();
            boolean completo = true;
            for (CampoNss c : l.campos()) {
                Integer p = c.encabezadosAceptados().stream().map(e -> posicion.get(CatalogosNss.clave(e)))
                        .filter(java.util.Objects::nonNull).findFirst().orElse(null);
                if (p == null) {
                    completo = false;
                    break;
                }
                idx.put(c, p);
            }
            if (completo && (mejor == null || l.campos().size() > mejor.layout.campos().size())) {
                mejor = new ValidadorNss(l, idx);
            }
        }
        return Optional.ofNullable(mejor);
    }

    /** Encabezados que faltan para cada layout: sirve para decirle al usuario por que no se reconocio. */
    public static String faltantes(List<String> encabezados) {
        List<String> claves = encabezados.stream().map(h -> h == null ? "" : CatalogosNss.clave(h)).toList();
        StringBuilder sb = new StringBuilder();
        for (LayoutNss l : LayoutNss.values()) {
            List<String> faltan = l.campos().stream()
                    .filter(c -> c.encabezadosAceptados().stream().noneMatch(e -> claves.contains(CatalogosNss.clave(e))))
                    .map(CampoNss::encabezado).toList();
            sb.append("\n    ").append(l.hoja()).append(": faltan ").append(faltan);
        }
        return sb.toString();
    }

    public LayoutNss layout() {
        return layout;
    }

    public Resultado validar(LectorXlsx.Fila fila) {
        Map<String, Object> valores = new LinkedHashMap<>();
        List<Error> errores = new ArrayList<>();
        List<Error> avisos = new ArrayList<>();
        for (Map.Entry<CampoNss, Integer> e : indices.entrySet()) {
            CampoNss c = e.getKey();
            String crudo = fila.valor(e.getValue());
            String v = crudo == null ? "" : crudo.trim();
            if (v.isEmpty()) {
                if (c.obligatorio()) {
                    errores.add(new Error(fila.numero(), c.encabezado(), "", "es obligatorio"));
                }
                continue;
            }
            // AÑO capturado como fecha con formato "yyyy" (se ve 2026, pero la celda es una fecha):
            // LectorXlsx la entrega como yyyy-MM-dd; se toma solo el ano
            if ("ANIO".equals(c.columna()) && v.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                v = v.substring(0, 4);
            }
            try {
                Object valor = convertir(c, v);
                valores.put(c.columna(), valor);
                if (c.negativoConAviso() && valor instanceof BigDecimal n && n.signum() < 0) {
                    avisos.add(new Error(fila.numero(), c.encabezado(), v, AVISO_NEGATIVO));
                }
            } catch (IllegalArgumentException ex) {
                errores.add(new Error(fila.numero(), c.encabezado(), v, ex.getMessage()));
            }
        }
        // Regla entre columnas: una incapacidad del IMSS siempre trae folio (en 2026, el 100%)
        if (layout == LayoutNss.INCAPACIDADES && "IMSS".equals(valores.get("RUBRO")) && valores.get("FOLIO") == null) {
            errores.add(new Error(fila.numero(), "FOLIO", "", "obligatorio cuando el RUBRO es IMSS"));
        }
        // Maternidad: el embarazo se identifica por NSS + FUM, o NSS + fecha probable de parto si
        // falta la FUM (decision 12-oct). Sin ninguna de las dos el renglon no se puede ligar a su
        // embarazo y el historial quedaria suelto.
        if (layout == LayoutNss.MATERNIDAD && valores.get("NSS") != null) {
            LocalDate fum = (LocalDate) valores.get("FUM");
            LocalDate fpp = (LocalDate) valores.get("FECHA_PROB_PARTO");
            if (fum != null) {
                valores.put("CLAVE_EMBARAZO", valores.get("NSS") + "|FUM:" + fum);
            } else if (fpp != null) {
                valores.put("CLAVE_EMBARAZO", valores.get("NSS") + "|FPP:" + fpp);
            } else {
                errores.add(new Error(fila.numero(), "FUM", "",
                        "falta FUM o FECHA PROBABLE DE PARTO: sin una de las dos no se identifica el embarazo"));
            }
        }
        // Medicamentos: el mes viene como nombre y el ano aparte (en el Excel original el mes es un
        // encabezado de texto); la FECHA que usan el reemplazo y las graficas es el dia 1 de ese mes.
        if (layout == LayoutNss.MEDICAMENTOS && valores.get("MES") != null && valores.get("ANIO") != null) {
            int anio = (Integer) valores.get("ANIO");
            if (anio < 2000 || anio > 2100) {
                errores.add(new Error(fila.numero(), "AÑO", String.valueOf(anio), "anio fuera de rango"));
            } else {
                valores.put("FECHA", LocalDate.of(anio, MESES.indexOf((String) valores.get("MES")) + 1, 1));
            }
        }
        return new Resultado(errores.isEmpty() ? new Registro(fila.numero(), valores) : null, errores, avisos);
    }

    private static Object convertir(CampoNss c, String v) {
        return switch (c.tipo()) {
            case NSS -> nss(v);
            case TEXTO -> {
                if (v.length() > c.maximo()) {
                    throw new IllegalArgumentException("excede " + c.maximo() + " caracteres (" + v.length() + ")");
                }
                yield v;
            }
            case FECHA -> fecha(v);
            // Dias, edad, SDI, pagos y costos no son negativos. Sin este freno, la prueba del
            // 10-oct cargo costos de accidentes de -248 millones (formulas rotas del Excel viejo)
            // y la grafica los sumo tal cual. Excepcion (12-oct-2026, decision del usuario): el
            // COSTO de accidentes entra negativo y se avisa (CampoNss.negativoConAviso).
            case ENTERO -> {
                BigDecimal n = noNegativo(numero(v));
                try {
                    yield n.intValueExact();
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("debe ser un numero entero");
                }
            }
            case DECIMAL -> c.negativoConAviso() ? numero(v) : noNegativo(numero(v));
            case CATALOGO -> CatalogosNss.buscar(c.catalogo(), v).orElseThrow(() ->
                    new IllegalArgumentException("no esta en el catalogo " + c.catalogo()
                            + " (validos: " + CatalogosNss.validos(c.catalogo()) + ")"));
        };
    }

    /**
     * NSS de 11 digitos. Si Excel lo guardo como numero, pierde los ceros a la izquierda: se
     * recuperan rellenando a 11.
     */
    static String nss(String v) {
        String d = v.replaceAll("[\\s-]", "");
        if (!d.matches("\\d+")) {
            throw new IllegalArgumentException("el NSS solo lleva digitos");
        }
        if (d.length() < 11) {
            d = "0".repeat(11 - d.length()) + d;
        }
        if (d.length() != 11) {
            throw new IllegalArgumentException("el NSS debe tener 11 digitos (trae " + d.length() + ")");
        }
        return d;
    }

    static LocalDate fecha(String v) {
        String t = v.length() >= 10 && v.charAt(4) == '-' ? v.substring(0, 10) : v;
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try {
                LocalDate d = LocalDate.parse(t, f);
                // Anios imposibles (2626 que en pantalla se ve "26", 1900 de una celda vacia con
                // formula): se rechazan en vez de guardarlos y deformar una grafica
                if (d.getYear() < 2000 || d.getYear() > 2100) {
                    throw new IllegalArgumentException("anio fuera de rango: " + d.getYear());
                }
                return d;
            } catch (DateTimeParseException ignorada) {
                // probar el siguiente formato
            }
        }
        throw new IllegalArgumentException("no es una fecha dd/mm/aaaa");
    }

    private static BigDecimal noNegativo(BigDecimal n) {
        if (n.signum() < 0) {
            throw new IllegalArgumentException("no puede ser negativo");
        }
        return n;
    }

    static BigDecimal numero(String v) {
        String t = v.replace("$", "").replace(",", "").replace(" ", "");
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("no es un numero");
        }
    }
}
