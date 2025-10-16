package com.heartsafe.desktop;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    // UI Components
    private JFrame frame;
    private JLabel hrLabel;
    private JLabel statusLabel;
    private JTextArea logArea;
    private ModernButton startBtn;
    private ModernButton stopBtn;
    private ModernButton emergencyBtn;
    private ModernButton bookBtn;
    private HeartRateChart chartPanel;
    private JPanel headerPanel;
    
    // Data and Threading
    private ScheduledExecutorService scheduler;
    private final List<Integer> lastReadings = new ArrayList<>();
    private boolean isMonitoring = false;
    
    // New Services
    private GoogleFitServiceDemo googleFitService;
    private EmergencySMSService smsService;
    private IncidentReportService reportService;
    private TeleconsultationService teleconsultService;
    
    // Patient info
    private String patientName = "Demo Patient";
    private String patientLocation = "Home";
    
    // New UI Components
    private ModernButton configBtn;
    private ModernButton reportsBtn;
    private ModernButton contactsBtn;
    private JLabel serviceStatusLabel;
    private boolean useGoogleFit = false;
    
    // Modern Color Scheme - Medical Theme
    public static final Color PRIMARY_RED = new Color(229, 57, 53);
    public static final Color PRIMARY_DARK = new Color(198, 40, 40);
    public static final Color SECONDARY_GREEN = new Color(46, 125, 50);
    public static final Color ACCENT_ORANGE = new Color(255, 111, 0);
    public static final Color BACKGROUND_LIGHT = new Color(250, 250, 250);
    public static final Color SURFACE_WHITE = new Color(255, 255, 255);
    public static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    public static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    public static final Color EMERGENCY_RED = new Color(211, 47, 47);

    // Choose an emoji-capable font when available to avoid tofu (square boxes) for emoji characters
    private Font getPreferredFont(int style, int size) {
        String[] candidates = new String[] {
            "Segoe UI Emoji",
            "Segoe UI Symbol",
            "Noto Color Emoji",
            "Apple Color Emoji",
            "Segoe UI",
            "SansSerif"
        };
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>(java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, style, size);
            }
        }
        return new Font("SansSerif", style, size);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        // Set system look and feel with modern enhancements
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        frame = new JFrame("❤️ HeartSafe Desktop Monitor");
        frame.setSize(900, 700);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BACKGROUND_LIGHT);
        
        // Create main layout
        frame.setLayout(new BorderLayout(10, 10));
        
        // Header Panel with gradient background
        createHeaderPanel();
        frame.add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_LIGHT);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Heart rate display panel
        JPanel hrPanel = createHeartRatePanel();
        mainPanel.add(hrPanel, BorderLayout.NORTH);
        
        // Chart panel
        chartPanel = new HeartRateChart();
        JPanel chartContainer = createStyledPanel("📈 Heart Rate Trends", chartPanel);
        mainPanel.add(chartContainer, BorderLayout.CENTER);
        
        // Control buttons panel
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        frame.add(mainPanel, BorderLayout.CENTER);
        
        // Activity log panel
        JPanel logPanel = createLogPanel();
        frame.add(logPanel, BorderLayout.EAST);
        
        wireEvents();
        frame.addWindowListener(new WindowAdapter() {
            @Override 
            public void windowClosing(WindowEvent e) { 
                shutdown(); 
            }
        });
        
        // Show the frame
        frame.setVisible(true);
        
        // Initialize new services
        initializeServices();
        
        log("🎉 HeartSafe Desktop Application Started");
    }

    private void createHeaderPanel() {
        headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, PRIMARY_RED,
                    getWidth(), getHeight(), PRIMARY_DARK
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 80));
        
    JLabel titleLabel = new JLabel("❤️ HeartSafe Desktop Monitor", JLabel.CENTER);
    titleLabel.setFont(getPreferredFont(Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        headerPanel.add(titleLabel, BorderLayout.CENTER);
    }
    
    private JPanel createHeartRatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1),
            new EmptyBorder(25, 25, 25, 25)
        ));
        
        // Heart rate display
    hrLabel = new JLabel("-- BPM", JLabel.CENTER);
    hrLabel.setFont(getPreferredFont(Font.BOLD, 48));
        hrLabel.setForeground(PRIMARY_RED);
        hrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Status label
    statusLabel = new JLabel("🔴 Ready to Monitor", JLabel.CENTER);
    statusLabel.setFont(getPreferredFont(Font.PLAIN, 16));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(Box.createVerticalGlue());
        panel.add(hrLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(statusLabel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBackground(BACKGROUND_LIGHT);
        
        startBtn = new ModernButton("▶️ Start Monitoring", SECONDARY_GREEN, Color.WHITE);
        stopBtn = new ModernButton("⏹️ Stop Monitoring", TEXT_SECONDARY, Color.WHITE);
        emergencyBtn = new ModernButton("🚨 EMERGENCY", EMERGENCY_RED, Color.WHITE);
        bookBtn = new ModernButton("📞 Book Teleconsult", ACCENT_ORANGE, Color.WHITE);
        ModernButton generateReportBtn = new ModernButton("📄 Generate Report", new Color(60, 120, 180), Color.WHITE);
        
        // New feature buttons
        configBtn = new ModernButton("⚙️ Settings", new Color(100, 100, 100), Color.WHITE);
        reportsBtn = new ModernButton("📄 Reports", new Color(60, 120, 180), Color.WHITE);
        contactsBtn = new ModernButton("📞 Contacts", new Color(180, 80, 120), Color.WHITE);
        
        stopBtn.setEnabled(false);
        
        // First row - main controls
        panel.add(startBtn);
        panel.add(stopBtn);
        panel.add(emergencyBtn);
        panel.add(bookBtn);
        
        // Second row - additional features
        panel.add(configBtn);
        panel.add(reportsBtn);
        panel.add(contactsBtn);
        panel.add(generateReportBtn);

        generateReportBtn.addActionListener(e -> {
            generateReportBtn.animateClick();
            // Create a normal session report on demand
            generateSessionReportAndShow();
        });
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = createStyledPanel("📋 Activity Log", null);
        panel.setPreferredSize(new Dimension(300, 0));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(248, 249, 250));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createStyledPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        if (title != null) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(getPreferredFont(Font.BOLD, 16));
            titleLabel.setForeground(TEXT_PRIMARY);
            titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
            panel.add(titleLabel, BorderLayout.NORTH);
        }
        
        if (content != null) {
            panel.add(content, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    private void wireEvents() {
        startBtn.addActionListener(e -> {
            startBtn.animateClick();
            beginMonitoring();
        });
        
        stopBtn.addActionListener(e -> {
            stopBtn.animateClick();
            stopMonitoring();
        });
        
        emergencyBtn.addActionListener(e -> {
            emergencyBtn.animateClick();
            triggerEmergency();
        });
        
        bookBtn.addActionListener(e -> {
            bookBtn.animateClick();
            showTeleconsultDialog();
        });
        
        // New button handlers
        configBtn.addActionListener(e -> {
            configBtn.animateClick();
            showConfigurationDialog();
        });
        
        reportsBtn.addActionListener(e -> {
            reportsBtn.animateClick();
            showReportsDialog();
        });
        
        contactsBtn.addActionListener(e -> {
            contactsBtn.animateClick();
            showContactsDialog();
        });
    }
    
    private void initializeServices() {
        log("🔧 Initializing HeartSafe services...");
        
        // Initialize Google Fit Service
        googleFitService = new GoogleFitServiceDemo();
        googleFitService.initialize().thenAccept(success -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    log("✅ Google Fit service initialized successfully");
                    useGoogleFit = true;
                } else {
                    log("⚠️ Google Fit service initialization failed - using simulation mode");
                }
            });
        });
        
        // Initialize SMS Service
        smsService = new EmergencySMSService();
        if (smsService.isConfigured()) {
            log("✅ SMS emergency service configured");
        } else {
            log("⚠️ SMS service in demo mode - configure Twilio for real alerts");
        }
        
        // Initialize Report Service
        reportService = new IncidentReportService();
        log("✅ PDF report service initialized");
        
        // Initialize Teleconsultation Service
        teleconsultService = new TeleconsultationService();
        log("✅ Teleconsultation service initialized");
        
        log("✨ All services initialized successfully!");
    }
    
    private void showConfigurationDialog() {
        JDialog configDialog = new JDialog(frame, "HeartSafe Configuration", true);
        configDialog.setSize(500, 400);
        configDialog.setLocationRelativeTo(frame);
        
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setText(
            "HeartSafe Configuration\n\n" +
            "Google Fit Integration:\n" +
            "- Status: " + (useGoogleFit ? "Enabled (Demo Mode)" : "Disabled") + "\n" +
            "- To enable: Add credentials.json to project root\n\n" +
            "SMS Emergency Alerts:\n" +
            "- Status: " + (smsService.isConfigured() ? "Configured" : "Demo Mode") + "\n" +
            "- Configure Twilio: Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER\n\n" +
            "Patient Information:\n" +
            "- Name: " + patientName + "\n" +
            "- Location: " + patientLocation + "\n\n" +
            "Reports Directory: ./reports/\n" +
            "Emergency Contacts: " + smsService.getEmergencyContacts().size() + " configured"
        );
        
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> configDialog.dispose());
        panel.add(closeBtn, BorderLayout.SOUTH);
        
        configDialog.add(panel);
        configDialog.setVisible(true);
        
        log("⚙️ Configuration dialog opened");
    }
    
    private void showReportsDialog() {
        JDialog reportsDialog = new JDialog(frame, "Incident Reports", true);
        reportsDialog.setSize(600, 500);
        reportsDialog.setLocationRelativeTo(frame);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<IncidentReportService.IncidentReport> reports = reportService.getAllReports();
        
        if (reports.isEmpty()) {
            listModel.addElement("No incident reports generated yet");
            listModel.addElement("Reports will appear here after emergencies are triggered");
        } else {
            for (IncidentReportService.IncidentReport report : reports) {
                listModel.addElement(report.toString());
            }
        }
        
        JList<String> reportsList = new JList<>(listModel);
        panel.add(new JScrollPane(reportsList), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton openBtn = new JButton("Open Report");
    JButton downloadBtn = new JButton("Download Report");
        JButton refreshBtn = new JButton("Refresh");
        JButton closeBtn = new JButton("Close");
        
        openBtn.addActionListener(e -> {
            if (!reports.isEmpty() && reportsList.getSelectedIndex() >= 0) {
                IncidentReportService.IncidentReport selectedReport = reports.get(reportsList.getSelectedIndex());
                try {
                    Desktop.getDesktop().open(new java.io.File(selectedReport.getFilepath()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(reportsDialog, "Could not open report: " + ex.getMessage());
                }
            }
        });

        downloadBtn.addActionListener(e -> {
            if (!reports.isEmpty() && reportsList.getSelectedIndex() >= 0) {
                IncidentReportService.IncidentReport selectedReport = reports.get(reportsList.getSelectedIndex());
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new java.io.File(selectedReport.getFilename()));
                int res = chooser.showSaveDialog(reportsDialog);
                if (res == JFileChooser.APPROVE_OPTION) {
                    java.io.File dest = chooser.getSelectedFile();
                    try {
                        java.nio.file.Path src = java.nio.file.Paths.get(selectedReport.getFilepath());
                        java.nio.file.Files.copy(src, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        JOptionPane.showMessageDialog(reportsDialog, "Report saved to: " + dest.getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(reportsDialog, "Failed to save report: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        refreshBtn.addActionListener(e -> {
            reportsDialog.dispose();
            showReportsDialog();
        });
        
        closeBtn.addActionListener(e -> reportsDialog.dispose());
        
    buttonPanel.add(openBtn);
    buttonPanel.add(downloadBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        reportsDialog.add(panel);
        reportsDialog.setVisible(true);
        
        log("📄 Reports dialog opened - " + reports.size() + " reports available");
    }
    
    private void showContactsDialog() {
        JDialog contactsDialog = new JDialog(frame, "Emergency Contacts", true);
        contactsDialog.setSize(500, 400);
        contactsDialog.setLocationRelativeTo(frame);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<EmergencySMSService.EmergencyContact> contacts = smsService.getEmergencyContacts();
        
        for (EmergencySMSService.EmergencyContact contact : contacts) {
            listModel.addElement(contact.toString());
        }
        
        JList<String> contactsList = new JList<>(listModel);
        panel.add(new JScrollPane(contactsList), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Contact");
        JButton testBtn = new JButton("Send Test SMS");
        JButton closeBtn = new JButton("Close");
        
        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(contactsDialog, "Enter contact name:");
            if (name != null && !name.trim().isEmpty()) {
                String phone = JOptionPane.showInputDialog(contactsDialog, "Enter phone number:");
                if (phone != null && !phone.trim().isEmpty()) {
                    String relationship = JOptionPane.showInputDialog(contactsDialog, "Enter relationship:");
                    if (relationship == null || relationship.trim().isEmpty()) {
                        relationship = "Emergency Contact";
                    }
                    smsService.addEmergencyContact(name.trim(), phone.trim(), relationship.trim());
                    contactsDialog.dispose();
                    showContactsDialog(); // Refresh
                }
            }
        });
        
        testBtn.addActionListener(e -> {
            if (contactsList.getSelectedIndex() >= 0) {
                EmergencySMSService.EmergencyContact contact = contacts.get(contactsList.getSelectedIndex());
                smsService.sendTestMessage(contact.getPhoneNumber()).thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            JOptionPane.showMessageDialog(contactsDialog, "Test message sent to " + contact.getName());
                        } else {
                            JOptionPane.showMessageDialog(contactsDialog, "Failed to send test message");
                        }
                    });
                });
            }
        });
        
        closeBtn.addActionListener(e -> contactsDialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(testBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        contactsDialog.add(panel);
        contactsDialog.setVisible(true);
        
        log("📞 Emergency contacts dialog opened - " + contacts.size() + " contacts");
    }
    
    private void showTeleconsultDialog() {
        JDialog teleconsultDialog = new JDialog(frame, "Book Teleconsultation", true);
        teleconsultDialog.setSize(700, 600);
        teleconsultDialog.setLocationRelativeTo(frame);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setText("Loading available doctors...");
        
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        // Load doctors asynchronously
        teleconsultService.getAvailableDoctors().thenAccept(doctors -> {
            SwingUtilities.invokeLater(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Available Doctors for Teleconsultation:\n\n");
                
                for (int i = 0; i < doctors.size(); i++) {
                    TeleconsultationService.Doctor doctor = doctors.get(i);
                    sb.append((i + 1)).append(". ").append(doctor.name).append("\n");
                    sb.append("   Specialty: ").append(doctor.specialty).append("\n");
                    sb.append("   Experience: ").append(doctor.experience).append(" years\n");
                    sb.append("   Rating: ").append(doctor.rating).append("/5.0 ⭐\n");
                    sb.append("   Fee: $").append(doctor.consultationFee).append("\n");
                    sb.append("   Availability: ").append(doctor.availability).append("\n\n");
                }
                
                sb.append("To book an appointment:\n");
                sb.append("1. Select a doctor\n");
                sb.append("2. Choose consultation type (Regular/Emergency)\n");
                sb.append("3. Provide symptoms and medical history\n\n");
                
                if (smsService.isConfigured()) {
                    sb.append("Emergency consultations are available 24/7\n");
                } else {
                    sb.append("Demo mode: All bookings are simulated\n");
                }
                
                infoArea.setText(sb.toString());
            });
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton bookRegularBtn = new JButton("Book Regular Consultation");
        JButton bookEmergencyBtn = new JButton("Book Emergency Consultation");
        JButton closeBtn = new JButton("Close");
        
        bookRegularBtn.addActionListener(e -> {
            bookRegularConsultation();
            teleconsultDialog.dispose();
        });
        
        bookEmergencyBtn.addActionListener(e -> {
            bookEmergencyConsultation();
            teleconsultDialog.dispose();
        });
        
        closeBtn.addActionListener(e -> teleconsultDialog.dispose());
        
        buttonPanel.add(bookRegularBtn);
        buttonPanel.add(bookEmergencyBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        teleconsultDialog.add(panel);
        teleconsultDialog.setVisible(true);
        
        log("📞 Teleconsultation dialog opened");
    }
    
    private void bookRegularConsultation() {
        TeleconsultationService.ConsultationBooking booking = new TeleconsultationService.ConsultationBooking();
        booking.patientId = "demo_patient";
        booking.patientName = patientName;
        booking.reason = "Regular heart rate consultation";
        booking.symptoms = "Heart rate monitoring concerns";
        booking.contactPhone = "+1234567890";
        
        teleconsultService.bookConsultation(booking).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                if (result.success) {
                    JOptionPane.showMessageDialog(frame, 
                        "Consultation booked successfully!\n" +
                        "Booking ID: " + result.bookingId + "\n" +
                        "Confirmation: " + result.confirmationNumber,
                        "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    log("✅ Regular teleconsultation booked: " + result.bookingId);
                } else {
                    JOptionPane.showMessageDialog(frame, "Booking failed: " + result.message, 
                        "Booking Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }
    
    private void bookEmergencyConsultation() {
        TeleconsultationService.EmergencyConsultationRequest request = new TeleconsultationService.EmergencyConsultationRequest();
        request.patientId = "demo_patient";
        request.patientName = patientName;
        request.emergencyType = "HIGH_HEART_RATE";
        request.currentHeartRate = lastReadings.isEmpty() ? 0 : lastReadings.get(lastReadings.size() - 1);
        request.symptoms = "Elevated heart rate detected by monitoring system";
        request.contactPhone = "+1234567890";
        request.location = patientLocation;
        
        teleconsultService.bookEmergencyConsultation(request).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                if (result.success) {
                    JOptionPane.showMessageDialog(frame, 
                        "Emergency consultation booked!\n" +
                        "Priority: URGENT\n" +
                        "Booking ID: " + result.bookingId + "\n" +
                        "Estimated wait time: " + result.estimatedWaitTime + "\n" +
                        "Message: " + result.message,
                        "Emergency Consultation Booked", JOptionPane.WARNING_MESSAGE);
                    log("🚨 Emergency teleconsultation booked: " + result.bookingId);
                } else {
                    JOptionPane.showMessageDialog(frame, "Emergency booking failed: " + result.message, 
                        "Emergency Booking Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    private void beginMonitoring() {
        if (isMonitoring) return;
        
        isMonitoring = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        Random rnd = new Random();
        
        // Update UI state
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        updateStatus("🟢 Monitoring Active", SECONDARY_GREEN);
        
        scheduler.scheduleAtFixedRate(() -> {
            // Generate realistic heart rate data
            int baseHR = 70;
            int variation = rnd.nextInt(30) - 15; // +/- 15 BPM variation
            int hr = Math.max(50, Math.min(150, baseHR + variation));
            
            SwingUtilities.invokeLater(() -> {
                updateHeartRate(hr);
                chartPanel.addDataPoint(hr);
                
                // Check for anomalies
                if (hr >= 120) {
                    emergencyAlert("⚠️ High Heart Rate Detected: " + hr + " BPM");
                } else if (hr <= 50) {
                    emergencyAlert("⚠️ Low Heart Rate Detected: " + hr + " BPM");
                }
            });
        }, 0, 2, TimeUnit.SECONDS);
        
        log("▶️ Heart rate monitoring started");
    }
    
    private void stopMonitoring() {
        if (!isMonitoring) return;
        
        isMonitoring = false;
        
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        
        // Update UI state
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        updateStatus("🔴 Monitoring Stopped", TEXT_SECONDARY);
        
        log("⏹️ Heart rate monitoring stopped");

            // After stopping, do not generate a session report
    }

    private void generateSessionReportAndShow() {
        // Build incident data for the monitoring session
        int triggerHr = lastReadings.isEmpty() ? 0 : lastReadings.get(lastReadings.size() - 1);
        IncidentReportService.IncidentData incidentData = new IncidentReportService.IncidentData(
            patientName,
            LocalDateTime.now(),
            "MONITORING_SESSION",
            triggerHr
        );

        incidentData.setLocation(patientLocation);
        // Copy recent history
        incidentData.setHeartRateHistory(new ArrayList<>(lastReadings));
        incidentData.addTimelineEvent("Monitoring session stopped");
        incidentData.addResponseAction("Session ended by user");

        log("📄 Generating session report...");

        reportService.generateIncidentReport(incidentData).thenAccept(report -> {
            SwingUtilities.invokeLater(() -> {
                log("✅ Report generated: " + report.getFilename());
                showGeneratedReportDialog(report);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                log("❌ Failed to generate report: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, "Failed to generate report: " + ex.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }

    private void showGeneratedReportDialog(IncidentReportService.IncidentReport report) {
        JDialog dialog = new JDialog(frame, "Session Report", true);
        dialog.setSize(500, 180);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lbl = new JLabel("Report created: " + report.getFilename());
        panel.add(lbl, BorderLayout.NORTH);

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setText("Location: " + (report.getIncidentData().getLocation() == null ? "N/A" : report.getIncidentData().getLocation()) + "\n"
            + "Generated: " + report.getGeneratedAt() + "\n"
            + "Path: " + report.getFilepath());
        info.setBackground(dialog.getBackground());
        panel.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openBtn = new JButton("Open Report");
        JButton saveAsBtn = new JButton("Save As...");
        JButton closeBtn = new JButton("Close");

        openBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(report.getFilepath()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Could not open report: " + ex.getMessage());
            }
        });

        saveAsBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(report.getFilename()));
            int res = chooser.showSaveDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File dest = chooser.getSelectedFile();
                try {
                    Path src = Paths.get(report.getFilepath());
                    Files.copy(src, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(dialog, "Report saved to: " + dest.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to save report: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        btns.add(openBtn);
        btns.add(saveAsBtn);
        btns.add(closeBtn);

        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void updateHeartRate(int hr) {
        lastReadings.add(hr);
        if (lastReadings.size() > 50) lastReadings.remove(0);
        
        // Animate heart rate update
        String newText = hr + " BPM";
        hrLabel.setText(newText);
        
        // Color coding based on heart rate
        Color hrColor;
        if (hr >= 120 || hr <= 50) {
            hrColor = EMERGENCY_RED;
        } else if (hr >= 100 || hr <= 60) {
            hrColor = ACCENT_ORANGE;
        } else {
            hrColor = SECONDARY_GREEN;
        }
        hrLabel.setForeground(hrColor);
        
        // Pulse animation for heart rate label
        Timer pulseTimer = new Timer(100, new ActionListener() {
            boolean growing = true;
            int count = 0;
            Font originalFont = hrLabel.getFont();
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (growing) {
                    hrLabel.setFont(originalFont.deriveFont(originalFont.getSize() + 2f));
                } else {
                    hrLabel.setFont(originalFont);
                }
                growing = !growing;
                count++;
                if (count >= 4) {
                    ((Timer) e.getSource()).stop();
                    hrLabel.setFont(originalFont);
                }
            }
        });
        pulseTimer.start();
    }
    
    private void updateStatus(String status, Color color) {
        statusLabel.setText(status);
        statusLabel.setForeground(color);
    }
    
    private void triggerEmergency() {
        // Flash emergency alert
        Timer flashTimer = new Timer(200, new ActionListener() {
            int count = 0;
            Color originalColor = frame.getContentPane().getBackground();
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count % 2 == 0) {
                    frame.getContentPane().setBackground(new Color(255, 200, 200));
                } else {
                    frame.getContentPane().setBackground(originalColor);
                }
                count++;
                if (count >= 6) {
                    ((Timer) e.getSource()).stop();
                    frame.getContentPane().setBackground(originalColor);
                }
                frame.repaint();
            }
        });
        flashTimer.start();
        
        updateStatus("🆘 EMERGENCY MODE ACTIVE", EMERGENCY_RED);
        emergencyAlert("🚨 EMERGENCY ALERT TRIGGERED!");
        log("🚨 EMERGENCY: Manual emergency alert activated");
    }

    private void emergencyAlert(String msg) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(frame, msg, "HeartSafe Alert", JOptionPane.WARNING_MESSAGE);
        log("ALERT: " + msg);
    }

    private void bookTeleconsult() {
        try {
            URL url = new URL("http://localhost:8081/api/teleconsult/book");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            String patientId = System.getProperty("user.name", "demo");
            long incidentTime = System.currentTimeMillis();
            String description = "Triggered from desktop UI";
            String json = "{" +
                    "\"patientId\":\"" + escape(patientId) + "\"," +
                    "\"incidentTime\":" + incidentTime + "," +
                    "\"description\":\"" + escape(description) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log("Teleconsult response (" + code + "): " + resp);
        } catch (Exception ex) {
            log("Teleconsult failed: " + ex.getMessage());
        }
    }

    private static String escape(String s) { return s.replace("\"", "'"); }

    private void log(String s) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logArea.append("[" + ts + "] " + s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void shutdown() {
        stopMonitoring();
    }

    // Modern Button Class with animations and styling
    @SuppressWarnings("serial")
    class ModernButton extends JButton {
        private Color backgroundColor;
        private Color hoverColor;
        private boolean isHovered = false;
        
        public ModernButton(String text, Color bgColor, Color textColor) {
            super(text);
            this.backgroundColor = bgColor;
            this.hoverColor = bgColor.brighter();
            
            setForeground(textColor);
            setFont(getPreferredFont(Font.BOLD, 14));
            setPreferredSize(new Dimension(180, 45));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color currentColor = isHovered ? hoverColor : backgroundColor;
            if (!isEnabled()) {
                currentColor = new Color(200, 200, 200);
            }
            
            g2d.setColor(currentColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            
            g2d.dispose();
            super.paintComponent(g);
        }
        
        public void animateClick() {
            Timer clickTimer = new Timer(50, new ActionListener() {
                int count = 0;
                Dimension originalSize = getSize();
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (count == 0) {
                        setSize(originalSize.width - 4, originalSize.height - 2);
                    } else if (count == 2) {
                        setSize(originalSize);
                        ((Timer) e.getSource()).stop();
                    }
                    count++;
                    repaint();
                }
            });
            clickTimer.start();
        }
    }
    
    // Modern Heart Rate Chart with real-time visualization
    @SuppressWarnings("serial")
    class HeartRateChart extends JPanel {
        private List<Integer> dataPoints = new ArrayList<>();
        private final int MAX_POINTS = 50;
        
        public HeartRateChart() {
            setBackground(SURFACE_WHITE);
            setPreferredSize(new Dimension(0, 250));
        }
        
        public void addDataPoint(int heartRate) {
            dataPoints.add(heartRate);
            if (dataPoints.size() > MAX_POINTS) {
                dataPoints.remove(0);
            }
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int padding = 40;
            
            // Draw background grid
            g2d.setColor(new Color(240, 240, 240));
            g2d.setStroke(new BasicStroke(1));
            
            // Horizontal grid lines
            for (int i = 0; i <= 10; i++) {
                int y = padding + (height - 2 * padding) * i / 10;
                g2d.drawLine(padding, y, width - padding, y);
            }
            
            // Vertical grid lines
            for (int i = 0; i <= 10; i++) {
                int x = padding + (width - 2 * padding) * i / 10;
                g2d.drawLine(x, padding, x, height - padding);
            }
            
            // Draw axes labels
            g2d.setColor(TEXT_SECONDARY);
            g2d.setFont(getPreferredFont(Font.PLAIN, 10));
            
            // Y-axis labels (BPM)
            for (int i = 0; i <= 10; i++) {
                int bpm = 40 + (140 - 40) * (10 - i) / 10;
                int y = padding + (height - 2 * padding) * i / 10;
                g2d.drawString(String.valueOf(bpm), 5, y + 3);
            }
            
            // Draw chart title
            g2d.setColor(TEXT_PRIMARY);
            g2d.setFont(getPreferredFont(Font.BOLD, 12));
            g2d.drawString("BPM", 8, 25);
            
            if (dataPoints.isEmpty()) {
                // Draw "No Data" message
                g2d.setColor(TEXT_SECONDARY);
                g2d.setFont(getPreferredFont(Font.ITALIC, 16));
                String noDataText = "Start monitoring to see heart rate data";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(noDataText);
                g2d.drawString(noDataText, (width - textWidth) / 2, height / 2);
                g2d.dispose();
                return;
            }
            
            // Draw heart rate line
            g2d.setColor(PRIMARY_RED);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            Path2D.Double path = new Path2D.Double();
            
            for (int i = 0; i < dataPoints.size(); i++) {
                int hr = dataPoints.get(i);
                int x = padding + (width - 2 * padding) * i / Math.max(1, dataPoints.size() - 1);
                int y = height - padding - ((hr - 40) * (height - 2 * padding) / (140 - 40));
                
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            
            g2d.draw(path);
            
            // Draw data points
            g2d.setColor(PRIMARY_DARK);
            for (int i = 0; i < dataPoints.size(); i++) {
                int hr = dataPoints.get(i);
                int x = padding + (width - 2 * padding) * i / Math.max(1, dataPoints.size() - 1);
                int y = height - padding - ((hr - 40) * (height - 2 * padding) / (140 - 40));
                
                // Highlight current point
                if (i == dataPoints.size() - 1) {
                    g2d.fillOval(x - 6, y - 6, 12, 12);
                    g2d.setColor(SURFACE_WHITE);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                    g2d.setColor(PRIMARY_DARK);
                } else {
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
            
            // Draw current heart rate value
            if (!dataPoints.isEmpty()) {
                int currentHR = dataPoints.get(dataPoints.size() - 1);
                int currentX = padding + (width - 2 * padding) * (dataPoints.size() - 1) / Math.max(1, dataPoints.size() - 1);
                int currentY = height - padding - ((currentHR - 40) * (height - 2 * padding) / (140 - 40));
                
                g2d.setColor(TEXT_PRIMARY);
                g2d.setFont(getPreferredFont(Font.BOLD, 12));
                String hrText = currentHR + " BPM";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(hrText);
                
                // Position text to avoid edge cutoff
                int textX = Math.min(currentX - textWidth / 2, width - textWidth - 10);
                textX = Math.max(textX, 10);
                
                g2d.drawString(hrText, textX, currentY - 15);
            }
            
            g2d.dispose();
        }
    }
}
