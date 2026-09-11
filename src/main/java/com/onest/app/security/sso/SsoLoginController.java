package com.onest.app.security.sso;

import com.onest.app.catalog.module.client.ModulePermissionClient;
import com.onest.app.security.service.PortalPrincipalResolver;
import com.onest.app.security.service.PortalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entrada SSO del launcher (ver ejemplo de referencia com.onest.portalcompras.SsoLoginAction).
 * El launcher manda un JWT (access_token) firmado por el IdP; se valida la firma + issuer via
 * JWKS (SsoJwtConfiguration).
 *
 * <p>Regla del launcher para el NSS: SOLO sale de {@code claims.get("nss")} despues de validar
 * el token, sin fallback a parametros de request - si el claim viene vacio, se rechaza el
 * login completo (ver bloque al inicio de {@link #ssoLogin}). email/preferred_username/upn
 * se resuelven aparte (misma cadena de fallback del ejemplo de referencia) solo para el avatar
 * de Intranet, que es un dato secundario (no de identidad) - por eso ese si degrada a "usuario"
 * del request si el token no trae ninguno.
 *
 * <p>No pasa por AuthenticationManager/LegacyPhpAuthenticationProvider (no hay password que
 * validar) - arma el principal via {@link PortalPrincipalResolver} (el mismo que usa el login
 * por password) y establece el SecurityContext a mano, replicando lo que hace
 * UsernamePasswordAuthenticationFilter tras un login exitoso (proteccion de session fixation +
 * persistencia del contexto en la sesion HTTP).
 *
 * <p>Que el JWT sea valido solo prueba que el IdP conoce a esa persona - NO que tenga
 * acceso a Portal Salud. Por eso, con el NSS ya resuelto se valida contra
 * {@link ModulePermissionClient#findRoleId} (mismo gateway ORDS que ya arma el menu
 * lateral, .../info/consulta_app_rol_usuario con el id_app de Salud/Biometrico): sin
 * rol asignado ahi, no entra, sin importar que tan valido sea el token.
 */
@Controller
public class SsoLoginController {

    private static final Logger log = LoggerFactory.getLogger(SsoLoginController.class);

    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final PortalPrincipalResolver principalResolver;
    private final ModulePermissionClient modulePermissionClient;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SessionAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    public SsoLoginController(
            ObjectProvider<JwtDecoder> jwtDecoderProvider, PortalPrincipalResolver principalResolver,
            ModulePermissionClient modulePermissionClient) {
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.principalResolver = principalResolver;
        this.modulePermissionClient = modulePermissionClient;
    }

    @PostMapping("/sso/login")
    public String ssoLogin(
            @RequestParam("access_token") String accessToken,
            @RequestParam(name = "usuario", required = false) String usuarioParam,
            @RequestParam(name = "perfil", required = false) String perfil,
            HttpServletRequest request,
            HttpServletResponse response) {

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder == null) {
            log.warn("[sso] intento de login SSO pero portal.sso.jwks-uri no esta configurado en este ambiente");
            return "redirect:/login?error";
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(accessToken);
        } catch (JwtException ex) {
            log.warn("[sso] token invalido: {}", ex.getMessage());
            return "redirect:/login?error";
        }

        // Regla del launcher: el NSS SOLO sale de getClaim(claims, "nss") despues de validar el
        // token, sin fallback a parametros de request - si el claim viene vacio, se rechaza el
        // login (no hay degradacion posible, a diferencia de identifier/email mas abajo).
        String nss = jwt.getClaimAsString("nss");
        if (nss == null || nss.isBlank()) {
            log.warn("[sso] token valido pero sin claim 'nss' - se rechaza (sin fallback permitido)");
            return "redirect:/login?error";
        }
        nss = nss.trim();

        String identifier = resolveIdentifier(jwt, usuarioParam);
        if (identifier == null || identifier.isBlank()) {
            log.warn("[sso] token valido pero sin claim de identidad (email/preferred_username/upn) ni usuario de fallback");
            return "redirect:/login?error";
        }

        if (modulePermissionClient.findRoleId(nss).isEmpty()) {
            log.warn("[sso] token valido para nss={} pero sin rol asignado en la app de Salud (consulta_app_rol_usuario vacio) - se rechaza",
                    nss);
            return "redirect:/login?sinrol";
        }

        // El avatar de Intranet SI necesita el email (identifier), no el NSS - ver
        // PortalPrincipalResolver.resolveConEmailConocido.
        PortalUserPrincipal principal = principalResolver.resolveConEmailConocido(nss, identifier);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        log.info("[sso] OK nss={} email={} legacy_role={} autoridades={}",
                nss, identifier, jwt.getClaimAsString("legacy_role"), principal.getAuthorities());
        return "redirect:/home";
    }

    /**
     * Misma cadena de fallback que el ejemplo de referencia (resolveEmail): email,
     * preferred_username, upn del JWT, y solo si el token no trae ninguno, el parametro
     * "usuario" del request. Los primeros tres SI estan atados a la firma del token; el
     * ultimo no, es una degradacion deliberada para no romper el flujo si el IdP no
     * manda esos claims, tal como en el ejemplo.
     */
    private String resolveIdentifier(Jwt jwt, String usuarioParam) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername.trim();
        }
        String upn = jwt.getClaimAsString("upn");
        if (upn != null && !upn.isBlank()) {
            return upn.trim();
        }
        return usuarioParam == null ? null : usuarioParam.trim();
    }
}
