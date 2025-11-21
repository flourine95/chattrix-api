#!/bin/bash

# Script để add contact nhanh
# Sử dụng: ./test-add-contact.sh <user_token> <contact_user_id> [nickname]

USER_TOKEN=$1
CONTACT_USER_ID=$2
NICKNAME=${3:-""}

if [ -z "$USER_TOKEN" ] || [ -z "$CONTACT_USER_ID" ]; then
    echo "❌ Thiếu tham số!"
    echo ""
    echo "Cách sử dụng:"
    echo "  ./test-add-contact.sh <user_token> <contact_user_id> [nickname]"
    echo ""
    echo "Ví dụ:"
    echo "  ./test-add-contact.sh eyJhbGc... 10"
    echo "  ./test-add-contact.sh eyJhbGc... 10 \"My Friend\""
    echo ""
    exit 1
fi

echo "👥 Đang thêm contact..."
echo "   Contact User ID: $CONTACT_USER_ID"
if [ -n "$NICKNAME" ]; then
    echo "   Nickname: $NICKNAME"
fi
echo ""

if [ -n "$NICKNAME" ]; then
    REQUEST_BODY="{\"contactUserId\": $CONTACT_USER_ID, \"nickname\": \"$NICKNAME\"}"
else
    REQUEST_BODY="{\"contactUserId\": $CONTACT_USER_ID}"
fi

RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY")

echo "📋 Response:"
echo "$RESPONSE" | jq .

# Check if successful
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')

if [ "$SUCCESS" = "true" ]; then
    echo ""
    echo "✅ Contact đã được thêm thành công!"
    echo ""
    echo "📞 Bây giờ bạn có thể gọi cho user này!"
else
    echo ""
    echo "❌ Không thể thêm contact. Kiểm tra lại token và user ID."
fi
