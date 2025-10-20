# Hướng dẫn Test Chattrix API với Postman

## 📋 Mục lục
1. [Setup môi trường](#setup-môi-trường)
2. [Flow 1: Test REST API - Tạo 2 user nhắn tin với nhau](#flow-1-rest-api---2-users-direct-chat)
3. [Flow 2: Test REST API - Tạo nhóm chat nhiều người](#flow-2-rest-api---group-chat)
4. [Flow 3: Test WebSocket - Real-time messaging](#flow-3-websocket---real-time-messaging)
5. [Import Postman Collection](#import-postman-collection)

---

## Setup môi trường

**Base URL:** `http://localhost:8080/api` (hoặc URL server của bạn)

### Tạo Environment trong Postman:
- `baseUrl`: `http://localhost:8080/api`
- `user1Token`: (sẽ được set tự động sau khi login)
- `user2Token`: (sẽ được set tự động sau khi login)
- `user1Id`: (sẽ được set tự động)
- `user2Id`: (sẽ được set tự động)
- `conversationId`: (sẽ được set tự động)

---

## Flow 1: REST API - 2 Users Direct Chat

### Bước 1: Đăng ký User 1

**Request:**
```http
POST {{baseUrl}}/v1/auth/register
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Password123!",
  "fullName": "Alice Johnson"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "data": null,
  "errorCode": null
}
```

**Postman Tests Script:**
```javascript
// Không cần lưu gì vì chưa có token
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});
```

---

### Bước 2: Verify Email User 1

**Note:** Nếu bạn không có email server, bạn có thể:
- Check logs của server để lấy verification token
- Hoặc bỏ qua email verification bằng cách update trực tiếp database

**Option 1 - Dùng verification token từ logs:**
```http
POST {{baseUrl}}/v1/auth/verify-email
Content-Type: application/json

{
  "token": "verification-token-from-email-or-logs"
}
```

**Option 2 - Update database trực tiếp (Development only):**
```sql
UPDATE users SET is_verified = true WHERE email = 'alice@example.com';
```

---

### Bước 3: Login User 1

**Request:**
```http
POST {{baseUrl}}/v1/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "Password123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "alice",
      "email": "alice@example.com",
      "fullName": "Alice Johnson",
      "isVerified": true
    }
  },
  "errorCode": null
}
```

**Postman Tests Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

var jsonData = pm.response.json();
if (jsonData.success && jsonData.data) {
    pm.environment.set("user1Token", jsonData.data.accessToken);
    pm.environment.set("user1Id", jsonData.data.user.id);
    console.log("User 1 Token saved:", jsonData.data.accessToken);
    console.log("User 1 ID:", jsonData.data.user.id);
}
```

---

### Bước 4: Đăng ký User 2

**Request:**
```http
POST {{baseUrl}}/v1/auth/register
Content-Type: application/json

{
  "username": "bob",
  "email": "bob@example.com",
  "password": "Password123!",
  "fullName": "Bob Smith"
}
```

---

### Bước 5: Verify Email User 2

Tương tự như User 1.

---

### Bước 6: Login User 2

**Request:**
```http
POST {{baseUrl}}/v1/auth/login
Content-Type: application/json

{
  "username": "bob",
  "password": "Password123!"
}
```

**Postman Tests Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

var jsonData = pm.response.json();
if (jsonData.success && jsonData.data) {
    pm.environment.set("user2Token", jsonData.data.accessToken);
    pm.environment.set("user2Id", jsonData.data.user.id);
    console.log("User 2 Token saved:", jsonData.data.accessToken);
    console.log("User 2 ID:", jsonData.data.user.id);
}
```

---

### Bước 7: User 1 thêm User 2 vào Contacts (Optional)

**Request:**
```http
POST {{baseUrl}}/v1/contacts
Authorization: Bearer {{user1Token}}
Content-Type: application/json

{
  "contactUserId": {{user2Id}},
  "nickname": "Bobby"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Contact added successfully",
  "data": {
    "id": 1,
    "contactUser": {
      "id": 2,
      "username": "bob",
      "fullName": "Bob Smith"
    },
    "nickname": "Bobby",
    "isFavorite": false,
    "createdAt": "2025-10-16T10:30:00Z"
  }
}
```

---

### Bước 8: User 1 tạo Conversation với User 2

**Request:**
```http
POST {{baseUrl}}/v1/conversations
Authorization: Bearer {{user1Token}}
Content-Type: application/json

{
  "type": "DIRECT",
  "participantIds": [{{user2Id}}]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": 1,
    "name": null,
    "type": "DIRECT",
    "participants": [
      {
        "userId": 1,
        "username": "alice",
        "fullName": "Alice Johnson",
        "role": "ADMIN"
      },
      {
        "userId": 2,
        "username": "bob",
        "fullName": "Bob Smith",
        "role": "MEMBER"
      }
    ],
    "createdAt": "2025-10-16T10:35:00Z"
  }
}
```

**Postman Tests Script:**
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

var jsonData = pm.response.json();
if (jsonData.success && jsonData.data) {
    pm.environment.set("conversationId", jsonData.data.id);
    console.log("Conversation ID saved:", jsonData.data.id);
}
```

---

### Bước 9: User 1 gửi message

**Request:**
```http
POST {{baseUrl}}/v1/conversations/{{conversationId}}/messages
Authorization: Bearer {{user1Token}}
Content-Type: application/json

{
  "content": "Hello Bob! How are you?"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": 1,
    "content": "Hello Bob! How are you?",
    "contentType": "TEXT",
    "sender": {
      "id": 1,
      "username": "alice",
      "fullName": "Alice Johnson"
    },
    "conversationId": 1,
    "sentAt": "2025-10-16T10:40:00Z",
    "isEdited": false,
    "isDeleted": false
  }
}
```

---

### Bước 10: User 2 xem messages

**Request:**
```http
GET {{baseUrl}}/v1/conversations/{{conversationId}}/messages
Authorization: Bearer {{user2Token}}
```

**Response:**
```json
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "id": 1,
      "content": "Hello Bob! How are you?",
      "sender": {
        "id": 1,
        "username": "alice",
        "fullName": "Alice Johnson"
      },
      "sentAt": "2025-10-16T10:40:00Z"
    }
  ]
}
```

---

### Bước 11: User 2 reply

**Request:**
```http
POST {{baseUrl}}/v1/conversations/{{conversationId}}/messages
Authorization: Bearer {{user2Token}}
Content-Type: application/json

{
  "content": "Hi Alice! I'm doing great, thanks!"
}
```

---

### Bước 12: User 1 lấy danh sách conversations

**Request:**
```http
GET {{baseUrl}}/v1/conversations
Authorization: Bearer {{user1Token}}
```

**Response:**
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": [
    {
      "id": 1,
      "type": "DIRECT",
      "participants": [
        {
          "userId": 1,
          "username": "alice"
        },
        {
          "userId": 2,
          "username": "bob"
        }
      ],
      "lastMessage": {
        "content": "Hi Alice! I'm doing great, thanks!",
        "sentAt": "2025-10-16T10:42:00Z"
      }
    }
  ]
}
```

---

## Flow 2: REST API - Group Chat

### Bước 1: Tạo thêm User 3

Lặp lại các bước đăng ký, verify và login cho user thứ 3:

```json
{
  "username": "charlie",
  "email": "charlie@example.com",
  "password": "Password123!",
  "fullName": "Charlie Brown"
}
```

Lưu token vào `user3Token` và ID vào `user3Id`.

---

### Bước 2: User 1 tạo Group Chat

**Request:**
```http
POST {{baseUrl}}/v1/conversations
Authorization: Bearer {{user1Token}}
Content-Type: application/json

{
  "name": "Project Team",
  "type": "GROUP",
  "participantIds": [{{user2Id}}, {{user3Id}}]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": 2,
    "name": "Project Team",
    "type": "GROUP",
    "participants": [
      {
        "userId": 1,
        "username": "alice",
        "role": "ADMIN"
      },
      {
        "userId": 2,
        "username": "bob",
        "role": "MEMBER"
      },
      {
        "userId": 3,
        "username": "charlie",
        "role": "MEMBER"
      }
    ]
  }
}
```

**Postman Tests Script:**
```javascript
var jsonData = pm.response.json();
if (jsonData.success && jsonData.data) {
    pm.environment.set("groupConversationId", jsonData.data.id);
}
```

---

### Bước 3: Các members gửi messages trong group

**User 1:**
```http
POST {{baseUrl}}/v1/conversations/{{groupConversationId}}/messages
Authorization: Bearer {{user1Token}}
Content-Type: application/json

{
  "content": "Welcome to the team everyone!"
}
```

**User 2:**
```http
POST {{baseUrl}}/v1/conversations/{{groupConversationId}}/messages
Authorization: Bearer {{user2Token}}
Content-Type: application/json

{
  "content": "Thanks Alice! Glad to be here."
}
```

**User 3:**
```http
POST {{baseUrl}}/v1/conversations/{{groupConversationId}}/messages
Authorization: Bearer {{user3Token}}
Content-Type: application/json

{
  "content": "Hello team! Excited to work with you all!"
}
```

---

## Flow 3: WebSocket - Real-time Messaging

### Setup WebSocket Client

**Tools bạn có thể dùng:**
- **Browser Developer Tools** (Console)
- **Postman** (có hỗ trợ WebSocket từ version mới)
- **WebSocket King** (Chrome Extension)
- **Insomnia**
- **wscat** (CLI tool)

### Bước 1: Kết nối WebSocket cho User 1

**WebSocket URL:**
```
ws://localhost:8080/api/v1/chat?token={{user1Token}}
```

**JavaScript Code (Browser Console):**
```javascript
// Thay YOUR_USER1_TOKEN bằng token thực tế
const user1Token = "YOUR_USER1_TOKEN";
const ws1 = new WebSocket(`ws://localhost:8080/api/v1/chat?token=${user1Token}`);

ws1.onopen = () => {
    console.log("User 1 connected");
};

ws1.onmessage = (event) => {
    console.log("User 1 received:", JSON.parse(event.data));
};

ws1.onerror = (error) => {
    console.error("User 1 error:", error);
};
```

---

### Bước 2: Kết nối WebSocket cho User 2

Mở tab Console khác hoặc dùng Incognito window:

```javascript
const user2Token = "YOUR_USER2_TOKEN";
const ws2 = new WebSocket(`ws://localhost:8080/api/v1/chat?token=${user2Token}`);

ws2.onopen = () => {
    console.log("User 2 connected");
};

ws2.onmessage = (event) => {
    console.log("User 2 received:", JSON.parse(event.data));
};
```

---

### Bước 3: User 1 gửi typing indicator

```javascript
ws1.send(JSON.stringify({
    type: "typing.start",
    payload: {
        conversationId: 1  // Thay bằng conversationId thực tế
    }
}));
```

**User 2 sẽ nhận:**
```json
{
  "type": "typing.indicator",
  "payload": {
    "conversationId": 1,
    "typingUsers": [
      {
        "userId": 1,
        "username": "alice",
        "fullName": "Alice Johnson"
      }
    ]
  }
}
```

---

### Bước 4: User 1 gửi message qua WebSocket

```javascript
ws1.send(JSON.stringify({
    type: "chat.message",
    payload: {
        conversationId: 1,
        content: "Hello via WebSocket!"
    }
}));
```

**Cả User 1 và User 2 sẽ nhận real-time:**
```json
{
  "type": "chat.message",
  "payload": {
    "id": 5,
    "content": "Hello via WebSocket!",
    "sender": {
      "userId": 1,
      "username": "alice",
      "fullName": "Alice Johnson"
    },
    "conversationId": 1,
    "sentAt": "2025-10-16T11:00:00Z",
    "isEdited": false,
    "isDeleted": false
  }
}
```

---

### Bước 5: User 1 stop typing

```javascript
ws1.send(JSON.stringify({
    type: "typing.stop",
    payload: {
        conversationId: 1
    }
}));
```

---

### Bước 6: User 2 reply qua WebSocket

```javascript
ws2.send(JSON.stringify({
    type: "chat.message",
    payload: {
        conversationId: 1,
        content: "Got your message! WebSocket is working!"
    }
}));
```

**Cả 2 users sẽ nhận real-time.**

---

### Bước 7: Test User Status

Khi User 1 connect WebSocket:
```json
{
  "type": "user.status",
  "payload": {
    "userId": "1",
    "username": "alice",
    "displayName": "Alice Johnson",
    "isOnline": true,
    "lastSeen": null
  }
}
```

Khi User 1 disconnect (đóng tab hoặc gọi `ws1.close()`):
```json
{
  "type": "user.status",
  "payload": {
    "userId": "1",
    "username": "alice",
    "isOnline": false,
    "lastSeen": "2025-10-16T11:05:00Z"
  }
}
```

---

## Test với Postman WebSocket

### Setup trong Postman:

1. **New Request** → Chọn **WebSocket**
2. **URL:** `ws://localhost:8080/api/v1/chat?token={{user1Token}}`
3. Click **Connect**

### Gửi messages:

**Tab "Message"**, chọn **JSON** và paste:

```json
{
  "type": "chat.message",
  "payload": {
    "conversationId": 1,
    "content": "Test from Postman WebSocket"
  }
}
```

Click **Send**.

### Xem messages nhận được:

Check tab **"Messages"** để thấy real-time responses.

---

## Complete Test Scenarios

### Scenario 1: Direct Chat với REST API

```bash
1. Register Alice
2. Verify Alice
3. Login Alice → Save token
4. Register Bob
5. Verify Bob
6. Login Bob → Save token
7. Alice tạo conversation với Bob → Save conversationId
8. Alice gửi message
9. Bob xem messages
10. Bob reply
11. Alice xem messages
```

### Scenario 2: Group Chat với REST API

```bash
1. Login Alice, Bob, Charlie
2. Alice tạo group với Bob và Charlie
3. Alice gửi message vào group
4. Bob reply trong group
5. Charlie reply trong group
6. Tất cả members xem messages
```

### Scenario 3: Real-time WebSocket

```bash
1. Alice connect WebSocket
2. Bob connect WebSocket
3. Alice gửi typing.start
4. Bob nhận typing indicator
5. Alice gửi message qua WebSocket
6. Bob nhận message real-time
7. Bob reply qua WebSocket
8. Alice nhận message real-time
9. Alice disconnect
10. Bob nhận user.status update
```

---

## Postman Collection Import

### Tạo Collection JSON:

Save file sau với tên `Chattrix-API.postman_collection.json`:

```json
{
  "info": {
    "name": "Chattrix API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register User 1",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Status code is 201', function () {",
                  "    pm.response.to.have.status(201);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"alice\",\n  \"email\": \"alice@example.com\",\n  \"password\": \"Password123!\",\n  \"fullName\": \"Alice Johnson\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/auth/register",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "auth", "register"]
            }
          }
        },
        {
          "name": "Login User 1",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "var jsonData = pm.response.json();",
                  "if (jsonData.success && jsonData.data) {",
                  "    pm.environment.set('user1Token', jsonData.data.accessToken);",
                  "    pm.environment.set('user1Id', jsonData.data.user.id);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"alice\",\n  \"password\": \"Password123!\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/auth/login",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "auth", "login"]
            }
          }
        },
        {
          "name": "Register User 2",
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"bob\",\n  \"email\": \"bob@example.com\",\n  \"password\": \"Password123!\",\n  \"fullName\": \"Bob Smith\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/auth/register",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "auth", "register"]
            }
          }
        },
        {
          "name": "Login User 2",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "var jsonData = pm.response.json();",
                  "if (jsonData.success && jsonData.data) {",
                  "    pm.environment.set('user2Token', jsonData.data.accessToken);",
                  "    pm.environment.set('user2Id', jsonData.data.user.id);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"bob\",\n  \"password\": \"Password123!\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/auth/login",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "auth", "login"]
            }
          }
        }
      ]
    },
    {
      "name": "Conversations",
      "item": [
        {
          "name": "Create Direct Conversation",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "var jsonData = pm.response.json();",
                  "if (jsonData.success && jsonData.data) {",
                  "    pm.environment.set('conversationId', jsonData.data.id);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{user1Token}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"type\": \"DIRECT\",\n  \"participantIds\": [{{user2Id}}]\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/conversations",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "conversations"]
            }
          }
        },
        {
          "name": "Get All Conversations",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{user1Token}}"
              }
            ],
            "url": {
              "raw": "{{baseUrl}}/v1/conversations",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "conversations"]
            }
          }
        }
      ]
    },
    {
      "name": "Messages",
      "item": [
        {
          "name": "Send Message (User 1)",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{user1Token}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"content\": \"Hello from Alice!\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/conversations/{{conversationId}}/messages",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "conversations", "{{conversationId}}", "messages"]
            }
          }
        },
        {
          "name": "Get Messages",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{user2Token}}"
              }
            ],
            "url": {
              "raw": "{{baseUrl}}/v1/conversations/{{conversationId}}/messages",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "conversations", "{{conversationId}}", "messages"]
            }
          }
        },
        {
          "name": "Send Message (User 2)",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{user2Token}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"content\": \"Hi Alice! Got your message.\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/v1/conversations/{{conversationId}}/messages",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "conversations", "{{conversationId}}", "messages"]
            }
          }
        }
      ]
    }
  ]
}
```

### Import vào Postman:

1. Mở Postman
2. Click **Import**
3. Chọn file `Chattrix-API.postman_collection.json`
4. Tạo Environment mới với variables: `baseUrl`, `user1Token`, `user2Token`, etc.

---

## Troubleshooting

### Lỗi 401 Unauthorized
- Check token có đúng không
- Token có expire chưa? (refresh token nếu cần)
- Header Authorization có đúng format: `Bearer <token>`

### Lỗi 404 Not Found
- Check conversationId có đúng không
- User có phải participant của conversation không

### Lỗi 400 Bad Request
- Check request body có đúng format JSON không
- Check validation (ví dụ: content không được rỗng)

### WebSocket không connect được
- Check token có hợp lệ không
- Check server có chạy không
- Check firewall/CORS settings

---

## Tips

1. **Dùng Environment Variables** trong Postman để tự động lưu tokens và IDs
2. **Test Scripts** tự động extract data từ responses
3. **Collection Runner** để chạy toàn bộ flow tự động
4. **Pre-request Scripts** để làm mới token nếu expire

Chúc bạn test thành công! 🎉

