// PATH: src/main/java/com/aja/internshipportal/service/impl/EmailServiceImpl.java

package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Standard styling for all emails
    private final String HTML_START = "<html><body style='font-family: sans-serif; color: #0A1628; line-height: 1.6; background-color: #f8fafc; padding: 40px 0;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; padding: 40px; border: 1px solid #e2e8f0; border-radius: 30px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);'>" +
            "<div style='text-align: center; margin-bottom: 40px; border-bottom: 2px solid #f1f5f9; padding-bottom: 20px;'>" +
            "<h1 style='color: #2563EB; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;'>Aja Internship Portal</h1>" +
            "<p style='color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; tracking: 0.1em; margin-top: 5px;'>Premium Interview Excellence</p>" +
            "</div>";

    private final String HTML_END = "<hr style='border: 0; border-top: 1px solid #f1f5f9; margin: 40px 0;'>" +
            "<div style='text-align: center;'>" +
            "<p style='font-size: 12px; color: #94a3b8; line-height: 1.5;'>&copy; 2026 Aja Internship Portal. All rights reserved.<br>" +
            "Empowering future professionals through real-world insights.</p>" +
            "<div style='margin-top: 20px;'><span style='color: #cbd5e1;'>&bull;</span></div>" +
            "</div>" +
            "</div></body></html>";

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Aja Internship Portal!");

            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Hello, " + fullName + "! 👋</h2>" +
                    "<p>Welcome to our professional network. Your account is now fully active.</p>" +
                    "<p>Get ready to sharpen your skills with real-world interview questions from top companies. We are excited to be part of your career journey!</p>" +
                    "<div style='text-align: center; margin: 40px 0;'>" +
                    "<a href='http://localhost:5173/dashboard' style='background: #0A1628; color: #ffffff; padding: 14px 35px; text-decoration: none; border-radius: 15px; font-weight: 700; font-size: 15px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); transition: all 0.2s;'>Access Your Dashboard</a>" +
                    "</div>" +
                    HTML_END;

            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Welcome email failed", e);
        }
    }

    // ✅ UPDATED: Now accepts packageName and tier for detailed receipts
    @Override
    @Async
    public void sendInvoiceEmail(String toEmail, String fullName, byte[] pdfInvoice, String orderId, String packageName, String tier) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Purchase Receipt — Order #" + orderId);

            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Thank you for your purchase! 💎</h2>" +
                    "<p>Hello " + fullName + ", your payment was successful. We've attached your official tax invoice to this email for your records.</p>" +
                    "<div style='background: #f1f5f9; padding: 25px; border-radius: 20px; border: 1px solid #e2e8f0; margin: 30px 0;'>" +
                    "<p style='margin: 0; color: #64748b; font-size: 11px; font-weight: 800; text-transform: uppercase;'>Package Details</p>" +
                    "<h3 style='margin: 5px 0 0 0; color: #0A1628; font-size: 18px;'>" + packageName + "</h3>" +
                    "<p style='margin: 5px 0 0 0; color: #3b82f6; font-size: 13px; font-weight: 700;'>" + tier + " Access</p>" +
                    "<hr style='border: 0; border-top: 1px solid #cbd5e1; margin: 15px 0;'>" +
                    "<p style='margin: 0; color: #64748b; font-size: 11px; font-weight: 800; text-transform: uppercase;'>Order Reference</p>" +
                    "<p style='margin: 0; color: #0A1628; font-size: 14px;'>#" + orderId + "</p>" +
                    "</div>" +
                    "<p>Your premium content has been instantly unlocked. Return to the portal to start your preparation.</p>" +
                    "<div style='text-align: center; margin: 40px 0;'>" +
                    "<a href='http://localhost:5173/dashboard' style='background: #0A1628; color: #ffffff; padding: 14px 40px; text-decoration: none; border-radius: 15px; font-weight: 700;'>Access Your Library</a>" +
                    "</div>" +
                    HTML_END;

            helper.setText(body, true);
            helper.addAttachment("Aja_Invoice_" + orderId + ".pdf", new ByteArrayResource(pdfInvoice));
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Invoice email failed", e);
        }
    }

    @Override
    @Async
    public void sendSupportAlertToAdmin(String fromEmail, String subject, String message) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(this.fromEmail);
            msg.setTo(this.fromEmail);
            msg.setSubject("URGENT: New Support Request - " + subject);
            msg.setText("New Request from: " + fromEmail + "\n\nMessage:\n" + message);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Support alert failed", e);
        }
    }

    @Override @Async
    public void sendCredentialsEmail(String toEmail, String fullName, String tempPassword, byte[] pdfAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Aja Internship Portal — Your Account Credentials");

            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Welcome aboard, " + fullName + "! 👋</h2>" +
                    "<p>Your professional account has been successfully provisioned. We’ve attached a secure PDF containing your full onboarding details.</p>" +
                    "<div style='background: #eff6ff; padding: 30px; border-radius: 25px; border: 1px solid #dbeafe; margin: 35px 0;'>" +
                    "<p style='margin: 0 0 10px 0; color: #3b82f6; font-size: 11px; font-weight: 800; text-transform: uppercase;'>Access Credentials</p>" +
                    "<p style='margin: 5px 0; font-size: 14px;'><strong>Email:</strong> <span style='color: #0A1628;'>" + toEmail + "</span></p>" +
                    "<p style='margin: 5px 0; font-size: 14px;'><strong>Temporary Password:</strong></p>" +
                    "<div style='background: white; padding: 12px; border-radius: 10px; border: 1px solid #dbeafe; text-align: center; margin-top: 10px;'>" +
                    "<code style='color: #2563EB; font-family: monospace; font-size: 20px; font-weight: 800; letter-spacing: 2px;'>" + tempPassword + "</code>" +
                    "</div>" +
                    "</div>" +
                    "<p style='font-size: 14px; text-align: center; color: #64748b; font-style: italic;'>Note: You will be required to update this password during your first login for security purposes.</p>" +
                    "<div style='text-align: center; margin: 40px 0;'>" +
                    "<a href='http://localhost:5173/login' style='background: #0A1628; color: #ffffff; padding: 14px 40px; text-decoration: none; border-radius: 15px; font-weight: 700; font-size: 15px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);'>Login to Portal</a>" +
                    "</div>" +
                    HTML_END;

            helper.setText(body, true);
            if (pdfAttachment != null) {
                String safeName = fullName.replaceAll("[^a-zA-Z0-9]", "_");
                helper.addAttachment("Aja_Credentials_" + safeName + ".pdf", new ByteArrayResource(pdfAttachment));
            }
            mailSender.send(message);
        } catch (MessagingException e) { log.error("Failed to send credentials", e); }
    }

    @Override @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Aja Internship Portal — Password Reset Request");

            String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;
            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Reset your password? 🔒</h2>" +
                    "<p>Hello " + fullName + ", we received a request to reset the password for your account. No worries, it happens!</p>" +
                    "<div style='text-align: center; margin: 40px 0;'>" +
                    "<a href='" + resetLink + "' style='background: #2563EB; color: #ffffff; padding: 14px 40px; text-decoration: none; border-radius: 15px; font-weight: 700; font-size: 15px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(37,99,235,0.2);'>Reset My Password</a>" +
                    "</div>" +
                    "<p style='font-size: 13px; color: #64748b;'>If you didn't request this, you can safely ignore this email. This link will expire in 1 hour.</p>" +
                    HTML_END;

            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) { log.error("Reset email failed", e); }
    }

    @Override @Async
    public void sendSubscriptionConfirmationEmail(String toEmail, String fullName, String packageName, String tier, String endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Aja Internship Portal — Subscription Activated");

            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Subscription Activated! 🚀</h2>" +
                    "<p>Great news, " + fullName + "! Your premium subscription to the **" + packageName + "** is now live.</p>" +
                    "<div style='background: #f8fafc; padding: 30px; border-radius: 25px; border: 1px solid #e2e8f0; margin: 35px 0;'>" +
                    "<div style='display: flex; justify-content: space-between; margin-bottom: 10px;'>" +
                    "<span style='color: #64748b; font-size: 13px;'>Selected Plan:</span>" +
                    "<span style='color: #0A1628; font-weight: 700; font-size: 13px;'>" + tier + "</span>" +
                    "</div>" +
                    "<div style='display: flex; justify-content: space-between;'>" +
                    "<span style='color: #64748b; font-size: 13px;'>Active Until:</span>" +
                    "<span style='color: #2563EB; font-weight: 700; font-size: 13px;'>" + endDate + "</span>" +
                    "</div>" +
                    "</div>" +
                    "<p>Go ahead and explore your new questions library in the portal.</p>" +
                    "<div style='text-align: center; margin: 40px 0;'>" +
                    "<a href='http://localhost:5173/dashboard/questions' style='background: #0A1628; color: #ffffff; padding: 14px 40px; text-decoration: none; border-radius: 15px; font-weight: 700; font-size: 15px; display: inline-block;'>View Question Bank</a>" +
                    "</div>" +
                    HTML_END;

            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) { log.error("Sub confirmation failed", e); }
    }

    @Override
    public void sendPasswordChangedEmail(String email, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Security Alert: Password Changed");

            String body = HTML_START +
                    "<h2 style='font-weight: 700; color: #1e293b; margin-top: 0;'>Security Update 🛡️</h2>" +
                    "<p>Hello " + name + ", this is a confirmation that your portal password was recently changed.</p>" +
                    "<p>If you made this change, you can safely ignore this email. If you **did not** change your password, please contact our support team immediately.</p>" +
                    HTML_END;

            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) { log.error("Password change email failed", e); }
    }
}
