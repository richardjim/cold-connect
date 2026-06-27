package com.coldconnect.service;

import com.coldconnect.entity.User;
import com.coldconnect.enums.Role;
import com.coldconnect.exception.AppException;
import com.coldconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final SmsService smsService;

    public OtpService(UserRepository userRepository, SmsService smsService) {
        this.userRepository = userRepository;
        this.smsService = smsService;
    }

    @Transactional
    public String requestOtp(String phone, String purpose, String language) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User u = new User();
            u.setPhone(phone);
            u.setFullName("");
            u.setRole(Role.CUSTOMER);
            u.setEnabled(true);
            return u;
        });

        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        smsService.sendOtp(phone, otp, language != null ? language : "en");

        String masked = phone.length() > 4
                ? phone.substring(0, phone.length() - 4) + "****"
                : "****";
        return "OTP sent to " + masked;
    }

    @Transactional
    public User verifyOtp(String phone, String code) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.NotFoundException("Phone not registered"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(code)) {
            throw new AppException.BadRequestException("Invalid OTP code");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException("OTP has expired. Please request a new one.");
        }

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }
}
