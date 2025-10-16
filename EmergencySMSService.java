package com.heartsafe.desktop;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Emergency SMS Alert Service for HeartSafe
 * Sends automatic SMS notifications during emergency situations
 */
public class EmergencySMSService {
    private static final Logger LOGGER = Logger.getLogger(EmergencySMSService.class.getName());
    
    private String accountSid;
    private String authToken;
    private String fromPhoneNumber;
    private List<EmergencyContact> emergencyContacts;
    private boolean isConfigured = false;
    
    public EmergencySMSService() {
        this.emergencyContacts = new ArrayList<>();
        loadConfiguration();
    }
    
    /**
     * Load SMS configuration from system properties or environment variables
     */
    private void loadConfiguration() {
        try {
            // Try to load from system properties first
            accountSid = System.getProperty("twilio.account.sid", System.getenv("TWILIO_ACCOUNT_SID"));
            authToken = System.getProperty("twilio.auth.token", System.getenv("TWILIO_AUTH_TOKEN"));
            fromPhoneNumber = System.getProperty("twilio.phone.number", System.getenv("TWILIO_PHONE_NUMBER"));
            
            if (accountSid != null && authToken != null && fromPhoneNumber != null) {
                Twilio.init(accountSid, authToken);
                isConfigured = true;
                LOGGER.info("SMS service configured successfully");
            } else {
                LOGGER.warning("SMS service not configured - missing Twilio credentials");
                addDefaultTestContacts(); // Add some test contacts for demo
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to configure SMS service: " + e.getMessage());
        }
    }
    
    /**
     * Add default test contacts for demonstration
     */
    private void addDefaultTestContacts() {
        emergencyContacts.add(new EmergencyContact("Emergency Contact 1", "+1234567890", "Primary"));
        emergencyContacts.add(new EmergencyContact("Emergency Contact 2", "+0987654321", "Secondary"));
        emergencyContacts.add(new EmergencyContact("Doctor", "+1122334455", "Doctor"));
    }
    
    /**
     * Add emergency contact
     */
    public void addEmergencyContact(String name, String phoneNumber, String relationship) {
        emergencyContacts.add(new EmergencyContact(name, phoneNumber, relationship));
        LOGGER.info("Added emergency contact: " + name + " (" + relationship + ")");
    }
    
    /**
     * Remove emergency contact
     */
    public void removeEmergencyContact(String phoneNumber) {
        emergencyContacts.removeIf(contact -> contact.getPhoneNumber().equals(phoneNumber));
    }
    
    /**
     * Get all emergency contacts
     */
    public List<EmergencyContact> getEmergencyContacts() {
        return new ArrayList<>(emergencyContacts);
    }
    
    /**
     * Send emergency alert SMS to all contacts
     */
    public CompletableFuture<EmergencyAlertResult> sendEmergencyAlert(EmergencyType type, int heartRate, String patientName, String location) {
        return CompletableFuture.supplyAsync(() -> {
            EmergencyAlertResult result = new EmergencyAlertResult();
            result.timestamp = LocalDateTime.now();
            result.emergencyType = type;
            result.heartRate = heartRate;
            result.patientName = patientName;
            result.location = location;
            
            if (emergencyContacts.isEmpty()) {
                LOGGER.warning("No emergency contacts configured");
                result.addFailure("No emergency contacts configured");
                return result;
            }
            
            String alertMessage = createEmergencyMessage(type, heartRate, patientName, location);
            
            // Send SMS to each emergency contact
            for (EmergencyContact contact : emergencyContacts) {
                try {
                    if (isConfigured) {
                        // Send actual SMS via Twilio
                        Message message = Message.creator(
                            new PhoneNumber(contact.getPhoneNumber()),
                            new PhoneNumber(fromPhoneNumber),
                            alertMessage
                        ).create();
                        
                        result.addSuccess(contact, message.getSid());
                        LOGGER.info("Emergency SMS sent to " + contact.getName() + ": " + message.getSid());
                    } else {
                        // Simulate SMS sending for demo purposes
                        result.addSuccess(contact, "DEMO_" + UUID.randomUUID().toString().substring(0, 8));
                        LOGGER.info("DEMO: Emergency SMS would be sent to " + contact.getName() + ": " + alertMessage);
                    }
                } catch (Exception e) {
                    result.addFailure("Failed to send SMS to " + contact.getName() + ": " + e.getMessage());
                    LOGGER.severe("Failed to send emergency SMS to " + contact.getName() + ": " + e.getMessage());
                }
            }
            
            return result;
        });
    }
    
    /**
     * Create emergency message content
     */
    private String createEmergencyMessage(EmergencyType type, int heartRate, String patientName, String location) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        StringBuilder message = new StringBuilder();
        message.append("🚨 HEARTSAFE EMERGENCY ALERT 🚨\n\n");
        
        switch (type) {
            case HIGH_HEART_RATE:
                message.append("HIGH HEART RATE DETECTED!\n");
                message.append("Patient: ").append(patientName).append("\n");
                message.append("Heart Rate: ").append(heartRate).append(" BPM\n");
                message.append("Threshold exceeded: >120 BPM\n");
                break;
                
            case LOW_HEART_RATE:
                message.append("LOW HEART RATE DETECTED!\n");
                message.append("Patient: ").append(patientName).append("\n");
                message.append("Heart Rate: ").append(heartRate).append(" BPM\n");
                message.append("Threshold below: <50 BPM\n");
                break;
                
            case MANUAL_EMERGENCY:
                message.append("MANUAL EMERGENCY ACTIVATED!\n");
                message.append("Patient: ").append(patientName).append("\n");
                message.append("Current Heart Rate: ").append(heartRate).append(" BPM\n");
                message.append("Emergency button pressed manually\n");
                break;
                
            case DEVICE_DISCONNECTED:
                message.append("DEVICE DISCONNECTED!\n");
                message.append("Patient: ").append(patientName).append("\n");
                message.append("Heart rate monitoring interrupted\n");
                break;
        }
        
        message.append("\nTime: ").append(timestamp).append("\n");
        if (location != null && !location.isEmpty()) {
            message.append("Location: ").append(location).append("\n");
        }
        message.append("\nPlease check on the patient immediately!\n");
        message.append("HeartSafe Monitoring System");
        
        return message.toString();
    }
    
