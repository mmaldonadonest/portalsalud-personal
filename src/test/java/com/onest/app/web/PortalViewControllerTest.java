package com.onest.app.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.onest.app.audit.service.AuditoriaService;
import com.onest.app.catalog.module.client.LocalModulePermissionClient;
import com.onest.app.config.SecurityConfiguration;
import com.onest.app.security.permission.PermissionService;
import com.onest.app.security.legacy.LegacyPhpAuthenticationProvider;
import com.onest.app.security.service.PortalUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4: @WebMvcTest vive en spring-boot-webmvc-test y @MockBean se reemplaza por @MockitoBean de Spring
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalViewController.class)
@Import({SecurityConfiguration.class, ViewModelAdvice.class})
class PortalViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortalUserDetailsService portalUserDetailsService;

    // Requerido por SecurityConfiguration (selector de estrategia de auth, U08).
    @MockitoBean
    private LegacyPhpAuthenticationProvider legacyPhpAuthenticationProvider;

    // Requeridos por SecurityConfiguration (ClinicalAccessFilter), ViewModelAdvice y el
    // AuditoriaInterceptor (WebMvcConfigurer, lo levanta @WebMvcTest). Sin BD: todo mock.
    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private LocalModulePermissionClient localModulePermissionClient;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @Test
    void shouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Portal Salud Personal")));
    }

    @Test
    @WithMockUser
    void shouldRenderHomePage() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/dashboard-demo-apex"))
                .andExpect(content().string(containsString("Dashboard ligero de KPIs")));
    }
}
