// 3. File: src/main/java/com/aja/internshipportal/service/InvoiceService.java

package com.aja.internshipportal.service;

import com.aja.internshipportal.entity.Payment;

public interface InvoiceService {
    byte[] generateInvoicePdf(Payment payment);
}