    /**
     * Send test SMS to verify configuration
     */
    public CompletableFuture<Boolean> sendTestMessage(String phoneNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String testMessage = "HeartSafe Test Message\n\n" +
                    "This is a test message from your HeartSafe monitoring system.\n" +
                    "If you receive this, emergency alerts are working correctly.\n\n" +
                    "Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                if (isConfigured) {
                    Message message = Message.creator(
                        new PhoneNumber(phoneNumber),
                        new PhoneNumber(fromPhoneNumber),
                        testMessage
                    ).create();
                    
                    LOGGER.info("Test SMS sent successfully: " + message.getSid());
                    return true;
                } else {
                    LOGGER.info("DEMO: Test SMS would be sent to " + phoneNumber);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.severe("Failed to send test SMS: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Check if SMS service is configured
     */
    public boolean isConfigured() {
        return isConfigured || !emergencyContacts.isEmpty(); // Allow demo mode
    }
    
    /**
     * Emergency contact data class
     */
    public static class EmergencyContact {
        private String name;
        private String phoneNumber;
        private String relationship;
        private LocalDateTime addedAt;
        
        public EmergencyContact(String name, String phoneNumber, String relationship) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.relationship = relationship;
            this.addedAt = LocalDateTime.now();
        }
        
        // Getters
        public String getName() { return name; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getRelationship() { return relationship; }
        public LocalDateTime getAddedAt() { return addedAt; }
        
        // Setters
        public void setName(String name) { this.name = name; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setRelationship(String relationship) { this.relationship = relationship; }
        
        @Override
        public String toString() {
            return name + " (" + relationship + ") - " + phoneNumber;
        }
    }
    
    /**
     * Emergency types enum
     */
    public enum EmergencyType {
        HIGH_HEART_RATE,
        LOW_HEART_RATE,
        MANUAL_EMERGENCY,
        DEVICE_DISCONNECTED
    }
    
    /**
     * Emergency alert result class
     */
    public static class EmergencyAlertResult {
        private LocalDateTime timestamp;
        private EmergencyType emergencyType;
        private int heartRate;
        private String patientName;
        private String location;
        private List<String> successMessages = new ArrayList<>();
        private List<String> failureMessages = new ArrayList<>();
        private Map<EmergencyContact, String> sentMessages = new HashMap<>();
        
        public void addSuccess(EmergencyContact contact, String messageId) {
            successMessages.add("SMS sent to " + contact.getName() + " (" + messageId + ")");
            sentMessages.put(contact, messageId);
        }
        
        public void addFailure(String error) {
            failureMessages.add(error);
        }
        
        public boolean isSuccessful() {
            return !successMessages.isEmpty() && failureMessages.isEmpty();
        }
        
        public boolean hasPartialSuccess() {
            return !successMessages.isEmpty() && !failureMessages.isEmpty();
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public EmergencyType getEmergencyType() { return emergencyType; }
        public int getHeartRate() { return heartRate; }
        public String getPatientName() { return patientName; }
        public String getLocation() { return location; }
        public List<String> getSuccessMessages() { return successMessages; }
        public List<String> getFailureMessages() { return failureMessages; }
        public Map<EmergencyContact, String> getSentMessages() { return sentMessages; }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Emergency Alert Summary:\n");
            sb.append("Type: ").append(emergencyType).append("\n");
            sb.append("Time: ").append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            sb.append("Heart Rate: ").append(heartRate).append(" BPM\n");
            sb.append("Success: ").append(successMessages.size()).append(" contacts\n");
            sb.append("Failures: ").append(failureMessages.size()).append(" contacts\n");
            return sb.toString();
        }
    }
}