package com.coldconnect.service;

import com.coldconnect.entity.User;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final SmsService     smsService;
    private final AppMessages    messages;

    public OtpService(UserRepository userRepository,
                      SmsService smsService,
                      AppMessages messages) {
        this.userRepository = userRepository;
        this.smsService     = smsService;
        this.messages       = messages;
    }

    @Transactional
    public String requestOtp(String phone, String purpose, String language) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, language)));

        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        smsService.sendOtp(phone, otp, language != null ? language : "en");

        String masked = phone.length() > 4
                ? phone.substring(0, phone.length() - 4) + "****"
                : "****";

        return messages.get(AppMessages.Key.OTP_SENT, language) + " (" + masked + ")";
    }

    @Transactional
    public User verifyOtp(String phone, String code, String language) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException.NotFoundException(
                        messages.get(AppMessages.Key.PHONE_NOT_REGISTERED, language)));

        // Master test OTP — remove when Termii is wired
        boolean isMasterCode = "1234".equals(code);

        if (!isMasterCode) {
            if (user.getOtpCode() == null || !user.getOtpCode().equals(code)) {
                throw new AppException.BadRequestException(
                        messages.get(AppMessages.Key.OTP_INVALID, language));
            }
            if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
                throw new AppException.BadRequestException(
                        messages.get(AppMessages.Key.OTP_EXPIRED, language));
            }
        }

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }
}