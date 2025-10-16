package com.heartsafe.desktop;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Incident Report Service for HeartSafe
 * Generates comprehensive PDF reports after emergency incidents
 */
public class IncidentReportService {
    private static final Logger LOGGER = Logger.getLogger(IncidentReportService.class.getName());
    
    private static final String REPORTS_DIRECTORY = "reports";
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public IncidentReportService() {
        // Create reports directory if it doesn't exist
        File reportsDir = new File(REPORTS_DIRECTORY);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
            LOGGER.info("Created reports directory: " + REPORTS_DIRECTORY);
        }
    }
    
    /**
     * Generate comprehensive incident report PDF
     */
    public CompletableFuture<IncidentReport> generateIncidentReport(IncidentData incidentData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String filename = "HeartSafe_Incident_" + incidentData.getTimestamp().format(FILENAME_FORMATTER) + ".pdf";
                String filepath = REPORTS_DIRECTORY + File.separator + filename;
                
                PDDocument document = new PDDocument();
                
                // Add pages to the document
                addCoverPage(document, incidentData);
                addIncidentDetailsPage(document, incidentData);
                addHeartRateAnalysisPage(document, incidentData);
                addEmergencyResponsePage(document, incidentData);
                addRecommendationsPage(document, incidentData);
                
                // Save document
                document.save(filepath);
                document.close();
                
                IncidentReport report = new IncidentReport(filename, filepath, incidentData);
                LOGGER.info("Incident report generated successfully: " + filepath);
                
                return report;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to generate incident report: " + e.getMessage());
                throw new RuntimeException("Failed to generate incident report", e);
            }
        });
    }
    
    /**
     * Add cover page to the report
     */
    private void addCoverPage(PDDocument document, IncidentData incidentData) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float yPosition = yStart;
        
        // Title
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("HEARTSAFE INCIDENT REPORT");
        contentStream.endText();
        yPosition -= 60;
        
        // Red line under title
        contentStream.setStrokingColor(Color.RED);
        contentStream.setLineWidth(2);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(page.getMediaBox().getWidth() - margin, yPosition);
        contentStream.stroke();
        yPosition -= 40;
        
        // Emergency icon (simulated with text)
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 48);
        contentStream.setNonStrokingColor(Color.RED);
        contentStream.newLineAtOffset(margin + 200, yPosition);
        contentStream.showText("🚨");
        contentStream.endText();
        yPosition -= 80;
        
        // Basic incident information
        contentStream.setNonStrokingColor(Color.BLACK);
        addTextLine(contentStream, "EMERGENCY INCIDENT DETAILS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 16);
        yPosition -= 30;
        
        addTextLine(contentStream, "Patient: " + incidentData.getPatientName(), margin, yPosition, PDType1Font.HELVETICA, 12);
        yPosition -= 20;
        
        addTextLine(contentStream, "Date & Time: " + incidentData.getTimestamp().format(DISPLAY_FORMATTER), margin, yPosition, PDType1Font.HELVETICA, 12);
        yPosition -= 20;
        
        addTextLine(contentStream, "Emergency Type: " + incidentData.getEmergencyType(), margin, yPosition, PDType1Font.HELVETICA, 12);
        yPosition -= 20;
        
        addTextLine(contentStream, "Heart Rate: " + incidentData.getTriggerHeartRate() + " BPM", margin, yPosition, PDType1Font.HELVETICA, 12);
        yPosition -= 20;
        
        if (incidentData.getLocation() != null && !incidentData.getLocation().isEmpty()) {
            addTextLine(contentStream, "Location: " + incidentData.getLocation(), margin, yPosition, PDType1Font.HELVETICA, 12);
            yPosition -= 20;
        }
        
        // Report generation info
        yPosition -= 40;
        addTextLine(contentStream, "Report Generated: " + LocalDateTime.now().format(DISPLAY_FORMATTER), margin, yPosition, PDType1Font.HELVETICA_OBLIQUE, 10);
        yPosition -= 15;
        addTextLine(contentStream, "Generated by: HeartSafe Monitoring System v1.0", margin, yPosition, PDType1Font.HELVETICA_OBLIQUE, 10);
        
        // Footer
        addTextLine(contentStream, "This report contains confidential medical information", margin, 50, PDType1Font.HELVETICA_OBLIQUE, 9);
        
        contentStream.close();
    }
    
    /**
     * Add incident details page
     */
    private void addIncidentDetailsPage(PDDocument document, IncidentData incidentData) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float yPosition = yStart;
        
        // Page title
        addTextLine(contentStream, "INCIDENT DETAILS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 18);
        yPosition -= 40;
        
        // Timeline section
        addTextLine(contentStream, "INCIDENT TIMELINE", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        // Add timeline events
        for (String event : incidentData.getTimeline()) {
            addTextLine(contentStream, "• " + event, margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
        }
        
        yPosition -= 20;
        
        // Heart rate analysis
        addTextLine(contentStream, "HEART RATE ANALYSIS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        addTextLine(contentStream, "Trigger Heart Rate: " + incidentData.getTriggerHeartRate() + " BPM", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        
        if (incidentData.getHeartRateHistory() != null && !incidentData.getHeartRateHistory().isEmpty()) {
            addTextLine(contentStream, "Heart Rate History (Last 10 readings):", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            
            for (int i = Math.max(0, incidentData.getHeartRateHistory().size() - 10); 
                 i < incidentData.getHeartRateHistory().size() && yPosition > 100; i++) {
                int hr = incidentData.getHeartRateHistory().get(i);
                String status = getHeartRateStatus(hr);
                addTextLine(contentStream, "    " + hr + " BPM (" + status + ")", margin + 40, yPosition, PDType1Font.HELVETICA, 10);
                yPosition -= 15;
            }
        }
        
        yPosition -= 20;
        
        // Emergency response
        addTextLine(contentStream, "EMERGENCY RESPONSE", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        if (incidentData.getSmsAlertResult() != null) {
            EmergencySMSService.EmergencyAlertResult smsResult = incidentData.getSmsAlertResult();
            addTextLine(contentStream, "SMS Alerts Sent: " + smsResult.getSuccessMessages().size() + " successful", 
                margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            
            for (String success : smsResult.getSuccessMessages()) {
                addTextLine(contentStream, "  • " + success, margin + 40, yPosition, PDType1Font.HELVETICA, 10);
                yPosition -= 15;
                if (yPosition < 100) break;
            }
        }
        
        contentStream.close();
    }
    
    /**
     * Add heart rate analysis page with chart
     */
    private void addHeartRateAnalysisPage(PDDocument document, IncidentData incidentData) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float yPosition = yStart;
        
        // Page title
        addTextLine(contentStream, "HEART RATE ANALYSIS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 18);
        yPosition -= 40;
        
        // Statistics
        if (incidentData.getHeartRateHistory() != null && !incidentData.getHeartRateHistory().isEmpty()) {
            List<Integer> hrData = incidentData.getHeartRateHistory();
            int minHR = Collections.min(hrData);
            int maxHR = Collections.max(hrData);
            double avgHR = hrData.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
            addTextLine(contentStream, "STATISTICAL SUMMARY", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
            yPosition -= 25;
            
            addTextLine(contentStream, "Minimum Heart Rate: " + minHR + " BPM", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            addTextLine(contentStream, "Maximum Heart Rate: " + maxHR + " BPM", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            addTextLine(contentStream, "Average Heart Rate: " + String.format("%.1f", avgHR) + " BPM", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            
            // Count anomalies
            long highCount = hrData.stream().filter(hr -> hr > 120).count();
            long lowCount = hrData.stream().filter(hr -> hr < 50).count();
            
            addTextLine(contentStream, "High HR readings (>120 BPM): " + highCount, margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            addTextLine(contentStream, "Low HR readings (<50 BPM): " + lowCount, margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 40;
        }
        
        // Add simple text-based chart representation
        addTextLine(contentStream, "HEART RATE TREND (Last 20 readings)", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        if (incidentData.getHeartRateHistory() != null && !incidentData.getHeartRateHistory().isEmpty()) {
            List<Integer> recent = incidentData.getHeartRateHistory();
            int startIdx = Math.max(0, recent.size() - 20);
            
            for (int i = startIdx; i < recent.size() && yPosition > 100; i++) {
                int hr = recent.get(i);
                String bar = createTextBar(hr);
                String status = getHeartRateStatus(hr);
                addTextLine(contentStream, String.format("%3d BPM %s [%s]", hr, bar, status), 
                    margin + 20, yPosition, PDType1Font.COURIER, 9);
                yPosition -= 12;
            }
        }
        
        contentStream.close();
    }
    
    /**
     * Add emergency response page
     */
    private void addEmergencyResponsePage(PDDocument document, IncidentData incidentData) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float yPosition = yStart;
        
        // Page title
        addTextLine(contentStream, "EMERGENCY RESPONSE", margin, yPosition, PDType1Font.HELVETICA_BOLD, 18);
        yPosition -= 40;
        
        // Response actions taken
        addTextLine(contentStream, "ACTIONS TAKEN", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        for (String action : incidentData.getResponseActions()) {
            addTextLine(contentStream, "✓ " + action, margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
        }
        
        yPosition -= 20;
        
        // SMS alert details
        if (incidentData.getSmsAlertResult() != null) {
            EmergencySMSService.EmergencyAlertResult smsResult = incidentData.getSmsAlertResult();
            
            addTextLine(contentStream, "SMS ALERT DETAILS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
            yPosition -= 25;
            
            addTextLine(contentStream, "Alert Type: " + smsResult.getEmergencyType(), margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            addTextLine(contentStream, "Sent at: " + smsResult.getTimestamp().format(DISPLAY_FORMATTER), margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
            
            addTextLine(contentStream, "Recipients:", margin + 20, yPosition, PDType1Font.HELVETICA_BOLD, 11);
            yPosition -= 18;
            
            for (String success : smsResult.getSuccessMessages()) {
                addTextLine(contentStream, "  • " + success, margin + 40, yPosition, PDType1Font.HELVETICA, 10);
                yPosition -= 15;
            }
            
            if (!smsResult.getFailureMessages().isEmpty()) {
                yPosition -= 10;
                addTextLine(contentStream, "Failures:", margin + 20, yPosition, PDType1Font.HELVETICA_BOLD, 11);
                yPosition -= 18;
                
                for (String failure : smsResult.getFailureMessages()) {
                    addTextLine(contentStream, "  • " + failure, margin + 40, yPosition, PDType1Font.HELVETICA, 10);
                    yPosition -= 15;
                }
            }
        }
        
        contentStream.close();
    }
    
    /**
     * Add recommendations page
     */
    private void addRecommendationsPage(PDDocument document, IncidentData incidentData) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float yPosition = yStart;
        
        // Page title
        addTextLine(contentStream, "RECOMMENDATIONS & FOLLOW-UP", margin, yPosition, PDType1Font.HELVETICA_BOLD, 18);
        yPosition -= 40;
        
        // Medical recommendations
        addTextLine(contentStream, "MEDICAL RECOMMENDATIONS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        List<String> recommendations = generateRecommendations(incidentData);
        for (String recommendation : recommendations) {
            addTextLine(contentStream, "• " + recommendation, margin + 20, yPosition, PDType1Font.HELVETICA, 11);
            yPosition -= 18;
        }
        
        yPosition -= 20;
        
        // Follow-up actions
        addTextLine(contentStream, "RECOMMENDED FOLLOW-UP", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        
        addTextLine(contentStream, "• Schedule immediate consultation with cardiologist", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        addTextLine(contentStream, "• Continue 24/7 heart rate monitoring", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        addTextLine(contentStream, "• Review and update emergency contact list", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        addTextLine(contentStream, "• Consider wearable device upgrade if applicable", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        
        // Contact information
        yPosition -= 40;
        addTextLine(contentStream, "EMERGENCY CONTACTS", margin, yPosition, PDType1Font.HELVETICA_BOLD, 14);
        yPosition -= 25;
        addTextLine(contentStream, "HeartSafe Support: +1-800-HEARTSAFE", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        addTextLine(contentStream, "Emergency Services: 911", margin + 20, yPosition, PDType1Font.HELVETICA, 11);
        yPosition -= 18;
        
        contentStream.close();
    }
    
    /**
     * Helper method to add text line
     */
    private void addTextLine(PDPageContentStream contentStream, String text, float x, float y, 
                           PDType1Font font, int fontSize) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
    
    /**
     * Get heart rate status description
     */
    private String getHeartRateStatus(int heartRate) {
        if (heartRate < 50) return "Low";
        if (heartRate <= 100) return "Normal";
        if (heartRate <= 120) return "Elevated";
        return "High";
    }
    
    /**
     * Create text-based bar for heart rate visualization
     */
    private String createTextBar(int heartRate) {
        int barLength = Math.max(1, Math.min(20, heartRate / 5));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append("█");
        }
        return bar.toString();
    }
    
    /**
     * Generate medical recommendations based on incident data
     */
    private List<String> generateRecommendations(IncidentData incidentData) {
        List<String> recommendations = new ArrayList<>();
        
        switch (incidentData.getEmergencyType()) {
            case "HIGH_HEART_RATE":
                recommendations.add("Immediate medical evaluation for tachycardia");
                recommendations.add("Review current medications with physician");
                recommendations.add("Monitor for signs of cardiac arrhythmia");
                break;
            case "LOW_HEART_RATE":
                recommendations.add("Immediate medical evaluation for bradycardia");
                recommendations.add("Check for medication side effects");
                recommendations.add("Consider pacemaker evaluation if persistent");
                break;
            case "MANUAL_EMERGENCY":
                recommendations.add("Complete medical assessment recommended");
                recommendations.add("Review emergency response procedures with patient");
                break;
            default:
                recommendations.add("General cardiac evaluation recommended");
        }
        
        recommendations.add("Maintain regular monitoring schedule");
        recommendations.add("Keep emergency contacts updated");
        
        return recommendations;
    }
    
    /**
     * Get all generated reports
     */
    public List<IncidentReport> getAllReports() {
        List<IncidentReport> reports = new ArrayList<>();
        File reportsDir = new File(REPORTS_DIRECTORY);
        
        if (reportsDir.exists() && reportsDir.isDirectory()) {
            File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".pdf"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                
                for (File file : files) {
                    reports.add(new IncidentReport(file.getName(), file.getAbsolutePath(), null));
                }
            }
        }
        
        return reports;
    }
    
    /**
     * Incident data container class
     */
    public static class IncidentData {
        private String patientName;
        private LocalDateTime timestamp;
        private String emergencyType;
        private int triggerHeartRate;
        private String location;
        private List<Integer> heartRateHistory;
        private List<String> timeline;
        private List<String> responseActions;
        private EmergencySMSService.EmergencyAlertResult smsAlertResult;
        
        public IncidentData(String patientName, LocalDateTime timestamp, String emergencyType, 
                          int triggerHeartRate) {
            this.patientName = patientName;
            this.timestamp = timestamp;
            this.emergencyType = emergencyType;
            this.triggerHeartRate = triggerHeartRate;
            this.timeline = new ArrayList<>();
            this.responseActions = new ArrayList<>();
            this.heartRateHistory = new ArrayList<>();
        }
        
        // Getters and setters
        public String getPatientName() { return patientName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEmergencyType() { return emergencyType; }
        public int getTriggerHeartRate() { return triggerHeartRate; }
        public String getLocation() { return location; }
        public List<Integer> getHeartRateHistory() { return heartRateHistory; }
        public List<String> getTimeline() { return timeline; }
        public List<String> getResponseActions() { return responseActions; }
        public EmergencySMSService.EmergencyAlertResult getSmsAlertResult() { return smsAlertResult; }
        
        public void setLocation(String location) { this.location = location; }
        public void setHeartRateHistory(List<Integer> heartRateHistory) { this.heartRateHistory = heartRateHistory; }
        public void setSmsAlertResult(EmergencySMSService.EmergencyAlertResult smsAlertResult) { this.smsAlertResult = smsAlertResult; }
        
        public void addTimelineEvent(String event) { 
            timeline.add(LocalDateTime.now().format(DISPLAY_FORMATTER) + " - " + event); 
        }
        
        public void addResponseAction(String action) { 
            responseActions.add(action); 
        }
    }
    
    /**
     * Incident report container class
     */
    public static class IncidentReport {
        private String filename;
        private String filepath;
        private LocalDateTime generatedAt;
        private IncidentData incidentData;
        
        public IncidentReport(String filename, String filepath, IncidentData incidentData) {
            this.filename = filename;
            this.filepath = filepath;
            this.generatedAt = LocalDateTime.now();
            this.incidentData = incidentData;
        }
        
        public String getFilename() { return filename; }
        public String getFilepath() { return filepath; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public IncidentData getIncidentData() { return incidentData; }
        
        @Override
        public String toString() {
            return filename + " (Generated: " + generatedAt.format(DISPLAY_FORMATTER) + ")";
        }
    }
}