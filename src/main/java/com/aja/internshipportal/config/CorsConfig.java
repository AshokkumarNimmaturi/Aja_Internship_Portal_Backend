package com.aja.internshipportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

// Allows React (localhost:5173) to call our API
// Without this — browser blocks all API calls
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // React dev server origin
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:3000"
        ));

        // allowed HTTP methods
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // allow Authorization header — needed for JWT
        config.setAllowedHeaders(List.of("*"));

        // allow frontend to read response headers
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        // apply to all routes
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}