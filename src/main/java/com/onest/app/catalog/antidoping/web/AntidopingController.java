package com.onest.app.catalog.antidoping.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.service.AntidopingService;
import com.onest.app.catalog.antidoping.service.CustodiaService;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.formato.service.FormatosImpresosService;
import com.onest.app.catalog.nss.client.NssSearchClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Antidoping / alcoholimetria: lista y alta. Backend aplicado y verificado 2026-08-14
 * contra el WS ORDS real (docs/ords-antidoping.sql). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class AntidopingController {

    private final AntidopingService antidopingService;
    private final FormatosImpresosService formatos;
    private final NssSearchClient nssClient;
    private final CustodiaService custodia;

    public AntidopingController(AntidopingService antidopingService, FormatosImpresosService formatos,
                                NssSearchClient nssClient, CustodiaService custodia) {
        this.antidopingService = antidopingService;
        this.formatos = formatos;
        this.nssClient = nssClient;
        this.custodia = custodia;
    }

    @PostMapping(
            path = "/antidoping",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String antidoping(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("antidopings", antidopingService.byNss(data));
            model.addAttribute("nss", data == null ? "" : data.trim());
            return "fragments/antidoping-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de antidoping/alcoholimetria. */
    @PostMapping(
            path = "/antidoping/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/antidoping-form :: form";
    }

    /**
     * FT-SO-22 Aceptacion a CECOSAMA (rev. 05, 20-oct-2026, formato B "por condicion"): solo para una
     * prueba POSITIVA, que se vuelve a revisar aqui aunque la lista solo muestre el boton en esas.
     * El WS guarda la fecha sin hora y no captura lo que el trabajador "esta ingiriendo": ambos quedan
     * en blanco para llenarse a mano. Bajo /api/nss/antidoping para heredar el permiso del modulo.
     */
    @GetMapping(path = "/antidoping/cecosama", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Antidoping", accion = "export", entidad = "Aceptacion a CECOSAMA FT-SO-22", registro = "nss")
    public String cecosama(@RequestParam("nss") String nss, @RequestParam("id") String id,
                           @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        AntidopingDto prueba = prueba(nssLimpio, id);
        if (!"POSITIVO".equalsIgnoreCase(prueba.resultado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La aceptación a CECOSAMA solo aplica a pruebas con resultado positivo.");
        }
        model.addAttribute("prueba", prueba);
        model.addAttribute("sustancia", sustanciaTexto(prueba));
        model.addAttribute("fecha", partesFecha(prueba.fechaRegistro()));
        model.addAttribute("nombre", nombreTrabajador(nssLimpio));
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("enc", formatos.encabezado("FT-SO-22"));
        formatos.aplicarFirmas(model, "FT-SO-22", "ANTIDOPING:" + id, nssLimpio);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/formato-ft-so-22";
    }

    private static final Map<String, String> SUSTANCIAS = Map.ofEntries(
            Map.entry("AMF", "anfetaminas"), Map.entry("BARB", "barbitúricos"), Map.entry("BENZO", "benzodiacepinas"),
            Map.entry("COC", "cocaína"), Map.entry("LSD", "LSD"), Map.entry("MET", "metanfetaminas"),
            Map.entry("OPI", "opiáceos"), Map.entry("PVC", "fenciclidina"), Map.entry("THC", "marihuana"),
            Map.entry("SOLVENTE", "solventes"));

    /** Lo que va en "siendo positivo a ___": nombre de la sustancia; alcoholemia = alcohol; "OTRO" queda en blanco. */
    private static String sustanciaTexto(AntidopingDto a) {
        if ("ALCOHOLEMIA".equalsIgnoreCase(a.tipoPrueba())) {
            return "alcohol";
        }
        String s = a.sustancia() == null ? "" : a.sustancia().trim().toUpperCase(Locale.ROOT);
        return SUSTANCIAS.getOrDefault(s, "OTRO".equals(s) || "0".equals(s) ? "" : s);
    }

    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    /** "DD/MM/YYYY" del WS -> [dia, mes en letra, anio]; vacios si no se puede leer (se llena a mano). */
    private static String[] partesFecha(String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha == null ? "" : fecha.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return new String[] {String.valueOf(f.getDayOfMonth()), MESES[f.getMonthValue() - 1], String.valueOf(f.getYear())};
        } catch (DateTimeParseException ex) {
            return new String[] {"", "", ""};
        }
    }

    /** Ficha del trabajador de Catalogo/usuario (solo lectura; no usa NssSearchService, que puede dar de ALTA). */
    private java.util.Optional<EmpleadoDto> empleado(String nss) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return nssClient.findAsociaciones(nss, auth == null ? "SISTEMA" : auth.getName()).stream().findFirst();
        } catch (RuntimeException ex) {
            return java.util.Optional.empty();
        }
    }

    private String nombreTrabajador(String nss) {
        return empleado(nss)
                .map(e -> String.join(" ", nvl(e.nombre()), nvl(e.apellidoPaterno()), nvl(e.apellidoMaterno())).trim()
                        .replaceAll("\\s+", " "))
                .orElse("");
    }

    /** Prueba del NSS por REG_ID; 400 si no existe. */
    private AntidopingDto prueba(String nss, String id) {
        try {
            return antidopingService.byNss(nss).stream()
                    .filter(a -> id.equals(a.idRegistro()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la prueba " + id + " del NSS " + nss + "."));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * FT-SO-14 Cadena de custodia (rev. 05, 20-oct-2026, formato B "siempre"): una por prueba. La ficha
     * sale de Catalogo/usuario, la prueba del WS y lo demas de la custodia capturada en el alta
     * (CustodiaService); en pruebas anteriores al 20-oct esos campos salen en blanco para llenarse a mano.
     */
    @GetMapping(path = "/antidoping/custodia", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Antidoping", accion = "export", entidad = "Cadena de custodia FT-SO-14", registro = "nss")
    public String cadenaCustodia(@RequestParam("nss") String nss, @RequestParam("id") String id,
                                 @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        AntidopingDto p = prueba(nssLimpio, id);
        EmpleadoDto e = empleado(nssLimpio).orElse(null);
        Map<String, String> c = custodia.leer(nssLimpio, id);
        model.addAttribute("prueba", p);
        model.addAttribute("e", e);
        model.addAttribute("sexo", e == null ? "" : ("1".equals(nvl(e.sexo())) ? "Hombre" : ("2".equals(nvl(e.sexo())) ? "Mujer" : "")));
        model.addAttribute("c", c);
        // sin custodia capturada no se sabe que detecto el kit: casillas vacias (no "las 7")
        java.util.List<String> marcados = java.util.Arrays.asList(c.getOrDefault("ELEMENTOS", "").split(","));
        model.addAttribute("elementos", CustodiaService.ELEMENTOS);
        model.addAttribute("marcados", marcados);
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("enc", formatos.encabezado("FT-SO-14"));
        formatos.aplicarFirmas(model, "FT-SO-14", "ANTIDOPING:" + id, nssLimpio);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/formato-ft-so-14";
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    /** Guarda el registro de antidoping (POST /Servcio/antidoping). */
    @PostMapping(
            path = "/antidoping/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Antidoping", accion = "create", entidad = "Prueba antidoping", registro = "nss", detalle = {"tipoPrueba"})
    public String save(@ModelAttribute AntidopingAltaForm form) {
        try {
            String proceso = antidopingService.crearAntidoping(form);
            String mensaje = (proceso == null || proceso.isBlank()) ? "Registro de antidoping guardado." : proceso;
            // la custodia va despues del WS: si el WS falla no queda custodia huerfana; si falla la
            // custodia, la prueba ya quedo y solo se avisa (el FT-SO-14 saldria con esos campos en blanco)
            try {
                java.util.Optional<String> idPrueba = custodia.guardarDeAlta(form.getNss().trim(), form);
                if (idPrueba.isEmpty()) {
                    mensaje += " Aviso: no se pudieron ligar los datos de cadena de custodia (FT-SO-14).";
                } else {
                    // quien dio de alta la prueba es quien elaboro sus formatos (segunda firma)
                    formatos.registrarAutor("ANTIDOPING:" + idPrueba.get(), form.getNss().trim());
                }
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(AntidopingController.class).error("[custodia] NSS {}", form.getNss(), ex);
                mensaje += " Aviso: no se guardaron los datos de cadena de custodia (FT-SO-14).";
            }
            return mensaje;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
