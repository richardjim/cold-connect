package com.coldconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String MAILJET_URL = "https://api.mailjet.com/v3.1/send";

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendVerificationEmail(String toEmail, String code) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;'>"
                + "<h2>Welcome to Cold Connect</h2>"
                + "<p>Use the code below to verify your email address. Expires in 30 minutes.</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:12px;"
                + "background:#f0f4ff;padding:24px;text-align:center;border-radius:8px;"
                + "color:#4f46e5;margin:24px 0;'>"
                + code
                + "</div>"
                + "<p style='color:#888;font-size:13px;'>If you did not register, ignore this email.</p>"
                + "</div>";
        send(toEmail, "Your Cold Connect verification code", html);
    }

    public void sendPasswordResetEmail(String toEmail, String code) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;'>"
                + "<h2>Reset your Cold Connect password</h2>"
                + "<p>Use the code below to reset your password. Expires in 60 minutes.</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:12px;"
                + "background:#fff5f5;padding:24px;text-align:center;border-radius:8px;"
                + "color:#dc2626;margin:24px 0;'>"
                + code
                + "</div>"
                + "<p style='color:#888;font-size:13px;'>If you did not request this, ignore this email.</p>"
                + "</div>";
        send(toEmail, "Your Cold Connect password reset code", html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((apiKey + ":" + secretKey).getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + credentials);

            String body = "{"
                    + "\"Messages\": [{"
                    + "\"From\": {\"Email\": \"" + fromAddress + "\", \"Name\": \"" + fromName + "\"},"
                    + "\"To\": [{\"Email\": \"" + to + "\"}],"
                    + "\"Subject\": \"" + subject + "\","
                    + "\"HTMLPart\": \"" + htmlBody.replace("\"", "\\\"") + "\""
                    + "}]"
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(MAILJET_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} via Mailjet", to);
            } else {
                log.error("Mailjet error {}: {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}