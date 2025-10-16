package com.heartsafe.desktop;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Demo Google Fit Service for HeartSafe
 * Simulates Google Fit integration for demonstration purposes
 * In production, this would connect to actual Google Fit API with OAuth2 authentication
 */
public class GoogleFitServiceDemo {
    private static final Logger LOGGER = Logger.getLogger(GoogleFitServiceDemo.class.getName());
    
    private boolean isInitialized = false;
    private Random random = new Random();
    
    /**
     * Initialize Google Fit service (Demo Mode)
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate initialization delay
                Thread.sleep(1000);
                isInitialized = true;
                LOGGER.info("Google Fit service initialized successfully (Demo Mode)");
                return true;
            } catch (Exception e) {
                LOGGER.warning("Google Fit service initialization failed: " + e.getMessage());
                isInitialized = false;
                return false;
            }
        });
    }
    
    /**
     * Get the latest heart rate reading (Demo)
     */
    public CompletableFuture<Integer> getLatestHeartRate() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                LOGGER.warning("Google Fit service not initialized");
                return null;
            }
            
            // Simulate realistic heart rate data
            int baseHR = 72;
            int variation = random.nextInt(20) - 10; // +/- 10 BPM variation
            int hr = Math.max(50, Math.min(140, baseHR + variation));
            
            LOGGER.info("Retrieved heart rate from Google Fit (Demo): " + hr + " BPM");
            return hr;
        });
    }
    
    /**
     * Get heart rate history for the specified time period (Demo)
     */
    public CompletableFuture<List<HeartRateReading>> getHeartRateHistory(LocalDateTime startTime, LocalDateTime endTime) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                return new ArrayList<>();
            }
            
            List<HeartRateReading> readings = new ArrayList<>();
            LocalDateTime current = startTime;
            
            // Generate sample data every 5 minutes
            while (current.isBefore(endTime)) {
                int hr = 70 + random.nextInt(25) + (int)(Math.sin(current.getMinute() * 0.1) * 10);
                readings.add(new HeartRateReading(current, hr));
                current = current.plusMinutes(5);
            }
            
            LOGGER.info("Retrieved " + readings.size() + " heart rate readings from Google Fit (Demo)");
            return readings;
        });
    }
    
    /**
     * Check if Google Fit service is available and initialized
     */
    public boolean isAvailable() {
        return isInitialized;
    }
    
    /**
     * Data class for heart rate readings
     */
    public static class HeartRateReading {
        private final LocalDateTime timestamp;
        private final int heartRate;
        
        public HeartRateReading(LocalDateTime timestamp, int heartRate) {
            this.timestamp = timestamp;
            this.heartRate = heartRate;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getHeartRate() { return heartRate; }
        
        @Override
        public String toString() {
            return String.format("HeartRate{time=%s, bpm=%d}", timestamp, heartRate);
        }
    }
}