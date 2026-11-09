package com.onest.app.importer.nss;

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

    /** Inicio del motivo cuando un valor entra traducido por una equivalencia aprobada (la pantalla lo pinta en verde). */
    public static final String ENTRO_COMO = "Entró como ";

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

    /** Atencion sin persona (LayoutNss.aceptaNssCeros). */
    public static final String NSS_CEROS = "00000000000";

    static final String AVISO_INTERNA ="incapacidad interna sin este dato: entra (no aplica a internas)";

    /** AREA y RAMO de incapacidades: obligatorios en IMSS, opcionales en INTERNA (ver validar()). */
    private boolean opcionalSiInterna(CampoNss c) {
        return layout == LayoutNss.INCAPACIDADES && ("AREA".equals(c.columna()) || "RAMO".equals(c.columna()));
    }

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
                if (p == null && c.puedeFaltar()) {
                    continue;   // columna nueva que una version anterior de la plantilla no trae: entra vacia
                }
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

    /**
     * Encabezados del archivo que no son de este layout: se ignoran al leer. Se avisan para que
     * nadie crea que una columna agregada al Excel se guardo.
     */
    public List<String> ignoradas(List<String> encabezados) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < encabezados.size(); i++) {
            String h = encabezados.get(i);
            if (h != null && !h.isBlank() && !indices.containsValue(i)) {
                res.add(h.trim());
            }
        }
        return res;
    }

    /** Encabezados que faltan para cada layout: sirve para decirle al usuario por que no se reconocio. */
    public static String faltantes(List<String> encabezados) {
        List<String> claves = encabezados.stream().map(h -> h == null ? "" : CatalogosNss.clave(h)).toList();
        StringBuilder sb = new StringBuilder();
        for (LayoutNss l : LayoutNss.values()) {
            List<String> faltan = l.campos().stream()
                    .filter(c -> !c.puedeFaltar())
                    .filter(c -> c.encabezadosAceptados().stream().noneMatch(e -> claves.contains(CatalogosNss.clave(e))))
                    .map(CampoNss::encabezado).toList();
            sb.append("\n    ").append(l.hoja()).append(": faltan ").append(faltan);
        }
        return sb.toString();
    }

    /** Columnas obligatorias de un layout que el archivo no trae (para el rechazo cuando se eligio ese tipo). */
    public static List<String> faltantes(List<String> encabezados, LayoutNss l) {
        List<String> claves = encabezados.stream().map(h -> h == null ? "" : CatalogosNss.clave(h)).toList();
        return l.campos().stream()
                .filter(c -> !c.puedeFaltar())
                .filter(c -> c.encabezadosAceptados().stream().noneMatch(e -> claves.contains(CatalogosNss.clave(e))))
                .map(CampoNss::encabezado).toList();
    }

    public LayoutNss layout() {
        return layout;
    }

    /** Columna del archivo donde viene la columna destino (NSS, PREDIO, FECHA...); -1 si no la trae. */
    public int indice(String columna) {
        return indices.entrySet().stream().filter(e -> columna.equals(e.getKey().columna()))
                .map(Map.Entry::getValue).findFirst().orElse(-1);
    }

    /**
     * ¿El valor seria valido en esa columna? Para detectar columnas cruzadas. TEXTO y MARCA no cuentan:
     * aceptan casi cualquier cosa y darian falsos cruces.
     */
    public boolean valeComo(String encabezado, String v) {
        return indices.keySet().stream().filter(c -> c.encabezado().equals(encabezado))
                .filter(c -> c.tipo() != CampoNss.Tipo.TEXTO && c.tipo() != CampoNss.Tipo.MARCA)
                .anyMatch(c -> {
                    try {
                        return convertir(c, v.trim()) != null;
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                });
    }

    public Resultado validar(LectorXlsx.Fila fila) {
        Map<String, Object> valores = new LinkedHashMap<>();
        List<Error> errores = new ArrayList<>();
        List<Error> avisos = new ArrayList<>();
        List<String> marcadas = new ArrayList<>();   // campos MARCA en 1, en el orden del layout
        // Obligatorios vacios que dependen del RUBRO (se lee despues en el renglon): se deciden al final
        List<CampoNss> pendientesInterna = new ArrayList<>();
        for (Map.Entry<CampoNss, Integer> e : indices.entrySet()) {
            CampoNss c = e.getKey();
            String crudo = fila.valor(e.getValue());
            String v = crudo == null ? "" : crudo.trim();
            if (c.tipo() == CampoNss.Tipo.MARCA) {
                // 1 = aplica; 0 o vacio = no aplica. Cualquier otra cosa (X, SI...) es error, para
                // que la captura sea uniforme (decision 13-oct-2026).
                if (v.matches("1(\\.0+)?")) {
                    marcadas.add(c.encabezado());
                } else if (!v.isEmpty() && !v.matches("0(\\.0+)?")) {
                    errores.add(new Error(fila.numero(), c.encabezado(), v, "solo se acepta 1 (aplica), 0 o vacio"));
                }
                continue;
            }
            if (v.isEmpty()) {
                if (c.obligatorio() && opcionalSiInterna(c)) {
                    pendientesInterna.add(c);
                } else if (c.obligatorio()) {
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
                // Variante conocida del catalogo (OFTALMOLOGICA -> OFTALMICO): entra con el valor oficial
                if (c.tipo() == CampoNss.Tipo.CATALOGO && valor != null
                        && !CatalogosNss.clave(v).equals(CatalogosNss.clave(valor.toString()))) {
                    // Solo llega aqui por una EQUIVALENCIA (OFTALMOLOGICA): entra y el mensaje lo dice. Las P1-P7
                    // de SM ya no: se rechazan (CatalogosNss.NO_ELEGIDOS, 3-nov-2026)
                    avisos.add(new Error(fila.numero(), c.encabezado(), v, ENTRO_COMO + valor
                            + " · equivalencia acordada con Servicio Médico: " + CatalogosNss.clave(v) + " = " + valor));
                }
            } catch (IllegalArgumentException ex) {
                errores.add(new Error(fila.numero(), c.encabezado(), v, ex.getMessage()));
            }
        }
        // Incapacidad INTERNA sin AREA o RAMO (14-oct-2026): en los Excel de servicio medico las internas
        // van en hoja aparte, sin ramo (EG/MAT/RT es del IMSS) y con "departamento" en lugar de area. Se
        // prevee desde el codigo: entran y queda AVISO en el .errores.csv para medirlo. IMSS: sigue error.
        boolean interna = "INTERNA".equals(valores.get("RUBRO"));
        for (CampoNss c : pendientesInterna) {
            if (interna) {
                avisos.add(new Error(fila.numero(), c.encabezado(), "", AVISO_INTERNA));
            } else {
                errores.add(new Error(fila.numero(), c.encabezado(), "", "es obligatorio"));
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
        // Antidoping: las sustancias marcadas se guardan juntas ("AMF, MET, THC"). Que el veredicto y
        // las sustancias no cuadren no rechaza el renglon (asi viene capturado), pero se avisa.
        if (layout == LayoutNss.ANTIDOPING) {
            valores.put("SUSTANCIA", marcadas.isEmpty() ? null : String.join(", ", marcadas));
            Object resultado = valores.get("RESULTADO");
            if ("POSITIVO".equals(resultado) && marcadas.isEmpty() && "ANTIDOPING".equals(valores.get("TIPO_PRUEBA"))) {
                avisos.add(new Error(fila.numero(), "RESULTADO", "POSITIVO", "positivo sin ninguna sustancia marcada"));
            } else if ("NEGATIVO".equals(resultado) && !marcadas.isEmpty()) {
                avisos.add(new Error(fila.numero(), "RESULTADO", "NEGATIVO",
                        "negativo con sustancia marcada (" + String.join(", ", marcadas) + ")"));
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
            case MARCA -> v;   // no llega aqui: las marcas se resuelven en validar()
            case CATALOGO -> CatalogosNss.buscar(c.catalogo(), v).orElseThrow(() ->
                    new IllegalArgumentException(CatalogosNss.elegidoEnLugarDe(c.catalogo(), v)
                            .map(e -> "no se acepta: Servicio Médico eligió «" + e + "»; cámbialo así en tu archivo")
                            .orElse("no esta en el catalogo " + c.catalogo()
                                    + " (validos: " + CatalogosNss.validos(c.catalogo()) + ")")));
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
