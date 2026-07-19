package com.kkalchake.enlightenment.filter;

import com.kkalchake.enlightenment.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    // Sole producer of the "username" MDC key; the Logback pattern's
                    // %X{username:-anonymous} token reads it back per log line on this thread.
                    MDC.put("username", username);
                }
            } catch (Exception e) {
                // Continue unauthenticated rather than failing the request; WARN keeps
                // failed auth attempts visible in the logs.
                log.warn("JWT validation failed for {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Servlet container threads are pooled and reused across unrelated requests;
            // without clearing, a later request on the same thread could inherit this
            // request's username in its log lines.
            MDC.clear();
        }
    }
}
