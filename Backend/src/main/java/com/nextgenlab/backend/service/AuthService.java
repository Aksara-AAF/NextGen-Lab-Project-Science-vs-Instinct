package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.AuthResponse;
import com.nextgenlab.backend.model.dto.LoginRequest;
import com.nextgenlab.backend.model.dto.RegisterRequest;
import com.nextgenlab.backend.model.dto.UserDTO;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.UserRepository;
import com.nextgenlab.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (req.getEmail() == null || req.getUsername() == null || req.getPassword() == null
                || req.getEmail().isBlank() || req.getUsername().isBlank() || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("Field tidak valid (password minimal 6 karakter)");
        }
        if (userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("Email sudah terdaftar");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Username sudah dipakai");

        User u = new User();
        u.setEmail(req.getEmail());
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        User saved = userRepository.save(u);

        return tokenize(saved);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email atau password salah"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Email atau password salah");
        }
        return tokenize(u);
    }

    public UserDTO getById(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        return new UserDTO(u.getId(), u.getEmail(), u.getUsername());
    }

    private AuthResponse tokenize(User u) {
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getEmail());
        return new AuthResponse(token, new UserDTO(u.getId(), u.getEmail(), u.getUsername()));
    }
}
