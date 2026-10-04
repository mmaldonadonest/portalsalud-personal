package com.onest.app.notification;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de la campanita (fragments/header.html + scripts.html la sondea cada tanto).
 * Solo lectura de lo que le corresponde al usuario y marcar leidas; crear notificaciones es
 * cosa de los modulos (NotificacionService.crear), no de esta API.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> pendientes() {
        List<NotificacionService.Item> items = service.pendientesDelUsuario();
        return Map.of("total", items.size(), "items", items);
    }

    @PostMapping("/{id}/leida")
    public Map<String, Object> leida(@PathVariable("id") long id) {
        service.marcarLeida(id);
        return Map.of("ok", true);
    }

    /** Cuerpo: {@code {"ids":[1,2,3]}}. */
    @PostMapping("/leidas")
    public Map<String, Object> leidas(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body == null ? null : body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            service.marcarLeidas(ids);
        }
        return Map.of("ok", true);
    }
}
