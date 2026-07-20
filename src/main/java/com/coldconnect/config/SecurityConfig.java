package com.coldconnect.config;

import com.coldconnect.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter      jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter      = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**",
            "/api-docs", "/api-docs/**", "/v3/api-docs/**"
    };

    private static final String[] PUBLIC_PATHS = {
            // Customer auth
            "/v1/auth/signup",
            "/v1/auth/login",
            "/v1/auth/otp/verify",
            "/v1/auth/otp/request",
            "/v1/auth/otp/call",
            // Admin auth
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify-email",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh",
            "/api/auth/resend-verification",
            // Public data
            "/v1/hubs",
            "/v1/hubs/**",
            "/v1/regions/**",
            "/v1/commodities/**",
            // Website public
            "/v1/leads/**",
            "/v1/public/**",
            "/v1/newsletter/**",
            "/web/pages/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Swagger
                        .requestMatchers(SWAGGER_PATHS).permitAll()

                        // Public endpoints
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Operator + Admin
                        .requestMatchers("/api/operator/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/operator/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Driver + above
                        .requestMatchers("/v1/driver/**").hasAnyRole("ADMIN", "OPERATOR", "DRIVER")

                        // All authenticated /v1/**
                        .requestMatchers("/v1/**").authenticated()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}