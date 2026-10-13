package com.onest.app.ayuda;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lado del usuario: el panel de chat (fragments/ayuda-panel.html, en todas las paginas) y el
 * centro de ayuda (/ayuda). Cualquier usuario autenticado; lo que ve se filtra por sus menus.
 */
@Controller
public class AyudaController {

    private final AyudaService service;
    private final AyudaProperties props;

    public AyudaController(AyudaService service, AyudaProperties props) {
        this.service = service;
        this.props = props;
    }

    /** Oculta (portal.ayuda.visible=false): las rutas de usuario no existen; el admin sigue en /admin/ayuda. */
    private void soloSiVisible() {
        if (!props.esVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La ayuda no está disponible todavía");
        }
    }

    @GetMapping("/ayuda")
    public String centro(Model model) {
        soloSiVisible();
        Map<String, List<AyudaArticulo>> porModulo = service.porModuloVisibles();
        model.addAttribute("porModulo", porModulo);
        model.addAttribute("nombresModulo", NOMBRES_MODULO);
        model.addAttribute("buscadorActivo", service.buscadorActivo());
        return "pages/ayuda";
    }

    public record PreguntaRequest(String q) {
    }

    @PostMapping(path = "/api/ayuda/buscar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AyudaService.Respuesta buscar(@RequestBody PreguntaRequest req) {
        soloSiVisible();
        return service.preguntar(req.q(), AyudaService.usuarioActual());
    }

    /** Articulo completo ya convertido a HTML, para "Leer" en el panel o en el centro de ayuda. */
    @GetMapping(path = "/api/ayuda/articulos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> articulo(@PathVariable("id") long id) {
        soloSiVisible();
        AyudaArticulo a = service.articuloVisible(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe o no está disponible para tu rol"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("clave", a.clave());
        m.put("modulo", a.modulo());
        m.put("titulo", a.titulo());
        m.put("html", Markdown.aHtml(a.cuerpo()));
        m.put("enlace", a.enlace());
        return m;
    }

    public record CalificacionRequest(Long articuloId, Boolean util) {
    }

    @PostMapping(path = "/api/ayuda/preguntas/{id}/calificar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String calificar(@PathVariable("id") long id, @RequestBody CalificacionRequest req) {
        soloSiVisible();
        service.calificar(id, req.articuloId(), Boolean.TRUE.equals(req.util()));
        return Boolean.TRUE.equals(req.util()) ? "¡Gracias!" : "Gracias, lo anotamos para mejorar la ayuda.";
    }

    /** Titulos legibles de los modulos (CODE de SERV_MED_MENU) para agrupar en el centro de ayuda. */
    static final Map<String, String> NOMBRES_MODULO = new LinkedHashMap<>();
    static {
        NOMBRES_MODULO.put("GENERAL", "Lo básico del portal");
        NOMBRES_MODULO.put("EXPEDIENTE", "Consultas médicas");
        NOMBRES_MODULO.put("INCAPACIDAD", "Incapacidades");
        NOMBRES_MODULO.put("EXAMEN_MEDICO", "Examen médico y archivos");
        NOMBRES_MODULO.put("EXAMEN_INICIAL", "Examen médico inicial");
        NOMBRES_MODULO.put("EXAMEN_INICIAL_BANDEJA", "Bandeja de exámenes iniciales");
        NOMBRES_MODULO.put("PRETEST", "Pre-Test");
        NOMBRES_MODULO.put("ANTIDOPING", "Antidoping");
        NOMBRES_MODULO.put("ANTIDOPING_SELECCION", "Antidoping - Selección");
        NOMBRES_MODULO.put("ACCIDENTES", "Accidentes");
        NOMBRES_MODULO.put("MATERNIDAD", "Maternidad");
        NOMBRES_MODULO.put("CAUSAS_CONSULTA", "Causas de consulta");
        NOMBRES_MODULO.put("CONSUMIBLES", "Consumibles");
    }
}
