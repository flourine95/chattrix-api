# Message Types - Sticker & Emoji Support

## Tổng Quan

Đã thêm 2 message types mới: **STICKER** và **EMOJI**

## Message Types Hiện Tại

```java
public enum MessageType {
    TEXT,       // Tin nhắn văn bản thường
    IMAGE,      // Hình ảnh
    VIDEO,      // Video
    VOICE,      // Tin nhắn thoại
    AUDIO,      // File audio
    DOCUMENT,   // File tài liệu
    LOCATION,   // Vị trí địa lý
    STICKER,    // ✨ MỚI - Sticker
    EMOJI,      // ✨ MỚI - Emoji (reaction lớn)
    SYSTEM      // Tin nhắn hệ thống
}
```

---

## Sử Dụng

### 1. Gửi Sticker Message

**Endpoint:** `POST /api/v1/conversations/{conversationId}/messages`

**Request:**
```json
{
  "content": "😊",
  "type": "STICKER",
  "mediaUrl": "https://cdn.example.com/stickers/happy.png",
  "thumbnailUrl": "https://cdn.example.com/stickers/happy_thumb.png"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 5,
    "content": "😊",
    "type": "STICKER",
    "mediaUrl": "https://cdn.example.com/stickers/happy.png",
    "thumbnailUrl": "https://cdn.example.com/stickers/happy_thumb.png",
    "sentAt": "2025-12-22T10:00:00Z"
  }
}
```

**Fields:**
- `content` - Text representation hoặc emoji (optional)
- `type` - **"STICKER"**
- `mediaUrl` - URL của sticker image (required)
- `thumbnailUrl` - URL thumbnail (optional)

---

### 2. Gửi Emoji Message

**Endpoint:** `POST /api/v1/conversations/{conversationId}/messages`

**Request:**
```json
{
  "content": "👍",
  "type": "EMOJI"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": 124,
    "conversationId": 1,
    "senderId": 5,
    "content": "👍",
    "type": "EMOJI",
    "sentAt": "2025-12-22T10:00:00Z"
  }
}
```

**Fields:**
- `content` - Emoji character (required)
- `type` - **"EMOJI"**

---

## Scheduled Messages với Sticker/Emoji

### Schedule Sticker Message

**Endpoint:** `POST /api/v1/conversations/{conversationId}/messages/schedule`

**Request:**
```json
{
  "content": "🎉",
  "type": "STICKER",
  "mediaUrl": "https://cdn.example.com/stickers/party.png",
  "scheduledTime": "2025-12-31T23:59:00Z"
}
```

### Schedule Emoji Message

**Endpoint:** `POST /api/v1/conversations/{conversationId}/messages/schedule`

**Request:**
```json
{
  "content": "🎊",
  "type": "EMOJI",
  "scheduledTime": "2025-12-31T23:59:00Z"
}
```

---

## Phân Biệt STICKER vs EMOJI

### STICKER
- **Mục đích:** Sticker pack, animated stickers, custom images
- **Có mediaUrl:** ✅ YES (required)
- **Content:** Optional (có thể là text representation)
- **Hiển thị:** Như một image lớn trong conversation
- **Example:** LINE stickers, Telegram stickers, custom sticker packs

### EMOJI
- **Mục đích:** Single emoji character, emoji reaction lớn
- **Có mediaUrl:** ❌ NO
- **Content:** Required (emoji character)
- **Hiển thị:** Emoji lớn, có thể animated
- **Example:** 👍, ❤️, 😂, 🎉

---

## Client Implementation

### Hiển Thị Sticker

```javascript
function renderMessage(message) {
  if (message.type === 'STICKER') {
    return `
      <div class="message sticker">
        <img 
          src="${message.mediaUrl}" 
          alt="${message.content || 'Sticker'}"
          class="sticker-image"
          loading="lazy"
        />
      </div>
    `;
  }
  
  // ... other types
}
```

**CSS:**
```css
.message.sticker {
  background: transparent;
  padding: 4px;
}

.sticker-image {
  width: 150px;
  height: 150px;
  object-fit: contain;
}
```

### Hiển Thị Emoji

```javascript
function renderMessage(message) {
  if (message.type === 'EMOJI') {
    return `
      <div class="message emoji">
        <span class="emoji-large">${message.content}</span>
      </div>
    `;
  }
  
  // ... other types
}
```

**CSS:**
```css
.message.emoji {
  background: transparent;
  padding: 8px;
}

.emoji-large {
  font-size: 64px;
  line-height: 1;
}
```

---

## Validation

### Backend Validation

**Sticker:**
- ✅ `type` = "STICKER"
- ✅ `mediaUrl` phải có (required)
- ✅ `content` optional

**Emoji:**
- ✅ `type` = "EMOJI"
- ✅ `content` phải có (required)
- ✅ `content` nên là emoji character

### Client Validation

