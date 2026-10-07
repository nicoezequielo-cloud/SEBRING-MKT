package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final SessionService sessions;

    public AuthFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        User user = token == null ? null : sessions.resolve(token).orElse(null);
        request.setAttribute(AuthSupport.ATTR_USER, user);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("GET".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        boolean publicMutation = path.startsWith("/api/auth")
                || (path.equals("/api/users") && "POST".equalsIgnoreCase(method));
        if (publicMutation) {
            chain.doFilter(request, response);
            return;
        }

        if (user == null) {
            writeJson(response, 401, "Unauthorized", "Authentication required");
            return;
        }

        if (path.startsWith("/api/vehicles") || path.startsWith("/api/images")) {
            if (!user.isAdmin()) {
                writeJson(response, 403, "Forbidden", "Only admins can manage vehicles and images");
                return;
            }
        } else if (path.startsWith("/api/users")) {
            boolean ownProfileUpdate = "PUT".equalsIgnoreCase(method) && path.matches("/api/users/\\d+");
            if (!ownProfileUpdate && !user.isAdmin()) {
                writeJson(response, 403, "Forbidden", "Only admins can manage users");
                return;
            }
        } else if (path.startsWith("/api/transactions")) {
            if (!user.isAdmin() && !"POST".equalsIgnoreCase(method)) {
                writeJson(response, 403, "Forbidden", "Only admins can update or delete transactions");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":" + status
                + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}