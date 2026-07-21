package com.onest.app.config;

import com.onest.app.security.legacy.LegacyPhpAuthenticationProvider;
import com.onest.app.security.service.PortalUserDetailsService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/theme/**", "/css/**", "/js/**", "/img/**", "/login", "/error", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .headers(headers -> headers
                        // X-XSS-Protection desactivado (header obsoleto; valor 0 es el correcto moderno)
                        .xssProtection(xss -> xss.disable())
                        // X-Frame-Options: SAMEORIGIN (permite embeber en mismo origen si fuera necesario)
                        .frameOptions(frame -> frame.sameOrigin())
                        // CSP: frame-ancestors complementa y reemplaza progresivamente X-Frame-Options
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self'"))
                        // Referrer-Policy: no enviar información del referrer a terceros
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Permissions-Policy: deshabilitar APIs sensibles del navegador
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "geolocation=(), microphone=(), camera=()")));

        return http.build();
    }

    /**
     * Handler de login exitoso: loguea usuario + autoridades (el rol delata el
     * proveedor: ROLE_ADMIN = BD local, ROLE_USER = login ORDS legacy) y redirige
     * siempre a /home (equivale al viejo defaultSuccessUrl("/home", true)).
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler delegate = new SimpleUrlAuthenticationSuccessHandler("/home");
        delegate.setAlwaysUseDefaultTargetUrl(true);
        return (request, response, authentication) -> {
            String principal = authentication.getPrincipal() == null
                    ? "null" : authentication.getPrincipal().getClass().getSimpleName();
            log.info("[login] OK usuario={} autoridades={} principal={}",
                    authentication.getName(), authentication.getAuthorities(), principal);
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    /**
     * Handler de login fallido: loguea usuario y causa (util para depurar el login
     * ORDS) y redirige a /login?error.
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        SimpleUrlAuthenticationFailureHandler delegate = new SimpleUrlAuthenticationFailureHandler("/login?error");
        return (request, response, exception) -> {
            String usuario = request.getParameter("username");
            log.warn("[login] FALLIDO usuario={} causa={}", usuario, exception.getMessage());
            delegate.onAuthenticationFailure(request, response, exception);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(
            PortalUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Selector de estrategia de autenticacion (U08): JAVA | LEGACY_PHP | HYBRID.
     * - JAVA (default): solo BD local (DaoAuthenticationProvider).
     * - LEGACY_PHP: solo login ORDS legacy.
     * - HYBRID: intenta BD local y, si falla, login legacy.
     * Cambiable por propiedad `portal.auth.strategy` sin tocar codigo.
     */
    @Bean
    AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider,
            LegacyPhpAuthenticationProvider legacyPhpAuthenticationProvider,
            @Value("${portal.auth.strategy:JAVA}") String strategy) {

        String normalized = strategy == null ? "JAVA" : strategy.trim().toUpperCase();
        List<AuthenticationProvider> providers = switch (normalized) {
            case "LEGACY_PHP" -> List.of(legacyPhpAuthenticationProvider);
            case "HYBRID" -> List.of(daoAuthenticationProvider, legacyPhpAuthenticationProvider);
            default -> List.of(daoAuthenticationProvider);
        };
        log.info("Estrategia de autenticacion: {} ({} provider/s)", normalized, providers.size());
        return new ProviderManager(providers);
    }
}