```javascript
function validateStickerMessage(data) {
  if (data.type === 'STICKER') {
    if (!data.mediaUrl) {
      throw new Error('Sticker message requires mediaUrl');
    }
  }
  
  if (data.type === 'EMOJI') {
    if (!data.content) {
      throw new Error('Emoji message requires content');
    }
    
    // Optional: Validate emoji character
    if (!isEmoji(data.content)) {
      throw new Error('Content must be a valid emoji');
    }
  }
}

function isEmoji(str) {
  const emojiRegex = /\p{Emoji}/u;
  return emojiRegex.test(str);
}
```

---

## Examples

### Example 1: Gửi Sticker Pack

```javascript
const stickerPacks = {
  happy: {
    url: 'https://cdn.example.com/stickers/happy.png',
    emoji: '😊'
  },
  love: {
    url: 'https://cdn.example.com/stickers/love.png',
    emoji: '❤️'
  }
};

async function sendSticker(conversationId, stickerKey) {
  const sticker = stickerPacks[stickerKey];
  
  const response = await fetch(
    `${API_BASE}/v1/conversations/${conversationId}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        content: sticker.emoji,
        type: 'STICKER',
        mediaUrl: sticker.url
      })
    }
  );
  
  return await response.json();
}

// Usage
await sendSticker(1, 'happy');
```

### Example 2: Gửi Emoji Lớn

```javascript
async function sendBigEmoji(conversationId, emoji) {
  const response = await fetch(
    `${API_BASE}/v1/conversations/${conversationId}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        content: emoji,
        type: 'EMOJI'
      })
    }
  );
  
  return await response.json();
}

// Usage
await sendBigEmoji(1, '👍');
await sendBigEmoji(1, '❤️');
await sendBigEmoji(1, '🎉');
```

### Example 3: Schedule Sticker

```javascript
async function scheduleSticker(conversationId, stickerUrl, scheduledTime) {
  const response = await fetch(
    `${API_BASE}/v1/conversations/${conversationId}/messages/schedule`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        content: '🎉',
        type: 'STICKER',
        mediaUrl: stickerUrl,
        scheduledTime: scheduledTime
      })
    }
  );
  
  return await response.json();
}

// Schedule New Year sticker
const newYearTime = new Date('2025-12-31T23:59:00Z').toISOString();
await scheduleSticker(1, 'https://cdn.example.com/stickers/newyear.png', newYearTime);
```

---

## UI/UX Recommendations

### Sticker Picker

```javascript
class StickerPicker {
  constructor() {
    this.packs = this.loadStickerPacks();
  }
  
  render() {
    return `
      <div class="sticker-picker">
        <div class="sticker-packs">
          ${this.packs.map(pack => `
            <div class="sticker-pack" data-pack-id="${pack.id}">
              <h3>${pack.name}</h3>
              <div class="stickers">
                ${pack.stickers.map(sticker => `
                  <img 
                    src="${sticker.thumbnailUrl}" 
                    data-url="${sticker.url}"
                    class="sticker-item"
                    onclick="sendSticker('${sticker.url}')"
                  />
                `).join('')}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }
}
```

### Emoji Picker

```javascript
class EmojiPicker {
  constructor() {
    this.emojis = ['👍', '❤️', '😂', '😊', '🎉', '🔥', '👏', '💯'];
  }
  
  render() {
    return `
      <div class="emoji-picker">
        ${this.emojis.map(emoji => `
          <button 
            class="emoji-button"
            onclick="sendEmoji('${emoji}')"
          >
            ${emoji}
          </button>
        `).join('')}
      </div>
    `;
  }
}
```

---

## Database Schema

**Không cần thay đổi schema!** 

Enum `MessageType` được lưu dưới dạng String trong database:
- Trước: `TEXT`, `IMAGE`, `VIDEO`, etc.
- Sau: `TEXT`, `IMAGE`, `VIDEO`, `STICKER`, `EMOJI`, etc.

PostgreSQL sẽ tự động accept các giá trị mới.

---

## Testing

### Test 1: Send Sticker

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "😊",
    "type": "STICKER",
    "mediaUrl": "https://example.com/sticker.png"
  }'
```

### Test 2: Send Emoji

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "👍",
    "type": "EMOJI"
  }'
```

### Test 3: Schedule Sticker

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "🎉",
    "type": "STICKER",
    "mediaUrl": "https://example.com/party.png",
    "scheduledTime": "2025-12-31T23:59:00Z"
  }'
```

---

## Summary

### Changes Made

✅ Added `STICKER` to `MessageType` enum
✅ Added `EMOJI` to `MessageType` enum
✅ No database migration needed
✅ Backward compatible with existing messages

### Usage

**STICKER:**
- Use for sticker packs, animated stickers
- Requires `mediaUrl`
- Optional `content`

**EMOJI:**
- Use for single emoji reactions
- Requires `content` (emoji character)
- No `mediaUrl` needed

### Status

✅ **DEPLOYED** - Application rebuilt successfully
✅ **READY** - Can start using STICKER and EMOJI types immediately
