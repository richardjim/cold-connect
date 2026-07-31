package com.coldconnect.thirdparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaystackService {

    private static final Logger log = LoggerFactory.getLogger(PaystackService.class);

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class InitializeResponse {
        public final boolean success;
        public final String  authorizationUrl;
        public final String  accessCode;
        public final String  reference;
        public final String  message;

        public InitializeResponse(boolean success, String authorizationUrl,
                                  String accessCode, String reference, String message) {
            this.success          = success;
            this.authorizationUrl = authorizationUrl;
            this.accessCode       = accessCode;
            this.reference        = reference;
            this.message          = message;
        }
    }

    public static class VerifyResponse {
        public final boolean   success;
        public final String    status;   // success, failed, abandoned
        public final String    reference;
        public final BigDecimal amount;  // in NGN (converted from kobo)
        public final String    channel;  // card, bank, ussd etc
        public final String    message;

        public VerifyResponse(boolean success, String status, String reference,
                              BigDecimal amount, String channel, String message) {
            this.success   = success;
            this.status    = status;
            this.reference = reference;
            this.amount    = amount;
            this.channel   = channel;
            this.message   = message;
        }
    }

    /**
     * Initialize a Paystack transaction.
     * Returns a checkout URL to redirect the customer to.
     */
    public InitializeResponse initializeTransaction(String email, BigDecimal amountNgn,
                                                    String reference, String callbackUrl,
                                                    Map<String, Object> metadata) {
        try {
            HttpHeaders headers = buildHeaders();

            // Paystack expects amount in kobo (NGN * 100)
            long amountKobo = amountNgn.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("email",        email);
            body.put("amount",       amountKobo);
            body.put("reference",    reference);
            body.put("currency",     "NGN");
            if (callbackUrl != null) body.put("callback_url", callbackUrl);
            if (metadata != null)    body.put("metadata", metadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/transaction/initialize", request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());

            if (json.get("status").asBoolean()) {
                JsonNode data = json.get("data");
                return new InitializeResponse(
                        true,
                        data.get("authorization_url").asText(),
                        data.get("access_code").asText(),
                        data.get("reference").asText(),
                        "Transaction initialized"
                );
            } else {
                return new InitializeResponse(false, null, null, null,
                        json.get("message").asText());
            }

        } catch (Exception e) {
            log.error("Paystack initialize error: {}", e.getMessage());
            return new InitializeResponse(false, null, null, null,
                    "Payment initialization failed: " + e.getMessage());
        }
    }

    /**
     * Verify a Paystack transaction by reference.
     * Call this after webhook or customer returns from checkout.
     */
    public VerifyResponse verifyTransaction(String reference) {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/transaction/verify/" + reference,
                    HttpMethod.GET, request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());

            if (json.get("status").asBoolean()) {
                JsonNode data    = json.get("data");
                String   status  = data.get("status").asText();
                long     kobo    = data.get("amount").asLong();
                BigDecimal amtNgn = BigDecimal.valueOf(kobo).divide(BigDecimal.valueOf(100));

                return new VerifyResponse(
                        true,
                        status,
                        data.get("reference").asText(),
                        amtNgn,
                        data.get("channel").asText(),
                        data.get("gateway_response").asText()
                );
            } else {
                return new VerifyResponse(false, "failed", reference,
                        BigDecimal.ZERO, null, json.get("message").asText());
            }

        } catch (Exception e) {
            log.error("Paystack verify error: {}", e.getMessage());
            return new VerifyResponse(false, "failed", reference,
                    BigDecimal.ZERO, null, "Verification failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(secretKey);
        return headers;
    }
}