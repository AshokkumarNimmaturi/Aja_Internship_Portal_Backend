package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${fast2sms.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendPaymentSuccessSms(String phoneNumber, String fullName, String packageName, String amount) {
        String message = "Hello " + fullName + ", Payment of Rs." + amount + " for " + packageName + " was successful. Welcome to Aja Interview Vault!";
        sendSms(phoneNumber, message);
    }

    @Override
    public void sendWelcomeSms(String phoneNumber, String fullName) {
        String message = "Welcome to Aja Interview Vault, " + fullName + "! Your account is now active. Explore our elite interview question bank and secure your dream career.";
        sendSms(phoneNumber, message);
    }

    // ✅ HELPER: Generic method to send SMS via Fast2SMS
    private void sendSms(String phoneNumber, String message) {
        try {
            String url = "https://www.fast2sms.com/dev/bulkV2";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = "{\"route\": \"q\", \"message\": \"" + message + "\", \"language\": \"english\", \"numbers\": \"" + phoneNumber + "\"}";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to: {}", phoneNumber);
            } else {
                log.error("Failed to send SMS: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending SMS via Fast2SMS: {}", e.getMessage());
        }
    }
}
