package mx.saludocupacional.portal.security.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Objetos de entrada y salida del proceso de autenticación. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Credenciales enviadas al iniciar sesión. */
    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {
    }

    /** Respuesta con el token y el perfil del usuario autenticado. */
    public record LoginResponse(
            String token,
            long expiraEnMinutos,
            UsuarioResponse usuario
    ) {
    }

    /** Perfil del usuario tal como lo necesita la interfaz. */
    public record UsuarioResponse(
            Long id,
            String email,
            String nombre,
            String rol,
            Set<String> permisos,
            Set<Long> prediosPermitidos,
            boolean alcanceGlobal
    ) {
    }

    /** Cambio de contraseña del propio usuario. */
    public record CambioPasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria")
            String passwordActual,

            @NotBlank(message = "La nueva contraseña es obligatoria")
            @Size(min = 10, message = "La nueva contraseña debe tener al menos 10 caracteres")
            String passwordNueva
    ) {
    }
}
