package com.aja.internshipportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ── PUBLIC ROUTES ──
    private static final String[] PUBLIC_URLS = {
        "/api/auth/**",
        "/api/packages",
        "/api/packages/**",
        "/api/questions/samples",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // 🔥 VERY IMPORTANT: ENABLE CORS
            .cors(cors -> {})

            // disable CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // authorization rules
            .authorizeHttpRequests(auth -> auth

                // allow preflight requests (IMPORTANT FOR CORS)
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                // public
                .requestMatchers(PUBLIC_URLS).permitAll()

                // admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // tutor/admin
                .requestMatchers("/api/questions/*/review")
                    .hasAnyRole("TUTOR", "ADMIN")

                // employee/tutor/admin
                .requestMatchers("/api/questions")
                    .hasAnyRole("EMPLOYEE", "TUTOR", "ADMIN")

                // subscriber
                .requestMatchers("/api/payment/**").hasRole("SUBSCRIBER")
                .requestMatchers("/api/subscriptions/**").hasRole("SUBSCRIBER")

                // all others
                .anyRequest().authenticated()
            )

            // stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // auth provider
            .authenticationProvider(authenticationProvider())

            // JWT filter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // ── AUTH PROVIDER ──
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── PASSWORD ENCODER ──
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── AUTH MANAGER ──
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}