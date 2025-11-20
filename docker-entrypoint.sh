#!/bin/bash
set -e

# Create WildFly admin user
if [ -n "$WILDFLY_ADMIN_USER" ] && [ -n "$WILDFLY_ADMIN_PASSWORD" ]; then
    echo "Creating WildFly admin user: $WILDFLY_ADMIN_USER"
    /opt/jboss/wildfly/bin/add-user.sh "$WILDFLY_ADMIN_USER" "$WILDFLY_ADMIN_PASSWORD" --silent
fi

# Configure PostgreSQL datasource if not already configured
DATASOURCE_MARKER="/opt/jboss/wildfly/standalone/configuration/.datasource-configured"
if [ ! -f "$DATASOURCE_MARKER" ]; then
    echo "Configuring PostgreSQL datasource..."
    /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/configure-datasource.cli
    touch "$DATASOURCE_MARKER"
    echo "Datasource configured successfully"
else
    echo "Datasource already configured, skipping..."
fi

echo "Starting WildFly..."
exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
