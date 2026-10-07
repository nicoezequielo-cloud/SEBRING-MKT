package org.example.security;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final UserRepository users;

    public SessionService(UserRepository users) {
        this.users = users;
    }

    public String create(User user) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, user.getId());
        return token;
    }

    public Optional<User> resolve(String token) {
        Integer userId = token == null ? null : tokens.get(token);
        return userId == null ? Optional.empty() : users.findById(userId);
    }

    public void invalidate(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}