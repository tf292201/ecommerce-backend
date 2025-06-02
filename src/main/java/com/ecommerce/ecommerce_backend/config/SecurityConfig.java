package com.ecommerce.ecommerce_backend.config;

import com.ecommerce.ecommerce_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Remove @Lazy - we'll inject the dependencies directly
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
    // Public endpoints - no authentication required
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
    
    // MOVE USER ENDPOINTS TO THE TOP - Most specific rules first
    .requestMatchers("/api/users/me").hasAnyRole("USER", "CUSTOMER", "ADMIN")
    .requestMatchers("/api/users/me/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")
    .requestMatchers("/api/cart/me/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")
    .requestMatchers("/api/orders/me/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("USER", "CUSTOMER", "ADMIN")
    
    // Admin only endpoints - AFTER the user endpoints
    .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
    
    // All other endpoints require authentication
    .anyRequest().authenticated()
);

        // Add JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Remove the constructor and field - let Spring inject directly into the method above

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, 
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}