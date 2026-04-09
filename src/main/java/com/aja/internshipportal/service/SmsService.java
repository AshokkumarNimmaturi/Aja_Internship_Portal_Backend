package com.aja.internshipportal.service;

public interface SmsService {
    void sendPaymentSuccessSms(String phoneNumber, String fullName, String packageName, String amount);
    
    // ✅ ADDED: Send SMS on registration
    void sendWelcomeSms(String phoneNumber, String fullName);
}
