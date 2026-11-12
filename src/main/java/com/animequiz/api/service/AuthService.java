package com.animequiz.api.service;

import com.animequiz.api.dto.*;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.nio.charset.StandardCharsets;
import com.animequiz.api.exception.BadRequestException;
import com.animequiz.api.exception.ResourceNotFoundException;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, UserProfileRepository profiles, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users; this.profiles = profiles; this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("A senha deve possuir no máximo 72 bytes em UTF-8 para compatibilidade com BCrypt");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("Já existe um usuário cadastrado com este e-mail");
        if (users.existsByUsernameIgnoreCase(username)) throw new ConflictException("Nome de usuário já em uso");

        User user = new User(username, email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole("USER");
        User saved = users.save(user);
        profiles.save(new UserProfile(saved, null, null));
        return createAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas"));
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
        return toUserResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationSeconds(), toUserResponse(user));
    }
    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDateOfBirth());
    }
}
