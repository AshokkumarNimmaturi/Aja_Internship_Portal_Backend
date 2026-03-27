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
@EnableMethodSecurity  // allows @PreAuthorize on controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ── routes that do NOT need a token ──
    private static final String[] PUBLIC_URLS = {
        "/api/auth/**",          // login, register, forgot password
        "/api/packages",         // anyone can browse packages
        "/api/packages/**",
        "/api/questions/samples",// free sample questions
        // ✅ FIXED SWAGGER PATHS   
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // disable CSRF — not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // configure route permissions
            .authorizeHttpRequests(auth -> auth

                // public routes — no token needed
                .requestMatchers(PUBLIC_URLS).permitAll()

                // admin only routes
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // tutor + admin can review questions
                .requestMatchers("/api/questions/*/review")
                    .hasAnyRole("TUTOR", "ADMIN")

                // employee + tutor + admin can submit questions
                .requestMatchers("/api/questions")
                    .hasAnyRole("EMPLOYEE", "TUTOR", "ADMIN")

                // payment + subscription — subscribers only
                .requestMatchers("/api/payment/**")
                    .hasRole("SUBSCRIBER")
                .requestMatchers("/api/subscriptions/**")
                    .hasRole("SUBSCRIBER")

                // everything else needs at least a valid token
                .anyRequest().authenticated()
            )

            // stateless — no sessions, every request must carry token
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // plug in our custom authentication provider
            .authenticationProvider(authenticationProvider())

            // run JwtAuthFilter before Spring's default login filter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // ── tells Spring how to verify passwords ──
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // use our UserDetailsServiceImpl to load users
        provider.setUserDetailsService(userDetailsService);
        // use BCrypt to verify passwords
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── BCrypt — hashes passwords before saving to DB ──
    // never store plain text passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── AuthenticationManager — used in AuthService to verify login ──
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}