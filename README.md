# ❤️ HeartSafe - Comprehensive Heart Monitoring System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

HeartSafe is a professional, medical-grade heart monitoring system that provides real-time heart rate monitoring, automatic emergency alerts, comprehensive incident reporting, and teleconsultation booking capabilities.

## 🌟 Features

### 🔍 **Real-Time Heart Rate Monitoring**
- Integration with Google Fit API for real device data
- Simulated monitoring for demo/testing purposes
- Real-time chart visualization with smooth animations
- Color-coded heart rate status (Normal/Elevated/Critical)

### 🚨 **Automatic Emergency Response**
- **SMS Alerts**: Instant notifications to emergency contacts via Twilio
- **Emergency Detection**: Automatic triggers for abnormal heart rates
- **Manual Emergency**: Panic button for immediate alerts
- **Visual Alerts**: Screen flash and audio notifications

### 📊 **Professional Incident Reports**
- **PDF Generation**: Comprehensive 5-page medical reports
- **Statistical Analysis**: Heart rate trends and anomaly detection
- **Timeline Documentation**: Complete incident chronology
- **Medical Recommendations**: AI-generated follow-up suggestions

### 👩‍⚕️ **Teleconsultation System**
- **Doctor Booking**: Browse available specialists
- **Emergency Consultations**: 24/7 urgent medical access
- **Regular Appointments**: Scheduled consultations
- **Patient History**: Integration with monitoring data

### 🎨 **Modern Desktop UI**
- **Material Design**: Professional medical interface
- **Real-time Charts**: Interactive heart rate visualization
- **Configuration Management**: Easy service setup
- **Contact Management**: Emergency contacts with testing

## 🏗️ Architecture

```
HeartSafe/
├── desktop/          # Java Swing Desktop Application
├── backend/          # Spring Boot API Server
├── shared/           # Common Data Models
├── app/             # Android Mobile App (Future)
└── reports/         # Generated PDF Reports
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Windows/macOS/Linux** (tested on Windows 11)
- Internet connection for external API integrations

### 1. Clone and Build

```bash
# Clone the repository
git clone https://github.com/yourusername/HeartSafe.git
cd HeartSafe

# Build the project
mvn clean compile
```

### 2. Run the Desktop Application

```bash
# Run with Maven
mvn exec:java -pl desktop

# OR compile and run JAR
mvn package -pl desktop
java -jar desktop/target/heartsafe-desktop-0.1.0.jar
```

### 3. First Launch

1. The application will start in **Demo Mode** with simulated data
2. Click **⚙️ Settings** to configure external services
3. Click **📞 Contacts** to add emergency contacts
4. Click **▶️ Start Monitoring** to begin heart rate tracking

## ⚙️ Configuration

### 🔗 Google Fit Integration (Optional)

For real heart rate data from wearable devices:

1. **Create Google Cloud Project**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing
   - Enable the **Fitness API**

2. **Setup OAuth2 Credentials**
   - Go to APIs & Services > Credentials
   - Create **OAuth 2.0 Client ID** for Desktop application
   - Set authorized redirect URI: `http://localhost:8888`
   - Download credentials as `credentials.json`

3. **Configure Application**
   ```bash
   # Place credentials.json in project root
   cp /path/to/downloaded/credentials.json ./credentials.json
   ```

4. **First-time Authorization**
   - Application will open browser for Google authentication
   - Grant permissions for Fitness data access
   - Tokens are stored locally in `tokens/` directory

### 📱 SMS Emergency Alerts (Optional)

For real SMS notifications via Twilio:

