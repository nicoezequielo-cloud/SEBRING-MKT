package org.example.security;

import jakarta.servlet.http.HttpServletRequest;
import org.example.exception.ForbiddenException;
import org.example.exception.UnauthorizedException;
import org.example.model.User;

import java.util.Optional;

public final class AuthSupport {

    public static final String ATTR_USER = "authUser";

    private AuthSupport() {
    }

    public static Optional<User> user(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USER);
        return value instanceof User user ? Optional.of(user) : Optional.empty();
    }

    public static User requireUser(HttpServletRequest request) {
        return user(request).orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    public static User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!user.isAdmin()) {
            throw new ForbiddenException("Only admins can access this resource");
        }
        return user;
    }
}