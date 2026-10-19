package com.onest.app.ayuda;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita portal.ayuda.* (AyudaProperties). */
@Configuration
@EnableConfigurationProperties(AyudaProperties.class)
public class AyudaConfig {
}
