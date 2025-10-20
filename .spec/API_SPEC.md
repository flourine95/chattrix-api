# Chattrix API Specification

## 1. General Information

- **Base Path:** `/api`
- **API Version:** `v1`
- **Authentication:** All endpoints are secured and require a `Bearer Token` in the `Authorization` header. The token should be obtained from your authentication provider.

---

## 2. RESTful API

### 2.1. User Status

**Resource Path:** `/api/v1/users/status`

- #### `GET /online`
  - **Description:** Retrieves a list of all currently online users.
  - **Responses:**
    - `200 OK`: Returns a list of online user objects.

- #### `GET /online/conversation/{conversationId}`
  - **Description:** Retrieves a list of online users within a specific conversation.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Responses:**
    - `200 OK`: Returns a list of online user objects in the conversation.

- #### `GET /{userId}`
  - **Description:** Retrieves the online status and active session count for a specific user.
  - **Path Parameters:**
    - `userId` (long): The ID of the user to check.
  - **Responses:**
    - `200 OK`: Returns an object containing `userId`, `isOnline` (boolean), and `sessionCount` (integer).

---

### 2.2. Conversations

**Resource Path:** `/api/v1/conversations`

- #### `POST /`
  - **Description:** Creates a new conversation.
  - **Request Body:** `CreateConversationRequest`
    ```json
    {
      "name": "Optional Conversation Name",
      "participantIds": [2, 3]
    }
    ```
  - **Responses:**
    - `201 Created`: Returns the newly created conversation object.

- #### `GET /`
  - **Description:** Retrieves all conversations for the authenticated user.
  - **Responses:**
    - `200 OK`: Returns a list of conversation objects.

- #### `GET /{conversationId}`
  - **Description:** Retrieves the details of a specific conversation.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Responses:**
    - `200 OK`: Returns the requested conversation object.

---

### 2.3. Messages

**Resource Path:** `/api/v1/conversations/{conversationId}/messages`

- #### `POST /`
  - **Description:** Sends a message to a specific conversation.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation to send the message to.
  - **Request Body:** `ChatMessageRequest`
    ```json
    {
      "content": "Hello, world!"
    }
    ```
  - **Responses:**
    - `201 Created`: Returns the newly created message object.

- #### `GET /`
  - **Description:** Retrieves messages for a specific conversation, with pagination.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Query Parameters:**
    - `page` (int, optional, default: `0`): The page number to retrieve.
    - `size` (int, optional, default: `50`): The number of messages per page.
  - **Responses:**
    - `200 OK`: Returns a list of message objects.

- #### `GET /{messageId}`
  - **Description:** Retrieves a single message by its ID.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation.
    - `messageId` (long): The ID of the message.
  - **Responses:**
    - `200 OK`: Returns the requested message object.

---

### 2.4. Contacts

**Resource Path:** `/api/v1/contacts`

- #### `POST /`
  - **Description:** Adds a new user to the authenticated user's contact list.
  - **Request Body:** `AddContactRequest`
    ```json
    {
      "contactUserId": 4
    }
    ```
  - **Responses:**
    - `201 Created`: Returns the newly added contact object.

- #### `GET /`
  - **Description:** Retrieves all contacts for the authenticated user.
  - **Responses:**
    - `200 OK`: Returns a list of contact objects.

- #### `GET /favorites`
  - **Description:** Retrieves all favorite contacts for the authenticated user.
  - **Responses:**
    - `200 OK`: Returns a list of favorite contact objects.

- #### `PUT /{contactId}`
  - **Description:** Updates a contact's details (e.g., alias, favorite status).
  - **Path Parameters:**
    - `contactId` (long): The ID of the contact to update.
  - **Request Body:** `UpdateContactRequest`
    ```json
    {
      "alias": "Johnny",
      "isFavorite": true
    }
    ```
  - **Responses:**
    - `200 OK`: Returns the updated contact object.

- #### `DELETE /{contactId}`
  - **Description:** Removes a contact from the user's contact list.
  - **Path Parameters:**
    - `contactId` (long): The ID of the contact to delete.
  - **Responses:**
    - `200 OK`: Confirms successful deletion.

---

### 2.5. Typing Indicators (Test Endpoints)

**Resource Path:** `/api/v1/typing`

These endpoints are primarily for testing and may be replaced by WebSocket events.

- #### `POST /start`
  - **Description:** Simulates the current user starting to type in a conversation.
  - **Query Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Responses:**
    - `200 OK`.

- #### `POST /stop`
  - **Description:** Simulates the current user stopping typing.
  - **Query Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Responses:**
    - `200 OK`.

- #### `GET /status/{conversationId}`
  - **Description:** Gets the list of users currently typing in a conversation.
  - **Path Parameters:**
    - `conversationId` (long): The ID of the conversation.
  - **Query Parameters:**
    - `excludeUserId` (long, optional): User ID to exclude from the results.
  - **Responses:**
    - `200 OK`: Returns an object with `conversationId` and a `typingUserIds` set.

---

## 3. WebSocket API

**Endpoint:** `ws://<your-server-address>/ws/chat?token=<your-jwt-token>`

The WebSocket connection is used for real-time, bidirectional communication between the client and the server.

### 3.1. Connection

- The client must connect to the WebSocket endpoint with a valid JWT `token` as a query parameter.
- The server will validate the token. If the token is invalid or missing, the connection will be closed.
- Once connected, the user is marked as `online`. When the last active session for a user is closed, they are marked as `offline`.

### 3.2. Message Format

All messages sent over the WebSocket follow a common structure:

```json
{
  "type": "event.name",
  "payload": { ... }
}
```

### 3.3. Client-to-Server Events

- #### Send Message
  - `type`: `chat.message`
  - `payload`:
    ```json
    {
      "conversationId": 123,
      "content": "This is a real-time message!"
    }
    ```

- #### Start Typing
  - `type`: `typing.start`
  - `payload`:
    ```json
    {
      "conversationId": 123
    }
    ```

- #### Stop Typing
  - `type`: `typing.stop`
  - `payload`:
    ```json
    {
      "conversationId": 123
    }
    ```

### 3.4. Server-to-Client Events

- #### New Message
  - `type`: `chat.message`
  - `payload`: An `OutgoingMessageDto` object.
    ```json
    {
      "id": 456,
      "conversationId": 123,
      "sender": {
        "id": 2,
        "username": "john.doe",
        "displayName": "John Doe"
      },
      "content": "This is a real-time message!",
      "timestamp": "2023-10-27T10:00:00Z",
      "type": "TEXT"
    }
    ```

- #### Typing Indicator
  - `type`: `typing.indicator`
  - `payload`: An object containing the list of users currently typing in a conversation.
    ```json
    {
      "conversationId": 123,
      "typingUsers": [
        {
          "id": 3,
          "username": "jane.doe",
          "displayName": "Jane Doe"
        }
      ]
    }
    ```

- #### User Status Change
  - `type`: `user.status`
  - `payload`: An object indicating a user has come online or gone offline.
    ```json
    {
      "userId": "2",
      "username": "john.doe",
      "displayName": "John Doe",
      "isOnline": true,
      "lastSeen": "2023-10-27T09:55:00Z"
    }
    ```
