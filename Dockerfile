# Multi-stage build for Chattrix API
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM quay.io/wildfly/wildfly:38.0.0.Final-jdk21

# Switch to root for setup
USER root

# Set environment variables
ENV WILDFLY_HOME=/opt/jboss/wildfly
ENV DEPLOYMENT_DIR=${WILDFLY_HOME}/standalone/deployments

# Default admin credentials (override via environment variables)
ENV WILDFLY_ADMIN_USER=admin
ENV WILDFLY_ADMIN_PASSWORD=admin123

# Download PostgreSQL JDBC driver as a module (not deployment)
RUN mkdir -p ${WILDFLY_HOME}/modules/org/postgresql/main && \
    curl -o ${WILDFLY_HOME}/modules/org/postgresql/main/postgresql-42.7.4.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.4.jar && \
    echo '<?xml version="1.0" encoding="UTF-8"?><module xmlns="urn:jboss:module:1.9" name="org.postgresql"><resources><resource-root path="postgresql-42.7.4.jar"/></resources><dependencies><module name="javax.api"/><module name="javax.transaction.api"/></dependencies></module>' \
    > ${WILDFLY_HOME}/modules/org/postgresql/main/module.xml

# Copy datasource configuration script
COPY configure-datasource.cli /opt/jboss/configure-datasource.cli

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Copy the WAR file from builder stage
COPY --from=builder /app/target/ROOT.war ${DEPLOYMENT_DIR}/ROOT.war

# Fix permissions
RUN chown -R jboss:jboss ${WILDFLY_HOME}

# Switch back to jboss user
USER jboss

# Expose ports
EXPOSE 8080 9990

# Start with entrypoint script
CMD ["/usr/local/bin/docker-entrypoint.sh"]
