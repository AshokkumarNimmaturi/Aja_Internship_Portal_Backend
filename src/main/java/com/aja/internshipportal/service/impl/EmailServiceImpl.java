package com.aja.internshipportal.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aja.internshipportal.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // reads from application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Send credentials email with PDF attachment ──
    // @Async means this runs in background thread
    // user creation API returns immediately without waiting for email
    @Override
    @Async
    public void sendCredentialsEmail(String toEmail, String fullName,
                                      String tempPassword,
                                      byte[] pdfAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // true = multipart (needed for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(
                message, true, "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Aja Internship Portal — Your Login Credentials");

            // HTML email body
            String body = buildCredentialsEmailBody(fullName, toEmail, tempPassword);
            helper.setText(body, true); // true = isHtml

            // attach PDF
            helper.addAttachment("credentials.pdf", 
                new org.springframework.core.io.ByteArrayResource(pdfAttachment)
            );

            mailSender.send(message);
            log.info("Credentials email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send credentials email to {}: {}",
                toEmail, e.getMessage());
        }
    }

    // ── Send password reset email ──
    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName,
                                        String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, true, "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Aja Internship Portal — Password Reset Request");

            String body = buildPasswordResetEmailBody(fullName, resetToken);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send reset email to {}: {}",
                toEmail, e.getMessage());
        }
    }

    // ── Send subscription confirmation email ──
    @Override
    @Async
    public void sendSubscriptionConfirmationEmail(String toEmail,
                                                   String fullName,
                                                   String packageName,
                                                   String tier,
                                                   String endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, true, "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Aja Internship Portal — Subscription Activated");

            String body = buildSubscriptionEmailBody(
                fullName, packageName, tier, endDate
            );
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Subscription email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send subscription email to {}: {}",
                toEmail, e.getMessage());
        }
    }

    // ── HTML email templates ──

    private String buildCredentialsEmailBody(String fullName,
                                              String email,
                                              String tempPassword) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: auto; padding: 20px;
                            border: 1px solid #ddd; border-radius: 8px;">
                    <h2 style="color: #2c3e50;">Welcome to Aja Internship Portal</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your account has been created. Here are your login credentials:</p>
                    <div style="background: #f4f4f4; padding: 15px;
                                border-radius: 6px; margin: 20px 0;">
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="color: #e74c3c;">
                        <strong>Important:</strong> You will be required to change
                        your password on first login.
                    </p>
                    <p>Please find your credentials PDF attached to this email.</p>
                    <br>
                    <p>Best regards,<br><strong>Aja Internship Portal Team</strong></p>
                </div>
            </body>
            </html>
            """.formatted(fullName, email, tempPassword);
    }

    private String buildPasswordResetEmailBody(String fullName,
                                                String resetToken) {
        // frontend reset password page URL
        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;

        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: auto; padding: 20px;
                            border: 1px solid #ddd; border-radius: 8px;">
                    <h2 style="color: #2c3e50;">Password Reset Request</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>We received a request to reset your password.</p>
                    <p>Click the button below to reset your password.
                       This link expires in <strong>15 minutes</strong>.</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background: #3498db; color: white;
                                  padding: 12px 30px; border-radius: 6px;
                                  text-decoration: none; font-size: 16px;">
                            Reset Password
                        </a>
                    </div>
                    <p>If you did not request this, ignore this email.</p>
                    <br>
                    <p>Best regards,<br><strong>Aja Internship Portal Team</strong></p>
                </div>
            </body>
            </html>
            """.formatted(fullName, resetLink);
    }

    private String buildSubscriptionEmailBody(String fullName,
                                               String packageName,
                                               String tier,
                                               String endDate) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: auto; padding: 20px;
                            border: 1px solid #ddd; border-radius: 8px;">
                    <h2 style="color: #27ae60;">Subscription Activated!</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your subscription has been activated successfully.</p>
                    <div style="background: #f4f4f4; padding: 15px;
                                border-radius: 6px; margin: 20px 0;">
                        <p><strong>Package:</strong> %s</p>
                        <p><strong>Tier:</strong> %s</p>
                        <p><strong>Valid Until:</strong> %s</p>
                    </div>
                    <p>You now have full access to all questions in your package.</p>
                    <br>
                    <p>Best regards,<br><strong>Aja Internship Portal Team</strong></p>
                </div>
            </body>
            </html>
            """.formatted(fullName, packageName, tier, endDate);
    }
    
    
    @Override
    public void sendPasswordChangedEmail(String email, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Password Changed Successfully");

            message.setText(
                "Hello " + name + ",\n\n" +
                "Your password has been changed successfully.\n\n" +
                "If you did not perform this action, please contact support immediately.\n\n" +
                "Regards,\nAja Internship Portal Team"
            );

            mailSender.send(message);

            log.info("Password change confirmation email sent to {}", email);

        } catch (Exception e) {
            log.error("Failed to send password changed email to {}: {}", email, e.getMessage());
        }
    }
}