package com.onest.app.catalog.firma.web;

import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.firma.service.FirmaService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * «Mi firma digitalizada» (20-oct-2026): cada usuario del portal registra SU propia firma (dibujada o
 * imagen), acepta el aviso de privacidad, activa la que le cargo un administrador o la revoca. Nadie
 * registra la firma de otra persona desde aqui: todo es sobre el usuario en sesion.
 */
@Controller
public class MiFirmaController {

    private final FirmaService firmas;

    public MiFirmaController(FirmaService firmas) {
        this.firmas = firmas;
    }

    @GetMapping("/perfil/firma")
    public String pagina(@RequestParam(name = "ok", required = false) String ok,
                         @RequestParam(name = "error", required = false) String error, Model model) {
        String usuario = usuario();
        try {
            model.addAttribute("mi", firmas.miFirma(usuario));
        } catch (org.springframework.dao.DataAccessException ex) {
            // modulo sin tablas (SQL aun no corrido): la pagina lo dice en vez de tronar
            model.addAttribute("sinModulo", true);
        }
        model.addAttribute("ok", ok);
        model.addAttribute("error", error);
        return "pages/mi-firma";
    }

    @PostMapping("/perfil/firma/registrar")
    public String registrar(@RequestParam("origen") String origen,
                            @RequestParam(name = "dibujo", required = false) String dibujo,
                            @RequestParam(name = "imagen", required = false) MultipartFile imagen,
                            @RequestParam(name = "avisoVersion", defaultValue = "0") int avisoVersion,
                            @RequestParam(name = "acepto", required = false) String acepto,
                            HttpServletRequest request) {
        try {
            exigirAceptacion(acepto);
            byte[] bytes;
            if ("DIBUJO".equals(origen)) {
                bytes = desdeDataUrl(dibujo);
            } else if ("IMAGEN".equals(origen)) {
                if (imagen == null || imagen.isEmpty()) {
                    throw new IllegalArgumentException("Elige la imagen de tu firma (PNG o JPG).");
                }
                String nombre = imagen.getOriginalFilename() == null ? "" : imagen.getOriginalFilename().toLowerCase();
                if (!(nombre.endsWith(".png") || nombre.endsWith(".jpg") || nombre.endsWith(".jpeg"))) {
                    throw new IllegalArgumentException("Solo se aceptan imágenes PNG o JPG (PDF no).");
                }
                bytes = imagen.getBytes();
            } else {
                throw new IllegalArgumentException("Elige cómo registrar tu firma: dibujarla o subir una imagen.");
            }
            firmas.registrarPropia(usuario(), DocumentoImpresoService.usuarioActual(), bytes, origen, avisoVersion, request);
            return volver("ok", "Tu firma quedó registrada y vigente.");
        } catch (IllegalArgumentException ex) {
            return volver("error", ex.getMessage());
        } catch (IOException ex) {
            return volver("error", "No se pudo leer la imagen.");
        }
    }

    @PostMapping("/perfil/firma/aceptar")
    public String aceptar(@RequestParam(name = "avisoVersion", defaultValue = "0") int avisoVersion,
                          @RequestParam(name = "acepto", required = false) String acepto, HttpServletRequest request) {
        try {
            exigirAceptacion(acepto);
            firmas.aceptar(usuario(), avisoVersion, request);
            return volver("ok", "Aceptaste el aviso: tu firma está vigente.");
        } catch (IllegalArgumentException ex) {
            return volver("error", ex.getMessage());
        }
    }

    @PostMapping("/perfil/firma/revocar")
    public String revocar(HttpServletRequest request) {
        try {
            firmas.revocar(usuario(), request);
            return volver("ok", "Retiraste tu permiso: tu firma ya no sale en formatos nuevos.");
        } catch (IllegalArgumentException ex) {
            return volver("error", ex.getMessage());
        }
    }

    private static void exigirAceptacion(String acepto) {
        if (!"SI".equals(acepto)) {
            throw new IllegalArgumentException("Para usar tu firma debes leer y aceptar el aviso de privacidad.");
        }
    }

    /** El recuadro de dibujo manda un data URL PNG; solo se acepta ese formato. */
    private static byte[] desdeDataUrl(String dataUrl) {
        String prefijo = "data:image/png;base64,";
        if (dataUrl == null || !dataUrl.startsWith(prefijo)) {
            throw new IllegalArgumentException("Dibuja tu firma en el recuadro antes de guardar.");
        }
        try {
            return Base64.getDecoder().decode(dataUrl.substring(prefijo.length()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El dibujo de la firma llegó dañado; vuelve a intentarlo.");
        }
    }

    private static String volver(String clave, String mensaje) {
        return "redirect:/perfil/firma?" + clave + "=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private static String usuario() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "" : a.getName();
    }
}
