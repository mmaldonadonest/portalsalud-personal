package mx.saludocupacional.portal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.saludocupacional.portal.IntegrationTestBase;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el inicio de sesión y la protección de las rutas. */
@AutoConfigureMockMvc
class AutenticacionTest extends IntegrationTestBase {

    private static final String ADMIN = "admin@saludocupacional.mx";
    private static final String PASSWORD = "CambiarEnProduccion123!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("La comprobación de salud es pública")
    void saludEsPublica() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("Con credenciales correctas entrega token, perfil y cookie de sesión")
    void loginCorrecto() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(ADMIN, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.usuario.alcanceGlobal").value(true))
                .andExpect(jsonPath("$.usuario.permisos").isNotEmpty())
                .andExpect(cookie().exists("portal_session"))
                .andExpect(cookie().httpOnly("portal_session", true));
    }

    @Test
    @DisplayName("Con contraseña incorrecta responde no autorizado sin revelar la causa")
    void loginIncorrecto() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(ADMIN, "contraseña-equivocada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas o sesión expirada"));
    }

    @Test
    @DisplayName("Un correo mal formado se rechaza con detalle por campo")
    void correoInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("no-es-correo", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").isArray());
    }

    @Test
    @DisplayName("Sin token, una ruta protegida no entrega datos")
    void rutaProtegidaSinToken() throws Exception {
        mockMvc.perform(get("/api/auth/perfil"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Con el token emitido se obtiene el perfil del usuario")
    void perfilConToken() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(ADMIN, PASSWORD))))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(cuerpo).get("token").asText();

        mockMvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN));
    }
}