1. **Create Twilio Account**
   - Sign up at [Twilio Console](https://console.twilio.com/)
   - Get Account SID, Auth Token, and Phone Number

2. **Set Environment Variables**
   ```bash
   # Windows PowerShell
   $env:TWILIO_ACCOUNT_SID = "your_account_sid"
   $env:TWILIO_AUTH_TOKEN = "your_auth_token"
   $env:TWILIO_PHONE_NUMBER = "+1234567890"

   # Windows Command Prompt
   set TWILIO_ACCOUNT_SID=your_account_sid
   set TWILIO_AUTH_TOKEN=your_auth_token
   set TWILIO_PHONE_NUMBER=+1234567890

   # Linux/macOS
   export TWILIO_ACCOUNT_SID="your_account_sid"
   export TWILIO_AUTH_TOKEN="your_auth_token"
   export TWILIO_PHONE_NUMBER="+1234567890"
   ```

3. **Alternative: System Properties**
   ```bash
   mvn exec:java -pl desktop -Dtwilio.account.sid=your_sid -Dtwilio.auth.token=your_token -Dtwilio.phone.number=+1234567890
   ```

### 🏥 Backend API (Optional)

For teleconsultation and data persistence:

1. **Start Backend Server**
   ```bash
   mvn spring-boot:run -pl backend
   ```

2. **Configure Backend URL**
   ```bash
   # Default: http://localhost:8081
   mvn exec:java -pl desktop -Dheartsafe.backend.url=http://your-server:port
   ```

## 📖 User Guide

### 🎮 Main Interface

1. **❤️ Heart Rate Display**: Shows current BPM with color coding
2. **📈 Real-time Chart**: Visualizes heart rate trends over time
3. **Control Buttons**:
   - **▶️ Start Monitoring**: Begin heart rate tracking
   - **⏹️ Stop Monitoring**: End current session
   - **🚨 EMERGENCY**: Trigger immediate alerts
   - **📞 Book Teleconsult**: Schedule doctor appointments

### 🔧 Settings Panel

- **Google Fit Status**: Integration status and setup instructions
- **SMS Service Status**: Twilio configuration status
- **Patient Information**: Name and location settings
- **Emergency Contacts**: Number of configured contacts

### 📞 Emergency Contacts Management

1. Click **📞 Contacts** button
2. **Add Contact**: Name, phone number, relationship
3. **Send Test SMS**: Verify contact configuration
4. Contacts receive formatted emergency alerts automatically

### 📄 Incident Reports

1. Click **📄 Reports** button
2. View list of generated PDF reports
3. **Open Report**: View detailed incident analysis
4. Reports are automatically generated after emergencies

### 👩‍⚕️ Teleconsultation Booking

1. Click **📞 Book Teleconsult** button
2. View available doctors with specialties and ratings
3. **Book Regular Consultation**: Schedule standard appointment
4. **Book Emergency Consultation**: Urgent medical access

## 🔥 Emergency Workflow

When abnormal heart rate is detected:

1. **🚨 Automatic Detection**: System identifies heart rate anomaly
2. **📱 SMS Alerts**: Instant notifications to all emergency contacts
3. **📄 PDF Report**: Comprehensive incident documentation generated
4. **🏥 Optional Teleconsult**: Automatic emergency doctor booking
5. **📋 Activity Log**: All actions logged with timestamps

### Emergency Triggers

- **High Heart Rate**: > 120 BPM
- **Low Heart Rate**: < 50 BPM  
- **Manual Emergency**: User presses emergency button
- **Device Disconnection**: Monitoring interruption

## 🧪 Demo Mode

HeartSafe runs in demo mode by default:

- **Simulated Heart Rate**: Realistic data generation
- **Mock SMS**: Logged messages instead of real SMS
- **Demo Doctors**: Sample teleconsultation providers
- **Test Reports**: PDF generation with sample data

All features are fully functional for testing and demonstration.

## 🐛 Troubleshooting

### Common Issues

**Issue**: Application won't start
```bash
# Check Java version
java -version
# Should be Java 21 or higher

# Check Maven
mvn -version
```

**Issue**: Google Fit authentication fails
```bash
# Verify credentials.json exists in project root
ls -la credentials.json

# Check OAuth redirect URI in Google Cloud Console
# Must be: http://localhost:8888
```

**Issue**: SMS not sending
```bash
# Verify Twilio environment variables
echo $TWILIO_ACCOUNT_SID
echo $TWILIO_AUTH_TOKEN
echo $TWILIO_PHONE_NUMBER

# Check Twilio account balance and phone number verification
```

**Issue**: PDF reports not generating
```bash
# Check reports directory permissions
ls -la reports/

# Check Java file write permissions
```

### Debug Mode

Run with verbose logging:
```bash
mvn exec:java -pl desktop -Djava.util.logging.level=ALL
```

## 📦 Building for Distribution

### Create Executable JAR

```bash
# Build with all dependencies
mvn clean package -pl desktop

# Run standalone JAR
java -jar desktop/target/heartsafe-desktop-0.1.0-jar-with-dependencies.jar
```

### Windows Installer

```bash
# Use jpackage (Java 14+)
jpackage --input desktop/target/ \
         --name HeartSafe \
         --main-jar heartsafe-desktop-0.1.0.jar \
         --main-class com.heartsafe.desktop.Main \
         --type msi \
         --win-shortcut \
         --win-menu
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **📧 Email**: support@heartsafe.app
- **🐛 Issues**: [GitHub Issues](https://github.com/yourusername/HeartSafe/issues)
- **💬 Discussions**: [GitHub Discussions](https://github.com/yourusername/HeartSafe/discussions)
- **📖 Wiki**: [Project Wiki](https://github.com/yourusername/HeartSafe/wiki)

## 🙏 Acknowledgments

- **Google Fit API** - Heart rate data integration
- **Twilio** - SMS emergency notifications  
- **Apache PDFBox** - PDF report generation
- **Material Design** - UI/UX inspiration
- **MPAndroidChart** - Chart visualization library

---

**⚠️ Medical Disclaimer**: HeartSafe is a monitoring tool and should not replace professional medical advice, diagnosis, or treatment. Always consult with qualified healthcare providers for medical concerns.

**🔒 Privacy**: All health data is processed locally. No data is transmitted without explicit user consent.
