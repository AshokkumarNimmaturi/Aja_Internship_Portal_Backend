package com.aja.internshipportal.service;

public interface EmailService {

    // Send credentials email to newly created internal user
    // with PDF attachment containing login details
    void sendCredentialsEmail(String toEmail, String fullName,
                               String tempPassword, byte[] pdfAttachment);

    // Send password reset link email
    void sendPasswordResetEmail(String toEmail, String fullName,
                                 String resetToken);

    // Send subscription confirmation email
    void sendSubscriptionConfirmationEmail(String toEmail, String fullName,
                                            String packageName, String tier,
                                            String endDate);
    
    void sendPasswordChangedEmail(String email, String name);
}