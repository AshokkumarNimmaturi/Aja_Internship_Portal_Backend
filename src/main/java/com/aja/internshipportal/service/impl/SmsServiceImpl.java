package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * ✅ Helper method to ensure exactly 10 digits are sent to Fast2SMS.
     * Fast2SMS often fails if you include '+91' or spaces.
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        
        // Remove all non-numeric characters (like +, -, spaces)
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        
        // If it's a 12-digit number starting with 91 (India), remove the 91
        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2);
        }
        
        return cleaned;
    }

    // ✅ UPDATED: Generic method to send SMS via Fast2SMS with robust JSON handling
    private void sendSms(String phoneNumber, String message) {
        // Clean the number before sending
        String cleanedNumber = cleanPhoneNumber(phoneNumber);
        
        // 1. VALIDATION: Catch API Key configuration errors
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("${")) {
            log.error("SMS FAILED: Fast2SMS API Key is MISSING or misconfigured in application.properties.");
            return;
        }

        try {
            String url = "https://www.fast2sms.com/dev/bulkV2";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. SAFE PAYLOAD: Use a Map to let Spring handle JSON serialization/escaping
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("route", "q");
            requestBody.put("message", message);
            requestBody.put("language", "english");
            requestBody.put("numbers", cleanedNumber);

            log.info("DEBUG: Fast2SMS Request -> Numbers: {}, Message: {}", cleanedNumber, message);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to: {} (Original: {})", cleanedNumber, phoneNumber);
                log.info("DEBUG: Fast2SMS Response -> {}", response.getBody());
            } else {
                log.error("Failed to send SMS to {}. Response: {}", cleanedNumber, response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending SMS to {} via Fast2SMS: {}", cleanedNumber, e.getMessage());
        }
    }
}
