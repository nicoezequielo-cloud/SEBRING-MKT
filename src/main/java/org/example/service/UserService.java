package org.example.service;

import org.example.dto.LoginRequest;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserUpdateRequest;
import org.example.exception.EmailAlreadyExistsException;
import org.example.exception.InvalidCredentialsException;
import org.example.exception.UserNotFoundException;
import org.example.exception.UsernameAlreadyExistsException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setNationalId(request.getNationalId());
        user.setAdmin(false);
        user.setActive(true);
        user.setRegistrationDate(new Timestamp(System.currentTimeMillis()));

        return userRepository.save(user);
    }

    public User updateUser(Integer id, UserUpdateRequest request, boolean callerIsAdmin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setNationalId(request.getNationalId());
        // Only an admin can change the role flag; a normal user editing its own
        // profile must not be able to grant itself admin.
        if (callerIsAdmin && request.getAdmin() != null) {
            user.setAdmin(request.getAdmin());
        }

        return userRepository.save(user);
    }

    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
        userRepository.save(user);
    }

    public User login(LoginRequest request) {
        if (request.getPassword() == null) {
            throw new InvalidCredentialsException("Invalid email/username or password");
        }
        User user = null;
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail()).orElse(null);
        } else if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user = userRepository.findByUsername(request.getUsername()).orElse(null);
        }
        if (user == null) {
            throw new InvalidCredentialsException("Invalid email/username or password");
        }
        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid email/username or password");
        }
        return user;
    }
}
