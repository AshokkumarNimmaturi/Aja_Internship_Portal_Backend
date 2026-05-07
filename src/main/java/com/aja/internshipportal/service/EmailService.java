// PATH: src/main/java/com/aja/internshipportal/service/EmailService.java

package com.aja.internshipportal.service;

public interface EmailService {

    // Standard welcome for new subscribers
    void sendWelcomeEmail(String toEmail, String fullName);

    // ✅ Updated: Now includes packageName and tier for better receipt details
    void sendInvoiceEmail(String toEmail, String fullName, byte[] pdfInvoice, String orderId, String packageName, String tier);

    // Admin alerts
    void sendSupportAlertToAdmin(String fromEmail, String subject, String message);

    // Initial credentials for employees/tutors
    void sendCredentialsEmail(String toEmail, String fullName, String tempPassword, byte[] pdfAttachment);

    // Security & Auth
    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken);
    
    void sendPasswordChangedEmail(String email, String name);

    // ✅ Added: Notification specifically for when a subscription is ACTIVATED
    void sendSubscriptionConfirmationEmail(String toEmail, String fullName, String packageName, String tier, String endDate);
}
