package com.configserverllp.csllp_learning_platform.certification_service.util;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class PdfGenerator {

    // Colors for certificate design
    private static final Color PRIMARY_COLOR = new DeviceRgb(0, 0, 0);       // Black
    private static final Color SECONDARY_COLOR = new DeviceRgb(51, 51, 51);  // Dark Gray
    private static final Color ACCENT_COLOR = new DeviceRgb(25, 135, 84);    // Success Green

    // Configuration keys
    private static final String EMPLOYEE_NAME = "employeeName";
    private static final String COURSE_NAME = "courseName";
    private static final String ISSUE_DATE = "issueDate";
    private static final String VERIFICATION_CODE = "verificationCode";
    private static final String COMPANY_NAME = "companyName";
    private static final String PLATFORM_NAME = "platformName";
    private static final String SIGNATORY_NAME = "signatoryName";
    private static final String SIGNATORY_TITLE = "signatoryTitle";
    private static final String SIGNATORY_COMPANY = "signatoryCompany";

    public void generateCertificatePdf(Map<String, Object> data, String outputPath) throws IOException {
        try {
            Path outputFilePath = Paths.get(outputPath);
            Path parentDir = outputFilePath.getParent();

            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("Created PDF directory: {}", parentDir.toAbsolutePath());
            }

            log.info("🔍 PDF Data received: {}", data);
            log.info("📄 Generating PDF certificate at: {}", outputFilePath.toAbsolutePath());

            try (PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument, PageSize.A4)) {

                // Set document margins to match template spacing
                document.setMargins(40, 40, 40, 40);

                // Add all certificate sections (ONLY LOGO, NO TEXT HEADER)
                addLogoOnlyHeader(document, data);
                addCertificateTitle(document);
                addRecipientSection(document, data);
                addCompletionDetails(document, data);
                addCertificateNumberAndIssueDate(document, data);
                addCertificateFooter(document, data);

                log.info("✅ PDF certificate generated successfully: {}", outputPath);

            }
        } catch (Exception e) {
            log.error("❌ Error generating PDF certificate at path: {}", outputPath, e);
            throw new IOException("Failed to generate PDF certificate at: " + outputPath, e);
        }
    }

    private void addLogoOnlyHeader(Document document, Map<String, Object> data) {
        try {
            // Create a table with 2 columns - EMPTY LEFT CELL, LOGO IN RIGHT
            float[] columnWidths = {3, 1}; // 75% empty, 25% for logo
            Table headerTable = new Table(UnitValue.createPercentArray(columnWidths));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setMarginBottom(20f);

            // Left cell - EMPTY (no platform name text)
            Cell emptyCell = new Cell();
            emptyCell.setBorder(null);
            emptyCell.setPadding(0);

            // Right cell - Logo ONLY
            ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
            if (logoResource.exists()) {
                ImageData imageData = ImageDataFactory.create(logoResource.getURL());
                Image logo = new Image(imageData);

                // Logo size
                logo.setWidth(100);
                logo.setHeight(100);
                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);

                Cell logoCell = new Cell();
                logoCell.add(logo);
                logoCell.setBorder(null);
                logoCell.setTextAlignment(TextAlignment.RIGHT);
                logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
                logoCell.setPadding(0);

                headerTable.addCell(emptyCell);
                headerTable.addCell(logoCell);
                document.add(headerTable);
                log.info("✅ Company logo added to header (NO PLATFORM TEXT)");
            } else {
                // If no logo, just add empty space
                document.add(new Paragraph(" ").setMarginBottom(20f));
                log.warn("⚠️ Logo not found at: static/images/logo.png");
            }

        } catch (Exception e) {
            log.warn("⚠️ Error adding logo header, using fallback: {}", e.getMessage());
            // Fallback: Add empty space
            document.add(new Paragraph(" ").setMarginBottom(20f));
        }
    }

    private void addCertificateTitle(Document document) {
        // Main certificate title
        Paragraph certificateTitle = new Paragraph("CERTIFICATE")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(5f);
        document.add(certificateTitle);

        // "OF COMPLETION" text
        Paragraph ofCompletion = new Paragraph("OF COMPLETION")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(10f);
        document.add(ofCompletion);

        // Presentation text
        Paragraph presentedText = new Paragraph("THIS CERTIFICATE IS PROUDLY PRESENTED TO")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(15f);
        document.add(presentedText);

        log.info("✅ Certificate title section added");
    }

    private void addRecipientSection(Document document, Map<String, Object> data) {
        String employeeName = String.valueOf(data.getOrDefault(EMPLOYEE_NAME, "Employee Name"));

        log.info("👤 Adding recipient name to PDF: {}", employeeName);

        Paragraph nameParagraph = new Paragraph(employeeName.toUpperCase())
                .setFontSize(36) // Increased font size
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(20f);
        document.add(nameParagraph);

        log.info("✅ Recipient section added for employee: {}", employeeName);
    }

    private void addCompletionDetails(Document document, Map<String, Object> data) {
        String courseName = String.valueOf(data.getOrDefault(COURSE_NAME, "Course Name"));
        String issueDate = String.valueOf(data.getOrDefault(ISSUE_DATE, ""));
        String trainingPeriod = formatTrainingPeriod(issueDate);

        String completionText = "In acknowledgment of the successful completion of the " +
                courseName + " through the company's professional learning and development platform, " +
                trainingPeriod + ".";

        log.info("📝 Generating completion text for course: {} and period: {}", courseName, trainingPeriod);

        Paragraph completionParagraph = new Paragraph(completionText)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(30f);
        document.add(completionParagraph);

        log.info("✅ Completion details added");
    }

    private void addCertificateNumberAndIssueDate(Document document, Map<String, Object> data) {
        String verificationCode = String.valueOf(data.getOrDefault(VERIFICATION_CODE, "N/A"));
        String issueDate = String.valueOf(data.getOrDefault(ISSUE_DATE, ""));
        String formattedDate = formatIssueDate(issueDate);
        String signatoryName = String.valueOf(data.getOrDefault(SIGNATORY_NAME, "Authorized Signatory"));
        String signatoryTitle = String.valueOf(data.getOrDefault(SIGNATORY_TITLE, "Mr. Dinesh Raywade"));
        String signatoryCompany = String.valueOf(data.getOrDefault(SIGNATORY_COMPANY, "Config Server LLP"));

        // Create a 2-column table for the entire section
        float[] columnWidths = {1, 1};
        Table combinedTable = new Table(UnitValue.createPercentArray(columnWidths));
        combinedTable.setWidth(UnitValue.createPercentValue(100));
        combinedTable.setMarginBottom(40f);

        // Left column - Certificate Number and Issue Date
        Cell leftCell = new Cell();
        leftCell.setBorder(null);
        leftCell.setPadding(0);

        // Certificate Number
        Paragraph certNumber = new Paragraph("Certificate No.")
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(2f);
        leftCell.add(certNumber);

        Paragraph certNumberValue = new Paragraph(verificationCode)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(ACCENT_COLOR)
                .setMarginBottom(5f);
        leftCell.add(certNumberValue);

        // Issue Date
        Paragraph issueDateLabel = new Paragraph("Issued On:")
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(2f);
        leftCell.add(issueDateLabel);

        Paragraph issueDateValue = new Paragraph(formattedDate)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(0f);
        leftCell.add(issueDateValue);

        // Right column - Certificate Verification and Signatory
        Cell rightCell = new Cell();
        rightCell.setBorder(null);
        rightCell.setPadding(0);
        rightCell.setTextAlignment(TextAlignment.RIGHT);

        // Certificate Verification
        Paragraph verificationTitle = new Paragraph("Certificate Verification")
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(5f);
        rightCell.add(verificationTitle);

        // Signatory information
        Paragraph authority = new Paragraph(signatoryName)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(2f);
        rightCell.add(authority);

        Paragraph authorityTitle = new Paragraph(signatoryTitle)
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(2f);
        rightCell.add(authorityTitle);

        Paragraph company = new Paragraph(signatoryCompany)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(0f);
        rightCell.add(company);

        combinedTable.addCell(leftCell);
        combinedTable.addCell(rightCell);
        document.add(combinedTable);

        log.info("✅ Certificate details section added");
    }

    private void addCertificateFooter(Document document, Map<String, Object> data) {
        String platformName = String.valueOf(data.getOrDefault(PLATFORM_NAME, "CSLLP Learning Platform"));

        Paragraph verifyInstructions = new Paragraph("Verify at: " + platformName + " Portal")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(SECONDARY_COLOR)
                .setMarginTop(20f);
        document.add(verifyInstructions);

        log.info("✅ Certificate footer added");
    }

    private String formatTrainingPeriod(String issueDate) {
        if (issueDate == null || issueDate.isEmpty() || "null".equalsIgnoreCase(issueDate)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            String currentPeriod = LocalDate.now().format(formatter);
            log.info("📅 Using current period for training: {}", currentPeriod);
            return currentPeriod;
        }

        try {
            LocalDate date = LocalDate.parse(issueDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            String formattedPeriod = date.format(formatter);
            log.info("📅 Using provided issue date for training period: {}", formattedPeriod);
            return formattedPeriod;
        } catch (Exception e) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            String fallbackPeriod = LocalDate.now().format(formatter);
            log.warn("⚠️ Invalid issue date format: {}, using fallback: {}", issueDate, fallbackPeriod);
            return fallbackPeriod;
        }
    }

    private String formatIssueDate(String issueDate) {
        if (issueDate == null || issueDate.isEmpty() || "null".equalsIgnoreCase(issueDate)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            String currentDate = LocalDate.now().format(formatter);
            return currentDate;
        }

        try {
            LocalDate date = LocalDate.parse(issueDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            return date.format(formatter);
        } catch (Exception e) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            return LocalDate.now().format(formatter);
        }
    }

    // Enhanced helper method with separate first and last names
    public Map<String, Object> prepareCertificateData(String firstName, String lastName, String courseName,
                                                      String issueDate, String verificationCode) {

        // Combine first and last name for display
        String fullName = formatFullName(firstName, lastName);

        Map<String, Object> data = Map.of(
                EMPLOYEE_NAME, fullName,
                COURSE_NAME, courseName != null ? courseName : "Course Name",
                ISSUE_DATE, issueDate != null ? issueDate : LocalDate.now().toString(),
                VERIFICATION_CODE, verificationCode != null ? verificationCode : "CERT-" + System.currentTimeMillis(),
                PLATFORM_NAME, "CSLLP Learning Platform",
                SIGNATORY_NAME, "Authorized Signatory",
                SIGNATORY_TITLE, "Mr. Dinesh Raywade",
                SIGNATORY_COMPANY, "Config Server LLP"
        );

        log.info("📋 Prepared certificate data: {}", data);
        return data;
    }

    // Helper method to format full name properly
    private String formatFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "Employee Name";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}