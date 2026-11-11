package com.onest.app.catalog.antidoping.service;

import com.onest.app.catalog.antidoping.client.AntidopingClient;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.web.AntidopingAltaForm;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Datos de la cadena de custodia FT-SO-14 (20-oct-2026) que el WS de antidoping no guarda:
 * identificacion, razon de la prueba, elementos a detectar, % de alcohol, tratamiento medico,
 * hora, lugar y responsable de la toma. Van a SERV_MED_TAG (base del portal) con TYPE
 * "FTSO14.<REG_ID>.<CAMPO>", uno por prueba.
 * <p>
 * El alta del WS no regresa el REG_ID: se toma la prueba mas reciente del NSS que coincide con
 * lo recien guardado (hoy, tipo, resultado, folio). Es la que acaba de crear este usuario.
 */
@Service
public class CustodiaService {

    private static final Logger log = LoggerFactory.getLogger(CustodiaService.class);
    static final String GRUPO = "CUSTODIA_FTSO14";
    public static final List<String> ELEMENTOS = List.of("AMP", "COC", "OPI", "THC", "MET", "BENZO", "BARB");

    private final AntidopingClient client;
    private final MedTagRepository tags;

    public CustodiaService(AntidopingClient client, MedTagRepository tags) {
        this.client = client;
        this.tags = tags;
    }

    /** Guarda la custodia de la prueba recien dada de alta. Devuelve el REG_ID usado, o vacio si no se encontro. */
    public Optional<String> guardarDeAlta(String nss, AntidopingAltaForm form) {
        Optional<String> id = idRecienCreado(nss, form);
        if (id.isEmpty()) {
            log.warn("[custodia] no se encontro la prueba recien creada del NSS {}; FT-SO-14 sin datos de custodia", nss);
            return id;
        }
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("IDENT_TIPO", form.getIdentTipo());
        campos.put("IDENT_EMITIDA", form.getIdentEmitida());
        campos.put("IDENT_NUMERO", form.getIdentNumero());
        campos.put("RAZON", form.getRazon());
        campos.put("RAZON_OTRO", "OTRO".equals(form.getRazon()) ? form.getRazonOtro() : null);
        if ("ANTIDOPING".equals(form.getTipoPrueba())) {
            // solo los del formato y en su orden; "ninguno marcado" se guarda vacio a proposito
            List<String> marcados = form.getElementos() == null ? List.of() : form.getElementos();
            campos.put("ELEMENTOS", String.join(",", ELEMENTOS.stream().filter(marcados::contains).toList()));
        }
        campos.put("BAC", "ALCOHOLEMIA".equals(form.getTipoPrueba()) ? form.getBac() : null);
        campos.put("TRATAMIENTO", form.getTratamiento());
        boolean conTratamiento = "SI".equals(form.getTratamiento());
        campos.put("DIAGNOSTICO", conTratamiento ? form.getDiagnostico() : null);
        campos.put("MEDICAMENTO", conTratamiento ? form.getMedicamento() : null);
        campos.put("RECETADO_POR", conTratamiento ? form.getRecetadoPor() : null);
        campos.put("HORA", form.getHoraToma());
        campos.put("LUGAR", form.getLugar());
        campos.put("RESPONSABLE", form.getResponsable());
        // C-077 (10-nov, Word SM punto 15): fecha de aplicacion (el WS solo guarda la de registro) y la lista completa
        // de sustancias de un antidoping positivo (el WS solo guarda 20 caracteres)
        campos.put("FECHA_APLICACION", form.getFechaAplicacion());
        if ("ANTIDOPING".equals(form.getTipoPrueba()) && "POSITIVO".equals(form.getResultado())
                && form.getSustancias() != null && !form.getSustancias().isEmpty()) {
            campos.put("SUSTANCIAS", String.join(",", limpias(form.getSustancias())));
        }
        String usuario = usuarioActual();
        String prefijo = "FTSO14." + id.get() + ".";
        campos.forEach((campo, valor) -> {
            String v = valor == null ? "" : valor.trim();
            if (!v.isEmpty() || "ELEMENTOS".equals(campo)) {
                tags.upsert(nss, prefijo + campo, v, GRUPO, usuario);
            }
        });
        return id;
    }

    /** C-034 / C-077: liga las dos pruebas de la misma toma (antidoping y alcoholimetria) para el FT-SO-14. */
    public void ligarPar(String nss, String idA, String idB) {
        String usuario = usuarioActual();
        tags.upsert(nss, "FTSO14." + idA + ".PAR", idB, GRUPO, usuario);
        tags.upsert(nss, "FTSO14." + idB + ".PAR", idA, GRUPO, usuario);
    }

    /** Ancho de SERV_MED_ANTIDOPING_RESULTADO.SUSTANCIA (docs/ords-antidoping.sql): pasarse truena el alta. */
    static final int ANCHO_SUSTANCIA_WS = 20;

    /**
     * C-077: lo que va al WS en SUSTANCIA con varias marcadas: las claves separadas por coma si caben en 20
     * caracteres; si no, «VARIAS» (la lista completa queda en la custodia, tag SUSTANCIAS). Nada marcado = null.
     */
    public static String sustanciaParaWs(List<String> sustancias) {
        List<String> l = sustancias == null ? List.of() : limpias(sustancias);
        if (l.isEmpty()) {
            return null;
        }
        String unidas = String.join(",", l);
        return unidas.length() <= ANCHO_SUSTANCIA_WS ? unidas : "VARIAS";
    }

    /** Texto para el impreso: la lista de la custodia o, si no hay, lo del WS («0» y «VARIAS» sin lista = vacio). */
    public static String sustanciasTexto(String listaCustodia, String delWs) {
        String s = listaCustodia != null && !listaCustodia.isBlank() ? listaCustodia : (delWs == null ? "" : delWs);
        s = s.trim();
        return "0".equals(s) || "VARIAS".equals(s) ? "" : s.replace(",", ", ");
    }

    private static List<String> limpias(List<String> l) {
        return l.stream().filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().toList();
    }

    /** Campos de custodia de una prueba (sin el prefijo); vacio para pruebas anteriores al FT-SO-14. */
    public Map<String, String> leer(String nss, String regId) {
        String prefijo = "FTSO14." + regId + ".";
        Map<String, String> r = new LinkedHashMap<>();
        tags.latestByNssAndTypePrefix(nss, prefijo).forEach((k, v) -> r.put(k.substring(prefijo.length()), v == null ? "" : v));
        return r;
    }

    private Optional<String> idRecienCreado(String nss, AntidopingAltaForm form) {
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String folio = limpio(form.getFolio());
        return client.findAntidopings(nss).stream()
                .filter(a -> hoy.equals(limpio(a.fechaRegistro())))
                .filter(a -> Objects.equals(limpio(form.getTipoPrueba()), limpio(a.tipoPrueba())))
                .filter(a -> Objects.equals(limpio(form.getResultado()), limpio(a.resultado())))
                // el WS regresa "0" cuando el folio va vacio
                .filter(a -> folio.isEmpty() ? (limpio(a.folio()).isEmpty() || "0".equals(limpio(a.folio()))) : folio.equals(limpio(a.folio())))
                .map(AntidopingDto::idRegistro)
                .filter(i -> i != null && i.matches("\\d+"))
                .max((x, y) -> Long.compare(Long.parseLong(x), Long.parseLong(y)));
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
