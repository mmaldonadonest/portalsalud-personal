package com.onest.app.ayuda;

import com.onest.app.security.permission.PermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * {@code ayudaVisible} en todas las vistas: boton "Ayuda" del header y panel del chat. Desde el 25-oct-2026
 * se asigna POR ROL con el menu AYUDA (Administracion > Roles > Menus), igual que el grupo Examenes; con
 * fuente de permisos ORDS no hay menus locales y se ve para todos, como antes.
 */
@ControllerAdvice
public class AyudaViewAdvice {

    static final String MENU_AYUDA = "AYUDA";

    private final AyudaProperties props;
    private final PermissionService permissionService;
    private final boolean fuenteLocal;

    public AyudaViewAdvice(AyudaProperties props, PermissionService permissionService,
                           @Value("${portal.permissions.source:ORDS}") String permissionsSource) {
        this.props = props;
        this.permissionService = permissionService;
        this.fuenteLocal = "LOCAL".equalsIgnoreCase(permissionsSource);
    }

    @ModelAttribute("ayudaVisible")
    public boolean ayudaVisible() {
        if (!props.esVisible()) {
            return false;
        }
        if (!fuenteLocal) {
            return true;
        }
        try {
            return permissionService.tieneAccesoPorCodigo(MENU_AYUDA);
        } catch (RuntimeException ex) {
            return false; // sin sesion (login) o sin tablas de permisos: no se muestra
        }
    }
}
