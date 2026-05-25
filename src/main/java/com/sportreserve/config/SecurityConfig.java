package com.sportreserve.config;

import com.sportreserve.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/initiate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/notify").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/*/result").permitAll()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/admin/courts/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/admin/courts/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/admin/courts/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reservations").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reservations/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/reservations/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
