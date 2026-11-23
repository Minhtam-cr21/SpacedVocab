package com.spacedvocab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Origins allowed to call the API from a browser ({@code app.cors.allowed-origins}), e.g. the Vite dev server. */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
