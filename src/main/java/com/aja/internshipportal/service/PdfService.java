package com.aja.internshipportal.service;

import com.aja.internshipportal.entity.User;

public interface PdfService {

    // Generates PDF with login credentials
    // returns PDF as byte array — passed to EmailService as attachment
    byte[] generateCredentialsPdf(User user, String tempPassword);
}