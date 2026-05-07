package com.aja.internshipportal.service.impl;

import com.itextpdf.text.Rectangle;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.service.PdfService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

	@Override
	public byte[] generateCredentialsPdf(User user, String tempPassword) {

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Document document = new Document(PageSize.A4);
			PdfWriter.getInstance(document, outputStream);

			document.open();

			// ── Fonts ──
			Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);

			Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

			Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.DARK_GRAY);

			Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);

			Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.RED);

			Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);

			// ── Title ──
			Paragraph title = new Paragraph("Aja Internship Portal", titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			title.setSpacingAfter(5);
			document.add(title);

			Paragraph subtitle = new Paragraph("Login Credentials",
					FontFactory.getFont(FontFactory.HELVETICA, 13, BaseColor.GRAY));
			subtitle.setAlignment(Element.ALIGN_CENTER);
			subtitle.setSpacingAfter(20);
			document.add(subtitle);

			// ── Table ──
			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(80);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setWidths(new float[] { 35f, 65f });
			table.setSpacingBefore(10);
			table.setSpacingAfter(20);

			// Header
			addTableHeader(table, "Field", headerFont);
			addTableHeader(table, "Value", headerFont);

			// Rows
			addTableRow(table, "Full Name", user.getFullName(), labelFont, valueFont);
			addTableRow(table, "Email", user.getEmail(), labelFont, valueFont);
			addTableRow(table, "Temporary Password", tempPassword, labelFont, valueFont);
			addTableRow(table, "Role", user.getRole().name(), labelFont, valueFont);
			addTableRow(table, "Generated On",
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), labelFont, valueFont);

			document.add(table);

			// ── Note ──
			Paragraph note = new Paragraph(
					"Important: You must change your password on first login. " + "Do not share these credentials.",
					noteFont);
			note.setAlignment(Element.ALIGN_CENTER);
			note.setSpacingAfter(15);
			document.add(note);

			// ── Footer ──
			Paragraph footer = new Paragraph("System generated document. Contact admin for support.", smallFont);
			footer.setAlignment(Element.ALIGN_CENTER);
			document.add(footer);

			document.close();

			log.info("PDF generated for user: {}", user.getEmail());
			return outputStream.toByteArray();

		} catch (Exception e) {
			log.error("PDF generation failed for user {}: {}", user.getEmail(), e.getMessage());
			return new byte[0];
		}
	}

	// ── Helper Methods ──

	private void addTableHeader(PdfPTable table, String text, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBackgroundColor(new BaseColor(44, 62, 80));
		cell.setPadding(8);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setBorder(Rectangle.NO_BORDER);
		table.addCell(cell);
	}

	private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {

		PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
		labelCell.setPadding(8);
		labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
		labelCell.setBorderColor(new BaseColor(200, 200, 200));

		PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
		valueCell.setPadding(8);
		valueCell.setBorderColor(new BaseColor(200, 200, 200));

		table.addCell(labelCell);
		table.addCell(valueCell);
	}
}