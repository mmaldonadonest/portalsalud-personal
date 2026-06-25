package com.onest.app.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.onest.app.config.SecurityConfiguration;
import com.onest.app.security.service.PortalUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalViewController.class)
@Import({SecurityConfiguration.class, ViewModelAdvice.class})
class PortalViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalUserDetailsService portalUserDetailsService;

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
                .andExpect(view().name("pages/home"))
                .andExpect(content().string(containsString("Website Analytics")));
    }
}
