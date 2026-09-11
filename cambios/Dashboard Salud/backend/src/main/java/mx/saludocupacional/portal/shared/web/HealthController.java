package mx.saludocupacional.portal.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/** Comprobación de disponibilidad para balanceadores y contenedores. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> estado() {
        return Map.of(
                "status", "ok",
                "aplicacion", "Portal Salud Ocupacional",
                "timestamp", OffsetDateTime.now().toString());
    }
}
