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

    /**
     * Tope del dibujo: 1 MB de PNG ya decodificado. Una firma trazada a mano pesa entre 5 y 50 KB,
     * asi que esto deja 20 veces de margen. NO se usa el tope de 25 MB de los adjuntos del
     * expediente (spring.servlet.multipart.max-file-size): ese es para documentos escaneados.
     */
    private static final int FIRMA_MAX_BYTES = 1024 * 1024;

    /** Los caracteres Base64 que caben en FIRMA_MAX_BYTES, con su relleno. Visible para la prueba. */
    static final int DIBUJO_MAX_CHARS = 4 * ((FIRMA_MAX_BYTES + 2) / 3);

    /**
     * El recuadro de dibujo manda un data URL PNG; solo se acepta ese formato.
     *
     * <p>Visibilidad de paquete en lugar de {@code private} para que la pruebe
     * {@code MiFirmaControllerTest}: es la frontera donde entra dato del cliente.
     */
    static byte[] desdeDataUrl(String dataUrl) {
        String prefijo = "data:image/png;base64,";
        if (dataUrl == null || !dataUrl.startsWith(prefijo)) {
            throw new IllegalArgumentException("Dibuja tu firma en el recuadro antes de guardar.");
        }
        String base64 = dataUrl.substring(prefijo.length());
        exigirBase64Estricto(base64);
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El dibujo de la firma llegó dañado; vuelve a intentarlo.");
        }
    }

    /**
     * Valida el Base64 ANTES de pasarlo al decodificador (CVE-2026-114938, «Java4Shell»).
     *
     * <p>El decodificador intrinseco de HotSpot calcula mal la mascara de escritura del ultimo grupo
     * cuando lleva relleno y escribe fuera del arreglo destino. En esta ruta el largo y el contenido
     * los elige por completo quien manda la peticion: lo unico que se revisaba era el prefijo
     * {@code data:image/png;base64,}, que el propio cliente escribe.
     *
     * <p>Se exige: largo acotado, multiplo de 4, solo el alfabeto Base64 estandar, y el relleno
     * {@code =} unicamente al final (uno o dos, sin datos despues). Con eso una cadena con relleno
     * mal formado no llega al decodificador.
     *
     * <p><b>Esto no sustituye actualizar el JDK</b> a 21.0.13.1 o posterior: es una contencion a
     * nivel de aplicacion, que es lo que recomienda el aviso de CISA mientras se parcha.
     */
    private static void exigirBase64Estricto(String base64) {
        if (base64.isEmpty()) {
            throw new IllegalArgumentException("Dibuja tu firma en el recuadro antes de guardar.");
        }
        if (base64.length() > DIBUJO_MAX_CHARS) {
            throw new IllegalArgumentException("El dibujo de la firma es demasiado grande; vuelve a intentarlo.");
        }
        if (base64.length() % 4 != 0) {
            throw new IllegalArgumentException("El dibujo de la firma llegó dañado; vuelve a intentarlo.");
        }
        int relleno = 0;
        for (int i = 0; i < base64.length(); i++) {
            char c = base64.charAt(i);
            boolean esDato = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '/';
            if (esDato) {
                if (relleno > 0) {
                    // dato despues del relleno: cadena mal formada
                    throw new IllegalArgumentException("El dibujo de la firma llegó dañado; vuelve a intentarlo.");
                }
                continue;
            }
            if (c == '=' && i >= base64.length() - 2) {
                relleno++;
                continue;
            }
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
