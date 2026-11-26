package com.diariofinanciero.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Aplica cabeceras HTTP de seguridad y lista blanca de métodos HTTP.
 *
 * TODO(security): Esta aplicación es de un solo usuario y corre exclusivamente en
 * localhost (server.address=127.0.0.1) sin login por diseño explícito. Si alguna vez
 * se expusiera fuera de localhost, se debe agregar Spring Security con autenticación,
 * cookies seguras, protección CSRF y rate limiting.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_PERMITIDOS = Set.of("GET", "POST", "HEAD", "OPTIONS");
    private static final String CSP_POLICY =
            "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self'; "
                    + "img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CSP_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");

        if (!METODOS_PERMITIDOS.contains(request.getMethod())) {
            response.setHeader("Allow", "GET, POST, HEAD, OPTIONS");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
