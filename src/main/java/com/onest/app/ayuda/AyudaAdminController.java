package com.onest.app.ayuda;

import com.onest.app.audit.web.Auditado;
import jakarta.servlet.http.HttpServletRequest;
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
 * Administracion de los articulos de ayuda (rol ADMIN; /admin/** lo protege SecurityConfiguration).
 * El servicio medico mantiene su propia ayuda desde aqui: alta, edicion, activar/desactivar,
 * reindexar el buscador y ver las preguntas que nadie pudo responder.
 */
@Controller
public class AyudaAdminController {

    private final AyudaService service;

    public AyudaAdminController(AyudaService service) {
        this.service = service;
    }

    @GetMapping("/admin/ayuda")
    public String pagina(Model model) {
        model.addAttribute("articulos", service.todos());
        model.addAttribute("sinRespuesta", service.preguntasSinRespuesta());
        model.addAttribute("nombresModulo", AyudaController.NOMBRES_MODULO);
        model.addAttribute("buscadorActivo", service.buscadorActivo());
        return "pages/admin-ayuda";
    }

    @GetMapping(path = "/api/admin/ayuda/articulos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AyudaArticulo articulo(@PathVariable("id") long id) {
        return service.porId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"));
    }

    public record ArticuloRequest(Long id, String clave, String modulo, String titulo, String pregunta, String palabrasClave,
                                  String cuerpo, String enlace, Integer orden, Boolean activo) {
    }

    @PostMapping(path = "/api/admin/ayuda/articulos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auditado(modulo = "Ayuda", accion = "update", entidad = "Articulo de ayuda", registro = "titulo")
    public Map<String, Object> guardar(@RequestBody ArticuloRequest r, HttpServletRequest request) {
        try {
            AyudaArticulo a = new AyudaArticulo(r.id() == null ? 0 : r.id(), r.clave(), r.modulo(), r.titulo(), r.pregunta(),
                    r.palabrasClave(), r.cuerpo(), r.enlace(), r.orden() == null ? 100 : r.orden(), r.activo() == null || r.activo(), null, null);
            long id = service.guardar(a, AyudaService.usuarioActual());
            return Map.of("id", id, "mensaje", "Artículo guardado.");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(path = "/api/admin/ayuda/articulos/{id}/activar", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String activar(@PathVariable("id") long id, @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        service.activar(id, activo, AyudaService.usuarioActual());
        return activo ? "Artículo activado." : "Artículo desactivado.";
    }

    @PostMapping(path = "/api/admin/ayuda/reindexar", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String reindexar() {
        return service.reindexar();
    }
}
