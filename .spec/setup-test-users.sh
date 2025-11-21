#!/bin/bash

# Script để setup 2 users để test call
# Sử dụng: ./setup-test-users.sh

echo "🚀 Setting up test users for call testing..."
echo ""

# Prompt for credentials
read -p "Enter User 1 username (default: long): " USER1_USERNAME
USER1_USERNAME=${USER1_USERNAME:-long}

read -sp "Enter User 1 password: " USER1_PASSWORD
echo ""

read -p "Enter User 2 username (default: phong): " USER2_USERNAME
USER2_USERNAME=${USER2_USERNAME:-phong}

read -sp "Enter User 2 password: " USER2_PASSWORD
echo ""
echo ""

# Login User 1
echo "🔐 Logging in User 1 ($USER1_USERNAME)..."
USER1_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER1_USERNAME\",\"password\":\"$USER1_PASSWORD\"}")

USER1_TOKEN=$(echo "$USER1_RESPONSE" | jq -r '.data.accessToken // empty')
USER1_ID=$(echo "$USER1_RESPONSE" | jq -r '.data.user.id // empty')

if [ -z "$USER1_TOKEN" ]; then
    echo "❌ Failed to login User 1"
    echo "$USER1_RESPONSE" | jq .
    exit 1
fi

echo "✅ User 1 logged in (ID: $USER1_ID)"
echo ""

# Login User 2
echo "🔐 Logging in User 2 ($USER2_USERNAME)..."
USER2_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER2_USERNAME\",\"password\":\"$USER2_PASSWORD\"}")

USER2_TOKEN=$(echo "$USER2_RESPONSE" | jq -r '.data.accessToken // empty')
USER2_ID=$(echo "$USER2_RESPONSE" | jq -r '.data.user.id // empty')

if [ -z "$USER2_TOKEN" ]; then
    echo "❌ Failed to login User 2"
    echo "$USER2_RESPONSE" | jq .
    exit 1
fi

echo "✅ User 2 logged in (ID: $USER2_ID)"
echo ""

# Add contacts (both directions)
echo "👥 Adding contacts..."

# User 1 add User 2
echo "   User 1 → User 2..."
CONTACT1_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"contactUserId\": $USER2_ID}")

CONTACT1_SUCCESS=$(echo "$CONTACT1_RESPONSE" | jq -r '.success // false')

if [ "$CONTACT1_SUCCESS" = "true" ]; then
    echo "   ✅ User 1 added User 2 as contact"
else
    echo "   ⚠️  User 1 → User 2 contact may already exist"
fi

# User 2 add User 1
echo "   User 2 → User 1..."
CONTACT2_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer $USER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"contactUserId\": $USER1_ID}")

CONTACT2_SUCCESS=$(echo "$CONTACT2_RESPONSE" | jq -r '.success // false')

if [ "$CONTACT2_SUCCESS" = "true" ]; then
    echo "   ✅ User 2 added User 1 as contact"
else
    echo "   ⚠️  User 2 → User 1 contact may already exist"
fi

echo ""

# Create or get conversation
echo "💬 Setting up conversation..."
CONV_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"DIRECT\",\"participantIds\":[$USER1_ID,$USER2_ID]}")

CONV_ID=$(echo "$CONV_RESPONSE" | jq -r '.data.id // empty')

if [ -n "$CONV_ID" ]; then
    echo "✅ Conversation created (ID: $CONV_ID)"
else
    # Try to get existing conversation
    CONVS_RESPONSE=$(curl -s -X GET http://localhost:8080/api/v1/conversations \
      -H "Authorization: Bearer $USER1_TOKEN")
    
    CONV_ID=$(echo "$CONVS_RESPONSE" | jq -r ".data[] | select(.participants | length == 2) | select(.participants[].userId == $USER2_ID) | .id" | head -1)
    
    if [ -n "$CONV_ID" ]; then
        echo "✅ Using existing conversation (ID: $CONV_ID)"
    else
        echo "⚠️  Could not create or find conversation"
    fi
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 Test Configuration:"
echo ""
echo "User 1 ($USER1_USERNAME):"
echo "  ID: $USER1_ID"
echo "  Token: $USER1_TOKEN"
echo ""
echo "User 2 ($USER2_USERNAME):"
echo "  ID: $USER2_ID"
echo "  Token: $USER2_TOKEN"
echo ""
if [ -n "$CONV_ID" ]; then
    echo "Conversation ID: $CONV_ID"
    echo ""
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🎯 Next Steps:"
echo ""
echo "1. Open websocket-test-client.html in browser"
echo "2. Paste tokens into User 1 and User 2 panels"
if [ -n "$CONV_ID" ]; then
    echo "3. Enter Conversation ID: $CONV_ID"
fi
echo "4. Click 'Connect' for both users"
echo "5. Test call by running:"
echo ""
echo "   ./test-call-initiate.sh $USER1_TOKEN $USER2_ID VIDEO"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
