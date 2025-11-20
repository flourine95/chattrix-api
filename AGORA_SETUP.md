# Agora Video/Audio Call API Setup Guide

This document provides instructions for setting up the Agora video/audio call functionality in the Chattrix API.

## Prerequisites

1. **Agora Account**: Sign up at [https://console.agora.io/](https://console.agora.io/)
2. **Agora Project**: Create a new project in the Agora Console
3. **App ID and Certificate**: Obtain your App ID and App Certificate from the project settings

## Dependencies Added

The following dependencies have been added to `pom.xml`:

### 1. Agora Authentication SDK
```xml
<dependency>
    <groupId>io.agora</groupId>
    <artifactId>authentication</artifactId>
    <version>2.0.0</version>
</dependency>
```
Used for generating secure RTC tokens for channel access.

### 2. Flyway Database Migrations
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.21.0</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>10.21.0</version>
</dependency>
```
Used for managing database schema migrations for call-related tables.

### 3. JUnit QuickCheck (Property-Based Testing)
```xml
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```
Used for property-based testing to verify correctness properties.

## Configuration Classes

### AgoraConfig
Located at: `src/main/java/com/chattrix/api/config/AgoraConfig.java`

Manages Agora-specific configuration:
- App ID
- App Certificate
- Token expiration settings (default, min, max)

### CallConfig
Located at: `src/main/java/com/chattrix/api/config/CallConfig.java`

Manages call-related configuration:
- Call timeout duration
- Maximum call duration

## Environment Variables

### Required Variables

These **MUST** be set before starting the application:

| Variable | Description | Example |
|----------|-------------|---------|
| `AGORA_APP_ID` | Your Agora App ID | `abc123def456` |
| `AGORA_APP_CERTIFICATE` | Your Agora App Certificate | `xyz789uvw012` |

### Optional Variables

These have default values but can be customized:

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `AGORA_DEFAULT_TOKEN_EXPIRATION` | Default token expiration (seconds) | 3600 (1 hour) | `3600` |
| `AGORA_MAX_TOKEN_EXPIRATION` | Maximum token expiration (seconds) | 86400 (24 hours) | `86400` |
| `AGORA_MIN_TOKEN_EXPIRATION` | Minimum token expiration (seconds) | 60 (1 minute) | `60` |
| `CALL_TIMEOUT_SECONDS` | Call timeout before marked as missed | 60 | `60` |
| `CALL_MAX_DURATION_SECONDS` | Maximum call duration | 14400 (4 hours) | `14400` |

## Setup Instructions

### 1. Get Agora Credentials

1. Go to [Agora Console](https://console.agora.io/)
2. Create a new project or select an existing one
3. Enable "App Certificate" in project settings
4. Copy your **App ID** and **App Certificate**

### 2. Set Environment Variables

#### Option A: System Environment Variables (Windows)
```cmd
setx AGORA_APP_ID "your_app_id_here"
setx AGORA_APP_CERTIFICATE "your_app_certificate_here"
```

#### Option B: IDE Configuration (IntelliJ IDEA)
1. Go to Run → Edit Configurations
2. Select your WildFly configuration
3. Add environment variables in the "Environment variables" field

#### Option C: WildFly Configuration
Add to `standalone.xml` or `standalone-full.xml`:
```xml
<system-properties>
    <property name="AGORA_APP_ID" value="your_app_id_here"/>
    <property name="AGORA_APP_CERTIFICATE" value="your_app_certificate_here"/>
</system-properties>
```

### 3. Create .env File (Optional)

Copy `.env.example` to `.env` and fill in your values:
```bash
cp .env.example .env
```

Edit `.env` with your actual credentials.

**Important**: Never commit `.env` to version control!

### 4. Verify Setup

Build the project to verify all dependencies are resolved:
```bash
mvn clean compile
```

If successful, you should see:
```
[INFO] BUILD SUCCESS
```

## Troubleshooting

### Application Fails to Start

**Error**: `IllegalStateException: AGORA_APP_ID environment variable is required but not set`

**Solution**: Ensure all required environment variables are set before starting the application.

### Token Generation Fails

**Error**: `TOKEN_GENERATION_FAILED`

**Possible causes**:
1. Invalid App ID or App Certificate
2. App Certificate not enabled in Agora Console
3. Network connectivity issues

**Solution**: Verify your credentials in the Agora Console and ensure they match your environment variables.

### Dependency Resolution Issues

**Error**: `Could not resolve dependencies`

**Solution**: 
1. Check your internet connection
2. Clear Maven cache: `mvn dependency:purge-local-repository`
3. Update Maven: `mvn -U clean install`

## Next Steps

After completing the setup:

1. **Database Migrations**: Run Flyway migrations to create call-related tables (Task 2)
2. **Token Service**: Implement the AgoraTokenService (Task 5)
3. **Call Service**: Implement call lifecycle management (Task 8)

## Security Notes

- **Never** commit App ID or App Certificate to version control
- Use environment variables or secure secret management systems
- Rotate App Certificate periodically
- Monitor token generation logs for suspicious activity
- Implement rate limiting on token generation endpoints (configured in Task 15)

## References

- [Agora Documentation](https://docs.agora.io/)
- [Agora Token Server](https://docs.agora.io/en/video-calling/develop/authentication-workflow)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [JUnit QuickCheck](https://pholser.github.io/junit-quickcheck/)
