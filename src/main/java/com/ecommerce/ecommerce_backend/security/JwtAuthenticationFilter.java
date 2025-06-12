package com.ecommerce.ecommerce_backend.security;

import com.ecommerce.ecommerce_backend.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        System.out.println("=== JWT Filter Debug ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Authorization Header: " + (requestTokenHeader != null ? "Present" : "Missing"));
    
        String username = null;
        String jwtToken = null;
    
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            System.out.println("JWT Token extracted: " + jwtToken.substring(0, Math.min(50, jwtToken.length())) + "...");
            try {
                username = jwtUtil.extractUsername(jwtToken);
                System.out.println("Username extracted: " + username);
            } catch (Exception e) {
                System.out.println("JWT ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                logger.error("Unable to get JWT Token or JWT Token has expired", e);
            }
        } else {
            System.out.println("No Bearer token found");
        }
    
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userService.loadUserByUsername(username);
                System.out.println("User found: " + userDetails.getUsername());
                System.out.println("User authorities: " + userDetails.getAuthorities());
    
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("JWT Token is VALID");
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("Authentication set successfully");
                } else {
                    System.out.println("JWT Token is INVALID");
                }
            } catch (Exception e) {
                System.out.println("User loading error: " + e.getMessage());
                logger.error("Error loading user details", e);
            }
        }
        
        System.out.println("=== End JWT Filter Debug ===");
        filterChain.doFilter(request, response);
    }
}