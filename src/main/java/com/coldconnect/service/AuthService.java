package com.coldconnect.service;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final EmailService emailService;

    @Value("${app.password-reset-expiry-minutes}")
    private int resetExpiryMinutes;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authManager,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.emailService = emailService;
    }

    @Transactional
    public String register(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException.ConflictException("Email is already registered");
        }
        // 6-digit code instead of UUID token
        String code = String.format("%06d", new Random().nextInt(999999));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(com.coldconnect.enums.Role.ADMIN);
        user.setEmailVerified(false);
        user.setVerificationToken(code);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(30));
        user.setEnabled(true);
        userRepository.save(user);
        emailService.sendVerificationEmail(email, code);
        return "Registration successful. Check your email for a 6-digit verification code.";
    }

    @Transactional
    public String verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.NotFoundException("Email not found"));

        if (user.isEmailVerified()) {
            return "Email already verified. You can log in.";
        }
        if (!code.equals(user.getVerificationToken())) {
            throw new AppException.BadRequestException("Invalid verification code");
        }
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException("Code has expired. Please register again.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return "Email verified successfully. You can now log in.";
    }

    @Transactional
    public User login(String email, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            throw new AppException.UnauthorizedException("Please verify your email before logging in.");
        }
        String refreshToken = jwtUtil.generateRefreshToken(user);
        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        return userRepository.save(user);
    }

    @Transactional
    public User refreshToken(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.UnauthorizedException("Invalid token"));
        boolean valid = user.getRefreshToken() != null
                && passwordEncoder.matches(refreshToken, user.getRefreshToken())
                && user.getRefreshTokenExpiry().isAfter(LocalDateTime.now());
        if (!valid) {
            throw new AppException.UnauthorizedException("Refresh token invalid or expired.");
        }
        String newRefresh = jwtUtil.generateRefreshToken(user);
        user.setRefreshToken(passwordEncoder.encode(newRefresh));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        return userRepository.save(user);
    }

    @Transactional
    public String forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String code = String.format("%06d", new Random().nextInt(999999));
            user.setPasswordResetToken(code);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(resetExpiryMinutes));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, code);
        });
        return "If that email is registered, a reset code has been sent.";
    }

    @Transactional
    public String resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.NotFoundException("Email not found"));

        if (user.getPasswordResetToken() == null || !user.getPasswordResetToken().equals(code)) {
            throw new AppException.BadRequestException("Invalid reset code");
        }
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException("Reset code has expired. Request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
        return "Password reset successful. Please log in.";
    }

    @Transactional
    public String logout(String username) {
        userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .ifPresent(user -> {
                    user.setRefreshToken(null);
                    user.setRefreshTokenExpiry(null);
                    userRepository.save(user);
                });
        return "Logged out successfully.";
    }

    @Transactional
    public String resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.NotFoundException("Email not found"));

        if (user.isEmailVerified()) {
            throw new AppException.BadRequestException("Email is already verified");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationToken(code);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        emailService.sendVerificationEmail(email, code);
        return "Verification code resent. Check your email.";
    }
}
