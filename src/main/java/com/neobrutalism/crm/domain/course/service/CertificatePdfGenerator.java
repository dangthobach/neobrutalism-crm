package com.neobrutalism.crm.domain.course.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.neobrutalism.crm.domain.course.model.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating certificate PDFs using iText
 * Implements neobrutalism design with bold borders and vibrant colors
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificatePdfGenerator {

    @Value("${app.base-url:https://your-domain.com}")
    private String applicationBaseUrl;

    private static final DeviceRgb NEOBRUTALISM_YELLOW = new DeviceRgb(255, 220, 0);
    private static final DeviceRgb NEOBRUTALISM_BLACK = new DeviceRgb(0, 0, 0);
    private static final DeviceRgb NEOBRUTALISM_WHITE = new DeviceRgb(255, 255, 255);
    private static final float BORDER_WIDTH = 4f;

    /**
     * Generate certificate PDF as byte array
     * 
     * @param certificate The certificate to generate PDF for
     * @return PDF content as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generateCertificatePdf(Certificate certificate) throws IOException {
        log.info("Generating PDF for certificate: {}", certificate.getCertificateNumber());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate()); // Landscape orientation

            Document document = new Document(pdfDoc);
            document.setMargins(40, 40, 40, 40);

            // Load fonts (using standard fonts included in iText)
            PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            // Add neobrutalism border
            addNeobrutalismBorder(document);

            // Title
            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION")
                .setFont(boldFont)
                .setFontSize(32)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(60)
                .setMarginBottom(20)
                .setBold();
            document.add(title);

            // Decorative line
            Paragraph line = new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .setFontColor(NEOBRUTALISM_YELLOW);
            document.add(line);

            // Main text
            Paragraph mainText = new Paragraph()
                .setFont(regularFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            
            mainText.add("This is to certify that\n\n");
            
            Paragraph userName = new Paragraph(certificate.getUser().getFullName())
                .setFont(boldFont)
                .setFontSize(28)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setBold();
            document.add(mainText);
            document.add(userName);

            // Course completion text
            Paragraph courseText = new Paragraph()
                .setFont(regularFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            courseText.add("has successfully completed the course\n\n");
            document.add(courseText);

            // Course name (highlighted)
            Paragraph courseName = new Paragraph(certificate.getCourse().getTitle())
                .setFont(boldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .setBold()
                .setBackgroundColor(NEOBRUTALISM_YELLOW)
                .setPadding(10)
                .setBorder(new SolidBorder(NEOBRUTALISM_BLACK, BORDER_WIDTH));
            document.add(courseName);

            // Score
            if (certificate.getFinalScore() != null) {
                Paragraph score = new Paragraph(String.format("Final Score: %.1f%%", certificate.getFinalScore()))
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
                document.add(score);
            }

            // Completion date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            String completionDateStr = certificate.getCompletionDate() != null 
                ? certificate.getCompletionDate().format(dateFormatter)
                : certificate.getIssuedAt().format(dateFormatter);

            Paragraph dateText = new Paragraph("Completed on " + completionDateStr)
                .setFont(regularFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(40);
            document.add(dateText);

            // Certificate number and verification
            Paragraph certNumber = new Paragraph()
                .setFont(regularFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40);
            certNumber.add(new Text("Certificate Number: " + certificate.getCertificateNumber()).setBold());
            certNumber.add("\n");
            certNumber.add(new Text("Verify at: " + certificate.getVerificationUrl()).setFontColor(ColorConstants.BLUE));
            document.add(certNumber);

            document.close();

            log.info("PDF generated successfully for certificate: {}", certificate.getCertificateNumber());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF for certificate {}: {}", certificate.getCertificateNumber(), e.getMessage(), e);
            throw new IOException("Failed to generate certificate PDF", e);
        }
    }

    /**
     * Add neobrutalism-style border to document
     */
    private void addNeobrutalismBorder(Document document) {
        // Add thick black border around entire page
        document.setBorder(new SolidBorder(NEOBRUTALISM_BLACK, BORDER_WIDTH));
        document.setBackgroundColor(NEOBRUTALISM_WHITE);
    }

    /**
     * Generate verification URL for certificate
     * 
     * @param certificateNumber Certificate number
     * @return Verification URL
     */
    public String generateVerificationUrl(String certificateNumber) {
        return String.format("%s/verify-certificate/%s", applicationBaseUrl, certificateNumber);
    }
}
