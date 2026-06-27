package com.coldconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public void sendOtp(String phone, String otp, String language) {
        // TODO: integrate Termii (Nigerian SMS gateway) or Twilio
        // Logging OTP so you can test without SMS credits during development
        log.info("OTP for {} -> {} [lang: {}]", phone, otp, language);
    }

    public void sendSms(String phone, String message) {
        log.info("SMS to {}: {}", phone, message);
    }
}
