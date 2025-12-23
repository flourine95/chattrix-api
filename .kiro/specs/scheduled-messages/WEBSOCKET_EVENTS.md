# WebSocket Events cho Scheduled Messages

## Tổng Quan

Khi scheduled message được gửi tự động, backend sẽ gửi **3 WebSocket events** để client có thể cập nhật UI real-time:

1. **`chat.message`** - Tin nhắn xuất hiện trong conversation (giống tin nhắn thường)
2. **`scheduled.message.sent`** - Thông báo scheduled message đã được gửi thành công
3. **`conversation.update`** - Cập nhật lastMessage trong conversation list

## Event 1: chat.message

**Mục đích:** Hiển thị tin nhắn trong conversation real-time (giống tin nhắn thường)

**Event Type:** `chat.message`

**Payload:**
```json
{
  "type": "chat.message",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 5,
    "senderUsername": "user1",
    "senderFullName": "John Doe",
    "content": "Chúc mừng năm mới!",
    "type": "TEXT",
    "sentAt": "2025-12-31T23:59:00.123Z",
    "createdAt": "2025-12-22T10:00:00Z",
    "updatedAt": "2025-12-31T23:59:00.123Z",
    "scheduled": true,
    "scheduledTime": "2025-12-31T23:59:00Z",
    "scheduledStatus": "SENT",
    "reactions": {},
    "edited": false,
    "deleted": false,
    "forwarded": false,
    "forwardCount": 0,
    "readCount": 0
  }
}
```

**Client Action:**
```javascript
websocket.on('chat.message', (data) => {
  // Thêm tin nhắn vào conversation
  addMessageToConversation(data.conversationId, data);
  
  // Scroll to bottom nếu user đang ở cuối conversation
  if (isAtBottom()) {
    scrollToBottom();
  }
  
  // Play notification sound (nếu không phải tin nhắn của mình)
  if (data.senderId !== currentUserId) {
    playNotificationSound();
  }
});
```

---

## Event 2: scheduled.message.sent

**Mục đích:** Thông báo scheduled message đã được gửi thành công

**Event Type:** `scheduled.message.sent`

**Payload:**
```json
{
  "type": "scheduled.message.sent",
  "data": {
    "scheduledMessageId": 123,
    "message": {
      "id": 123,
      "conversationId": 1,
      "senderId": 5,
      "content": "Chúc mừng năm mới!",
      "sentAt": "2025-12-31T23:59:00.123Z",
      "scheduled": true,
      "scheduledTime": "2025-12-31T23:59:00Z",
      "scheduledStatus": "SENT"
    }
  }
}
```

**Client Action:**
```javascript
websocket.on('scheduled.message.sent', (data) => {
  // Update scheduled message status trong scheduled messages list
  updateScheduledMessageStatus(data.scheduledMessageId, 'SENT');
  
  // Hiển thị notification
  showNotification({
    title: 'Tin nhắn đã được gửi',
    message: `Tin nhắn "${data.message.content}" đã được gửi thành công`,
    type: 'success'
  });
  
  // Nếu đang xem scheduled messages list, refresh
  if (isOnScheduledMessagesPage()) {
    refreshScheduledMessagesList();
  }
});
```

---

## Event 3: conversation.update

**Mục đích:** Cập nhật lastMessage trong conversation list

**Event Type:** `conversation.update`

**Payload:**
```json
{
  "type": "conversation.update",
  "data": {
    "conversationId": 1,
    "updatedAt": "2025-12-31T23:59:00.123Z",
    "lastMessage": {
      "id": 123,
      "content": "Chúc mừng năm mới!",
      "senderId": 5,
      "senderUsername": "user1",
      "sentAt": "2025-12-31T23:59:00.123Z",
      "type": "TEXT"
    }
  }
}
```

**Client Action:**
```javascript
websocket.on('conversation.update', (data) => {
  // Update lastMessage trong conversation list
  updateConversationLastMessage(data.conversationId, data.lastMessage);
  
  // Update updatedAt để sort conversation list
  updateConversationTimestamp(data.conversationId, data.updatedAt);
  
  // Re-sort conversation list (mới nhất lên đầu)
  sortConversationList();
  
  // Increment unread count (nếu không phải tin nhắn của mình)
  if (data.lastMessage.senderId !== currentUserId) {
    incrementUnreadCount(data.conversationId);
  }
});
```

---

## Event 4: scheduled.message.failed

**Mục đích:** Thông báo scheduled message gửi thất bại

**Event Type:** `scheduled.message.failed`

