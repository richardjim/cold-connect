package com.coldconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String link = baseUrl + "/api/auth/verify-email?token=" + token;
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;'>"
                + "<h2>Welcome to Cold Connect</h2>"
                + "<p>Please verify your email address to activate your account.</p>"
                + "<a href='" + link + "' style='display:inline-block;padding:12px 24px;"
                + "background:#4f46e5;color:#fff;border-radius:6px;text-decoration:none;'>"
                + "Verify Email</a>"
                + "<p style='color:#888;font-size:13px;margin-top:24px;'>Expires in 24 hours.</p>"
                + "</div>";
        send(toEmail, "Verify your Cold Connect account", html);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = baseUrl + "/api/auth/reset-password?token=" + token;
        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;'>"
                + "<h2>Reset your password</h2>"
                + "<p>Click below to set a new password. Expires in 60 minutes.</p>"
                + "<a href='" + link + "' style='display:inline-block;padding:12px 24px;"
                + "background:#dc2626;color:#fff;border-radius:6px;text-decoration:none;'>"
                + "Reset Password</a>"
                + "</div>";
        send(toEmail, "Reset your Cold Connect password", html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
