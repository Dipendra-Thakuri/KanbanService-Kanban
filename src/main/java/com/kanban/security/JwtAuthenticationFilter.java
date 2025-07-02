package com.kanban.security;

import com.kanban.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            // Debug logging
            System.out.println("=== JWT Filter Debug ===");
            System.out.println("Request URI: " + request.getRequestURI());
            System.out.println("Authorization Header: " + request.getHeader("Authorization"));
            System.out.println("Extracted JWT: " + (jwt != null ? jwt.substring(0, Math.min(50, jwt.length())) + "..." : "null"));

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                User user = tokenProvider.getUserFromToken(jwt);
                System.out.println("User from token: " + user.getUsername() + " | Role: " + user.getRole());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set successfully");
            } else {
                System.out.println("JWT validation failed or token is empty");
            }
        } catch (Exception ex) {
            System.err.println("JWT Filter Error: " + ex.getMessage());
            ex.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}