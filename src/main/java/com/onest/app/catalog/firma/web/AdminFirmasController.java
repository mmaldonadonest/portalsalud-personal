package com.onest.app.catalog.firma.web;

import com.onest.app.catalog.firma.service.FirmaService;
import com.onest.app.security.repository.AppSecUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Administracion de firmas digitalizadas (20-oct-2026, solo ROLE_ADMIN por /admin/**): ver el estado de
 * todas, cargar una firma existente (escaneo) que queda PENDIENTE hasta que su dueno acepte el aviso, y
 * desactivar una firma. Aqui nunca se ven las imagenes ni se deja vigente una firma.
 */
@Controller
public class AdminFirmasController {

    private final FirmaService firmas;
    private final AppSecUserRepository usuarios;

    public AdminFirmasController(FirmaService firmas, AppSecUserRepository usuarios) {
        this.firmas = firmas;
        this.usuarios = usuarios;
    }

    @GetMapping("/admin/firmas")
    public String pagina(@RequestParam(name = "ok", required = false) String ok,
                         @RequestParam(name = "error", required = false) String error, Model model) {
        String errorTabla = null;
        try {
            model.addAttribute("firmas", firmas.todas());
        } catch (org.springframework.dao.DataAccessException ex) {
            model.addAttribute("firmas", java.util.List.of());
            errorTabla = "Faltan las tablas del módulo de firmas (docs/portal-firmas-digitalizadas.sql).";
        }
        model.addAttribute("habilitado", firmas.habilitado());
        model.addAttribute("ok", ok);
        model.addAttribute("error", error != null ? error : errorTabla);
        return "pages/admin-firmas";
    }

    @PostMapping("/admin/firmas/cargar")
    public String cargar(@RequestParam("usuario") String usuario, @RequestParam("imagen") MultipartFile imagen,
                         HttpServletRequest request) {
        try {
            String u = usuario == null ? "" : usuario.trim();
            var dueno = usuarios.findByUsernameIgnoreCase(u)
                    .orElseThrow(() -> new IllegalArgumentException("El usuario " + u + " no existe en el portal."));
            String nombre = imagen.getOriginalFilename() == null ? "" : imagen.getOriginalFilename().toLowerCase();
            if (!(nombre.endsWith(".png") || nombre.endsWith(".jpg") || nombre.endsWith(".jpeg"))) {
                throw new IllegalArgumentException("Solo se aceptan imágenes PNG o JPG (PDF no).");
            }
            firmas.cargarPorAdmin(dueno.getUsername(), dueno.getDisplayName(), imagen.getBytes(), admin(), request);
            return volver("ok", "Firma cargada para " + dueno.getUsername() + ". Queda pendiente hasta que su dueño acepte el aviso en «Mi firma digitalizada».");
        } catch (IllegalArgumentException ex) {
            return volver("error", ex.getMessage());
        } catch (IOException ex) {
            return volver("error", "No se pudo leer la imagen.");
        }
    }

    @PostMapping("/admin/firmas/{id}/desactivar")
    public String desactivar(@PathVariable("id") long id, @RequestParam(name = "motivo", required = false) String motivo,
                             HttpServletRequest request) {
        try {
            firmas.desactivar(id, admin(), motivo, request);
            return volver("ok", "Firma " + id + " desactivada.");
        } catch (IllegalArgumentException ex) {
            return volver("error", ex.getMessage());
        }
    }

    private static String volver(String clave, String mensaje) {
        return "redirect:/admin/firmas?" + clave + "=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private static String admin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SISTEMA" : a.getName();
    }
}
