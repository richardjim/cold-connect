package com.coldconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${termii.api-key:}")
    private String termiiApiKey;

    @Value("${termii.sender-id:ColdConnect}")
    private String senderId;

    @Value("${sms.provider:console}")
    private String smsProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtp(String phone, String otp, String language) {
        String message = buildOtpMessage(otp, language);
        if ("termii".equals(smsProvider) && !termiiApiKey.isBlank()) {
            sendViaTermii(phone, message);
        } else {
            log.info("OTP for {} -> {} [lang: {}]", phone, otp, language);
        }
    }

    public void sendOtpVoiceCall(String phone, String otp) {
        if ("termii".equals(smsProvider) && !termiiApiKey.isBlank()) {
            sendVoiceCallViaTermii(phone);
        } else {
            log.info("Voice OTP call for {}", phone);
        }
    }

    public void sendSms(String phone, String message) {
        if ("termii".equals(smsProvider) && !termiiApiKey.isBlank()) {
            sendViaTermii(phone, message);
        } else {
            log.info("SMS to {}: {}", phone, message);
        }
    }

    private String buildOtpMessage(String otp, String language) {
        if (language == null) language = "en";
        return switch (language) {
            case "ha"  -> "Lambar sirri ku: " + otp + ". Yana da inganci na minti 10.";
            case "yo"  -> "Koodu rẹ ni: " + otp + ". O wulo fun iṣẹju 10.";
            case "ig"  -> "Koodu gị bụ: " + otp + ". Ọ dị maka nkeji 10.";
            case "pcm" -> "Your OTP be: " + otp + ". E go expire for 10 minutes.";
            default    -> "Your Cold Connect OTP is: " + otp + ". Valid for 10 minutes.";
        };
    }

    private void sendViaTermii(String phone, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "to", phone,
                    "from", senderId,
                    "sms", message,
                    "type", "plain",
                    "api_key", termiiApiKey,
                    "channel", "dnd"
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                    "https://api.ng.termii.com/api/sms/send", request, String.class);
            log.info("SMS sent to {} via Termii", phone);
        } catch (Exception ex) {
            log.error("Failed to send SMS to {}: {}", phone, ex.getMessage());
        }
    }

    private void sendVoiceCallViaTermii(String phone) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "api_key", termiiApiKey,
                    "phone_number", phone,
                    "pin_attempts", 3,
                    "pin_time_to_live", 10,
                    "pin_length", 6,
                    "pin_type", "NUMERIC"
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                    "https://api.ng.termii.com/api/sms/otp/send/voice", request, String.class);
            log.info("Voice OTP triggered for {}", phone);
        } catch (Exception ex) {
            log.error("Failed to send voice OTP to {}: {}", phone, ex.getMessage());
        }
    }
}