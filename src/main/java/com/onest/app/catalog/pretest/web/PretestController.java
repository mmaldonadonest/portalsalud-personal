package com.onest.app.catalog.pretest.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.nss.service.NssSearchService;
import com.onest.app.catalog.pretest.service.PretestService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pre-Test (U03, id 11). Replica pretest.php (form pre-cargado) y savePretest.php (guardado).
 */
@Controller
@RequestMapping("/api/nss")
public class PretestController {

    private final PretestService pretestService;
    private final NssSearchService nssSearchService;

    public PretestController(PretestService pretestService, NssSearchService nssSearchService) {
        this.pretestService = pretestService;
        this.nssSearchService = nssSearchService;
    }

    @PostMapping(
            path = "/pretest",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("nss", data == null ? "" : data.trim());
            model.addAttribute("p", pretestService.load(data));
            // El boton Imprimir lee lo GUARDADO, no lo que esta en pantalla: se muestra la fecha
            // de la ultima version y se deshabilita si nunca se ha guardado.
            model.addAttribute("ultimoGuardado", fechaCorta(pretestService.ultimoGuardado(data)));
            return "fragments/pretest-form :: form";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Documento imprimible del Pre-Test (declaracion firmada del trabajador). Datos de SERV_MED_TAG
     * + ficha del empleado; el PDF lo genera el navegador; {@code auto=1} abre el dialogo al
     * cargar. Ver docs/plan-impresion-pretest-incapacidad-consulta.md.
     */
    @GetMapping(path = "/pretest/imprimir", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Pretest", accion = "export", entidad = "Pre-Test impreso", registro = "nss")
    public String imprimir(@RequestParam("nss") String nss,
                           @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        Map<String, String> crudo = pretestService.load(nssLimpio);
        LocalDateTime guardado = pretestService.ultimoGuardado(nssLimpio);
        model.addAttribute("ultimoGuardado", fechaCorta(guardado));
        model.addAttribute("sinDatos", guardado == null);
        Map<String, String> p = new HashMap<>();
        crudo.forEach((k, v) -> p.put(k, DocumentoImpresoService.texto(v)));
        Map<String, String> respuestas = new HashMap<>();
        Map<String, String> observaciones = new HashMap<>();
        for (PretestService.Pregunta q : PretestService.SALUD) {
            respuestas.put(q.codigo(), DocumentoImpresoService.siNo(crudo.get(q.codigo())));
            observaciones.put(q.codigo(), DocumentoImpresoService.texto(crudo.get(q.codigo() + "OBS")));
        }
        for (PretestService.Pregunta q : PretestService.SINTOMAS) {
            respuestas.put(q.codigo(), DocumentoImpresoService.siNo(crudo.get(q.codigo())));
            observaciones.put(q.codigo(), DocumentoImpresoService.texto(crudo.get(q.codigo() + "OBS")));
        }
        String domicilio = String.join(" ", List.of(p.getOrDefault("calleIn", ""), p.getOrDefault("numeroCin", ""),
                p.getOrDefault("coloniaInp", ""), p.getOrDefault("delegOmUn", ""))).replaceAll("\\s+", " ").trim();
        var empleado = DocumentoImpresoService.empleado(nssSearchService, nssLimpio);

        model.addAttribute("nss", nssLimpio);
        model.addAttribute("p", p);
        model.addAttribute("domicilio", domicilio);
        model.addAttribute("salud", PretestService.SALUD);
        model.addAttribute("sintomas", PretestService.SINTOMAS);
        model.addAttribute("respuestas", respuestas);
        model.addAttribute("observaciones", observaciones);
        model.addAttribute("empleado", empleado);
        model.addAttribute("nombreTrabajador", DocumentoImpresoService.nombreCompleto(empleado, nssLimpio));
        model.addAttribute("firmaTrabajador", DocumentoImpresoService.firma(crudo.get("drawdataUrl")));
        model.addAttribute("capturo", DocumentoImpresoService.usuarioActual());
        model.addAttribute("ahora", DocumentoImpresoService.ahora());
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/pretest-documento";
    }

    private static String fechaCorta(LocalDateTime t) {
        return t == null ? null : t.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @PostMapping(
            path = "/pretest/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Pretest", accion = "create", entidad = "Pretest", registro = "nss")
    public String save(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            Map<String, String> fields = new HashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"_csrf".equals(key)) {
                    fields.put(key, (values == null || values.isEmpty()) ? null : values.get(0));
                }
            });
            pretestService.save(nss, fields);
            return "Pre-Test guardado.";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
