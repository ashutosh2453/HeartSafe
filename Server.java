package com.heartsafe.backend;

import com.google.gson.Gson;
import com.heartsafe.backend.db.MySql;
import com.heartsafe.shared.models.IncidentReport;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public class Server {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/health", Server::handleHealth);
        server.createContext("/api/incidents/pdf", Server::handleIncidentPdf);
        server.createContext("/api/teleconsult/book", Server::handleTeleconsultBook);

        server.start();
        System.out.println("HeartSafe backend listening on " + port);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        respondJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private static void handleIncidentPdf(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        IncidentReport report = gson.fromJson(readBody(exchange), IncidentReport.class);
        String fileName = "incident-" + UUID.randomUUID() + ".pdf";
        Path out = Files.createTempDirectory("heartsafe").resolve(fileName);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("HeartSafe Incident Report");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 720);
                cs.showText("Patient ID: " + report.patientId);
                cs.newLineAtOffset(0, -18);
                cs.showText("Incident Time: " + report.incidentTime);
                cs.newLineAtOffset(0, -18);
                cs.showText("Description: " + (report.description == null ? "" : report.description));
                cs.endText();
            }
            doc.save(out.toFile());
        } catch (Exception e) {
            respondJson(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            return;
        }

        respondJson(exchange, 201, "{\"message\":\"pdf created\",\"path\":\"" + out.toAbsolutePath().toString().replace("\\", "\\\\") + "\"}");
    }

    private static void handleTeleconsultBook(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        // Parse incoming JSON as a generic map to support flexible booking payloads
        String body = readBody(exchange);
        java.util.Map<String, Object> payload = gson.fromJson(body, java.util.Map.class);

        String patientId = getString(payload.get("patientId"));
        String patientName = getString(payload.get("patientName"));
        String doctorId = getString(payload.get("doctorId"));
        String timeSlotId = getString(payload.get("timeSlotId"));
        String appointmentTimeText = getString(payload.get("appointmentTime"));
        String reason = getString(payload.get("reason"));
        String symptoms = getString(payload.get("symptoms"));
        boolean isEmergency = payload.getOrDefault("isEmergency", "false").toString().equalsIgnoreCase("true");
        String contactPhone = getString(payload.get("contactPhone"));
        String contactEmail = getString(payload.get("contactEmail"));

        // Serialize complex objects (medicalHistory, vitalSigns) to JSON strings
        String medicalHistoryJson = gson.toJson(payload.getOrDefault("medicalHistory", new java.util.HashMap<>()));
        String vitalSignsJson = gson.toJson(payload.getOrDefault("vitalSigns", new java.util.HashMap<>()));

        long generatedId = -1;
        try (Connection conn = MySql.get()) {
            // Create table with richer schema if it doesn't exist
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS teleconsultations ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "patient_id VARCHAR(255), "
                        + "patient_name VARCHAR(255), "
                        + "doctor_id VARCHAR(255), "
                        + "time_slot_id VARCHAR(255), "
                        + "appointment_time_text VARCHAR(255), "
                        + "reason TEXT, "
                        + "symptoms TEXT, "
                        + "is_emergency BOOLEAN DEFAULT FALSE, "
                        + "contact_phone VARCHAR(100), "
                        + "contact_email VARCHAR(255), "
                        + "medical_history TEXT, "
                        + "vital_signs TEXT, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            }

            String insertSql = "INSERT INTO teleconsultations(patient_id, patient_name, doctor_id, time_slot_id, appointment_time_text, reason, symptoms, is_emergency, contact_phone, contact_email, medical_history, vital_signs) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, patientId);
                ps.setString(2, patientName);
                ps.setString(3, doctorId);
                ps.setString(4, timeSlotId);
                ps.setString(5, appointmentTimeText);
                ps.setString(6, reason);
                ps.setString(7, symptoms);
                ps.setBoolean(8, isEmergency);
                ps.setString(9, contactPhone);
                ps.setString(10, contactEmail);
                ps.setString(11, medicalHistoryJson);
                ps.setString(12, vitalSignsJson);

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) generatedId = rs.getLong(1);
                }
            }

        } catch (Exception e) {
            respondJson(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            return;
        }

        respondJson(exchange, 201, "{\"message\":\"booking created\",\"id\":" + generatedId + "}");
    }

    // Helper to safely convert object to string
    private static String getString(Object o) {
        return o == null ? null : o.toString();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }
}
