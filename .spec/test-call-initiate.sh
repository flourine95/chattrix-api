#!/bin/bash

# Script để test call initiation nhanh
# Sử dụng: ./test-call-initiate.sh <caller_token> <callee_id> <call_type>

CALLER_TOKEN=$1
CALLEE_ID=$2
CALL_TYPE=${3:-VIDEO}  # Mặc định là VIDEO nếu không chỉ định

if [ -z "$CALLER_TOKEN" ] || [ -z "$CALLEE_ID" ]; then
    echo "❌ Thiếu tham số!"
    echo ""
    echo "Cách sử dụng:"
    echo "  ./test-call-initiate.sh <caller_token> <callee_id> [call_type]"
    echo ""
    echo "Ví dụ:"
    echo "  ./test-call-initiate.sh eyJhbGc... 10 VIDEO"
    echo "  ./test-call-initiate.sh eyJhbGc... 10 AUDIO"
    echo ""
    exit 1
fi

CHANNEL_ID="test-channel-$(date +%s)"

echo "📞 Đang tạo cuộc gọi..."
echo "   Callee ID: $CALLEE_ID"
echo "   Call Type: $CALL_TYPE"
echo "   Channel ID: $CHANNEL_ID"
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calls/initiate \
  -H "Authorization: Bearer $CALLER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"calleeId\": \"$CALLEE_ID\",
    \"callType\": \"$CALL_TYPE\",
    \"channelId\": \"$CHANNEL_ID\"
  }")

echo "📋 Response:"
echo "$RESPONSE" | jq .

# Extract call ID
CALL_ID=$(echo "$RESPONSE" | jq -r '.data.id // empty')

if [ -n "$CALL_ID" ]; then
    echo ""
    echo "✅ Cuộc gọi đã được tạo thành công!"
    echo "🆔 Call ID: $CALL_ID"
    echo ""
    echo "📱 Kiểm tra WebSocket test client của callee để xem call invitation!"
else
    echo ""
    echo "❌ Không thể tạo cuộc gọi. Kiểm tra lại token và callee ID."
fi
