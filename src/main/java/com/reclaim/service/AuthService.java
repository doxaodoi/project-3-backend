package com.reclaim.service;

import com.reclaim.config.JwtUtil;
import com.reclaim.dto.request.LoginRequest;
import com.reclaim.dto.request.RegisterRequest;
import com.reclaim.dto.response.AuthResponse;
import com.reclaim.dto.response.UserResponse;
import com.reclaim.entity.User;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw ApiException.conflict("Email already registered");
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .passwordHash(encoder.encode(req.getPassword()))
            .role("USER")
            .build();
        userRepo.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw ApiException.badRequest("Invalid refresh token");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepo.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String access = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refresh = jwtUtil.generateRefreshToken(user.getId());
        return AuthResponse.builder()
            .accessToken(access)
            .refreshToken(refresh)
            .user(UserResponse.from(user))
            .build();
    }
}
