// PATH: src/main/java/com/aja/internshipportal/service/impl/InvoiceServiceImpl.java

package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.service.InvoiceService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    @Override
    public byte[] generateInvoicePdf(Payment payment) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. HEADER & BRANDING
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new BaseColor(10, 22, 40));
            Paragraph brand = new Paragraph("AJA INTERNSHIP PORTAL", brandFont);
            brand.setAlignment(Element.ALIGN_RIGHT);
            document.add(brand);

            Paragraph subHeader = new Paragraph("EXCELLENCE IN CAREER TRACKING", FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
            subHeader.setAlignment(Element.ALIGN_RIGHT);
            document.add(subHeader);
            document.add(new Paragraph("\n"));

            // 2. INVOICE TITLE
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            document.add(new Paragraph("TAX INVOICE / RECEIPT", titleFont));
            document.add(new Paragraph("Order ID: " + payment.getRazorpayOrderId(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            document.add(new Paragraph("Date: " + (payment.getCreatedAt() != null ? payment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "N/A"), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            document.add(new Paragraph("\n"));

            // 3. BILL TO SECTION
            document.add(new Paragraph("BILL TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            document.add(new Paragraph(payment.getUser().getFullName(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            document.add(new Paragraph(payment.getUser().getEmail(), FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY)));
            document.add(new Paragraph("\n\n"));

            // 4. ITEM TABLE
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Table Headers
            Stream.of("Description", "Tier", "Total Price")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(new BaseColor(240, 240, 240));
                    header.setBorderWidth(1);
                    header.setPadding(8);
                    header.setPhrase(new Phrase(columnTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                    table.addCell(header);
                });

            // Table Data
            table.addCell(new Phrase(payment.getAPackage() != null ? payment.getAPackage().getName() : "General Package", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            table.addCell(new Phrase(payment.getTier().name(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            table.addCell(new Phrase("INR " + payment.getAmount(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

            document.add(table);

            // 5. FOOTER
            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("This is a computer-generated document. No signature required.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            log.error("Critical error generating Invoice PDF for Order: {}", payment.getRazorpayOrderId(), e);
        }

        return out.toByteArray();
    }
}