**Payload:**
```json
{
  "type": "scheduled.message.failed",
  "data": {
    "scheduledMessageId": 123,
    "conversationId": 1,
    "failedReason": "User has left the conversation",
    "failedAt": "2025-12-31T23:59:00.123Z"
  }
}
```

**Client Action:**
```javascript
websocket.on('scheduled.message.failed', (data) => {
  // Update scheduled message status
  updateScheduledMessageStatus(data.scheduledMessageId, 'FAILED', data.failedReason);
  
  // Hiển thị error notification
  showNotification({
    title: 'Gửi tin nhắn thất bại',
    message: `Lý do: ${data.failedReason}`,
    type: 'error'
  });
  
  // Nếu đang xem scheduled messages list, refresh
  if (isOnScheduledMessagesPage()) {
    refreshScheduledMessagesList();
  }
});
```

---

## Complete Example: Client Implementation

```javascript
class ScheduledMessageHandler {
  constructor(websocket, currentUserId) {
    this.websocket = websocket;
    this.currentUserId = currentUserId;
    this.setupListeners();
  }

  setupListeners() {
    // Event 1: Tin nhắn xuất hiện trong conversation
    this.websocket.on('chat.message', (data) => {
      this.handleChatMessage(data);
    });

    // Event 2: Scheduled message đã được gửi
    this.websocket.on('scheduled.message.sent', (data) => {
      this.handleScheduledMessageSent(data);
    });

    // Event 3: Conversation được update
    this.websocket.on('conversation.update', (data) => {
      this.handleConversationUpdate(data);
    });

    // Event 4: Scheduled message gửi thất bại
    this.websocket.on('scheduled.message.failed', (data) => {
      this.handleScheduledMessageFailed(data);
    });
  }

  handleChatMessage(data) {
    console.log('New message received:', data);
    
    // Thêm tin nhắn vào conversation
    const conversationView = document.querySelector(`[data-conversation-id="${data.conversationId}"]`);
    if (conversationView) {
      this.addMessageToView(conversationView, data);
      
      // Scroll to bottom nếu user đang ở cuối
      if (this.isAtBottom(conversationView)) {
        this.scrollToBottom(conversationView);
      }
    }
    
    // Play sound nếu không phải tin nhắn của mình
    if (data.senderId !== this.currentUserId) {
      this.playNotificationSound();
    }
    
    // Hiển thị badge nếu scheduled message
    if (data.scheduled && data.scheduledStatus === 'SENT') {
      this.showScheduledBadge(data.id);
    }
  }

  handleScheduledMessageSent(data) {
    console.log('Scheduled message sent:', data);
    
    // Update status trong scheduled messages list
    const scheduledMessageElement = document.querySelector(`[data-scheduled-id="${data.scheduledMessageId}"]`);
    if (scheduledMessageElement) {
      scheduledMessageElement.dataset.status = 'SENT';
      scheduledMessageElement.querySelector('.status-badge').textContent = 'Đã gửi';
      scheduledMessageElement.querySelector('.status-badge').className = 'status-badge sent';
    }
    
    // Hiển thị toast notification
    this.showToast({
      title: 'Tin nhắn đã được gửi',
      message: `"${this.truncate(data.message.content, 50)}" đã được gửi thành công`,
      type: 'success',
      duration: 3000
    });
  }

  handleConversationUpdate(data) {
    console.log('Conversation updated:', data);
    
    // Update lastMessage trong conversation list
    const conversationItem = document.querySelector(`[data-conversation-id="${data.conversationId}"]`);
    if (conversationItem) {
      // Update last message preview
      const lastMessagePreview = conversationItem.querySelector('.last-message-preview');
      if (lastMessagePreview && data.lastMessage) {
        lastMessagePreview.textContent = data.lastMessage.content;
      }
      
      // Update timestamp
      const timestamp = conversationItem.querySelector('.timestamp');
      if (timestamp) {
        timestamp.textContent = this.formatTimestamp(data.updatedAt);
      }
      
      // Update unread count (nếu không phải tin nhắn của mình)
      if (data.lastMessage && data.lastMessage.senderId !== this.currentUserId) {
        this.incrementUnreadCount(data.conversationId);
      }
      
      // Move conversation to top
      this.moveConversationToTop(conversationItem);
    }
  }

  handleScheduledMessageFailed(data) {
    console.error('Scheduled message failed:', data);
    
    // Update status trong scheduled messages list
    const scheduledMessageElement = document.querySelector(`[data-scheduled-id="${data.scheduledMessageId}"]`);
    if (scheduledMessageElement) {
      scheduledMessageElement.dataset.status = 'FAILED';
      scheduledMessageElement.querySelector('.status-badge').textContent = 'Thất bại';
      scheduledMessageElement.querySelector('.status-badge').className = 'status-badge failed';
      
      // Hiển thị failed reason
      const failedReasonElement = scheduledMessageElement.querySelector('.failed-reason');
      if (failedReasonElement) {
        failedReasonElement.textContent = data.failedReason;
        failedReasonElement.style.display = 'block';
      }
    }
    
    // Hiển thị error toast
    this.showToast({
      title: 'Gửi tin nhắn thất bại',
      message: data.failedReason,
      type: 'error',
      duration: 5000
    });
  }

  // Helper methods
  addMessageToView(conversationView, message) {
    const messageElement = this.createMessageElement(message);
    conversationView.querySelector('.messages-container').appendChild(messageElement);
  }

  createMessageElement(message) {
    const div = document.createElement('div');
    div.className = `message ${message.senderId === this.currentUserId ? 'sent' : 'received'}`;
    div.dataset.messageId = message.id;
    
    let badges = '';
    if (message.scheduled && message.scheduledStatus === 'SENT') {
      badges = '<span class="badge scheduled">📅 Tin nhắn tự động</span>';
    }
    
    div.innerHTML = `
      <div class="message-content">
        ${badges}
        <p>${this.escapeHtml(message.content)}</p>
        <span class="timestamp">${this.formatTimestamp(message.sentAt)}</span>
      </div>
    `;
    
    return div;
  }

  isAtBottom(container) {
    const threshold = 100;
    return container.scrollHeight - container.scrollTop - container.clientHeight < threshold;
  }

  scrollToBottom(container) {
    container.scrollTop = container.scrollHeight;
  }

  playNotificationSound() {
    const audio = new Audio('/sounds/notification.mp3');
    audio.play().catch(e => console.log('Could not play sound:', e));
  }

  showToast({ title, message, type, duration }) {
    // Implementation depends on your toast library
    console.log(`[${type.toUpperCase()}] ${title}: ${message}`);
  }

  incrementUnreadCount(conversationId) {
    const badge = document.querySelector(`[data-conversation-id="${conversationId}"] .unread-badge`);
    if (badge) {
      const current = parseInt(badge.textContent) || 0;
      badge.textContent = current + 1;
      badge.style.display = 'block';
    }
  }

  moveConversationToTop(conversationItem) {
    const parent = conversationItem.parentElement;
    parent.insertBefore(conversationItem, parent.firstChild);
  }

  formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return 'Vừa xong';
    if (diff < 3600000) return `${Math.floor(diff / 60000)} phút trước`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} giờ trước`;
    return date.toLocaleDateString('vi-VN');
  }

  truncate(text, maxLength) {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

// Usage
const websocket = new WebSocket('ws://localhost:8080/ws/chat');
const handler = new ScheduledMessageHandler(websocket, currentUserId);
```

---

## Testing WebSocket Events

### Test 1: Tạo scheduled message với thời gian gần
```bash
# Tạo scheduled message gửi sau 1 phút
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Test scheduled message",
    "type": "TEXT",
    "scheduledTime": "'$(date -u -d '+1 minute' +%Y-%m-%dT%H:%M:%SZ)'"
  }'
```

### Test 2: Listen WebSocket events
```javascript
// Open browser console và connect WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/chat');

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received event:', data.type, data);
};

// Sau 1 phút, bạn sẽ thấy 3 events:
// 1. chat.message
// 2. scheduled.message.sent
// 3. conversation.update
```

### Test 3: Verify trong UI
1. Mở conversation trong UI
2. Tạo scheduled message với thời gian gần (1-2 phút)
3. Đợi đến thời gian scheduled
4. Verify:
   - ✅ Tin nhắn xuất hiện trong conversation
   - ✅ Toast notification hiển thị
   - ✅ Conversation list được update
   - ✅ Unread count tăng (nếu không phải sender)

---

## Summary

Khi scheduled message được gửi tự động, client sẽ nhận **3 events**:

1. **`chat.message`** → Tin nhắn xuất hiện trong conversation
2. **`scheduled.message.sent`** → Notification thành công
3. **`conversation.update`** → Update conversation list

Client cần listen cả 3 events để có trải nghiệm real-time hoàn chỉnh.

**Lưu ý:** Nếu gửi thất bại, chỉ có event `scheduled.message.failed` được gửi (không có `chat.message` và `conversation.update`).
