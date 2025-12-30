# Tóm Tắt Dọn Dẹp Entities

## ✅ Hoàn Thành

### 1. Di Chuyển Enums Vào Inner Classes

Tất cả enums đã được di chuyển vào trong entity classes để nhất quán:

#### User.java
- ✅ `Gender` (MALE, FEMALE, OTHER)
- ✅ `ProfileVisibility` (PUBLIC, FRIENDS_ONLY, PRIVATE)

#### Call.java
- ✅ `CallType` (AUDIO, VIDEO)
- ✅ `CallStatus` (INITIATING, RINGING, CONNECTING, CONNECTED, DISCONNECTING, ENDED, MISSED, REJECTED, FAILED)
- ✅ `CallDirection` (INCOMING, OUTGOING)
- ✅ `CallEndReason` (USER_HANGUP, NETWORK_DISCONNECT, DEVICE_ERROR, TIMEOUT, UNKNOWN)

#### CallParticipant.java
- ✅ `ParticipantStatus` (INVITED, RINGING, JOINED, LEFT, REJECTED, MISSED)

#### UserToken.java
- ✅ `TokenType` (VERIFY, RESET)

#### Contact.java
- ✅ `ContactStatus` (PENDING, ACCEPTED, REJECTED, BLOCKED) - đã có sẵn

#### GroupPermissions.java
- ✅ `PermissionLevel` (ALL, ADMIN_ONLY) - đã có sẵn
- ✅ `DeletePermissionLevel` (OWNER, ADMIN_ONLY, ALL) - đã có sẵn

#### Message.java
- ✅ `MessageType` (TEXT, IMAGE, LINK, VIDEO, VOICE, AUDIO, DOCUMENT, LOCATION, STICKER, EMOJI, SYSTEM, POLL, EVENT, ANNOUNCEMENT) - đã có sẵn
- ✅ `ScheduledStatus` (PENDING, SENT, FAILED, CANCELLED) - đã có sẵn

#### Conversation.java
- ✅ `ConversationType` (DIRECT, GROUP) - đã có sẵn

#### ConversationParticipant.java
- ✅ `Role` (ADMIN, MEMBER) - đã có sẵn

---

### 2. Xóa Enum Files Độc Lập (9 files)

- ❌ `CallType.java` → `Call.CallType`
- ❌ `CallStatus.java` → `Call.CallStatus`
- ❌ `CallDirection.java` → `Call.CallDirection`
- ❌ `CallEndReason.java` → `Call.CallEndReason`
- ❌ `CallHistoryStatus.java` → Không còn dùng
- ❌ `ParticipantStatus.java` → `CallParticipant.ParticipantStatus`
- ❌ `TokenType.java` → `UserToken.TokenType`
- ❌ `Gender.java` → `User.Gender`
- ❌ `ProfileVisibility.java` → `User.ProfileVisibility`

---

### 3. Xóa Entity Files Không Còn Dùng (12 files)

#### Đã gộp vào Message.metadata (JSONB)
- ❌ `Poll.java`
- ❌ `PollOption.java`
- ❌ `PollVote.java`
- ❌ `Event.java`
- ❌ `EventRsvp.java`

#### Đã gộp vào ConversationParticipant
- ❌ `ConversationSettings.java`
- ❌ `MessageReadReceipt.java`

#### Đã gộp vào Conversation.metadata (JSONB)
- ❌ `GroupInviteLink.java`

#### Sử dụng Message.pinned field
- ❌ `PinnedMessage.java`

#### Đã gộp vào UserToken
- ❌ `VerificationToken.java`
- ❌ `PasswordResetToken.java`

#### Logic đã gộp vào Call
- ❌ `CallHistory.java`

---

## 📊 Entities Còn Lại (13 entities)

### Core Entities (7 bảng chính)
1. ✅ **User.java** - Users table
2. ✅ **Conversation.java** - Conversations table (+ metadata JSONB)
3. ✅ **ConversationParticipant.java** - Conversation participants (+ settings)
4. ✅ **Message.java** - Messages table (+ metadata JSONB)
5. ✅ **Call.java** - Calls table
6. ✅ **CallParticipant.java** - Call participants
7. ✅ **UserToken.java** - User tokens (verify + reset)

### Supporting Entities (6 bảng phụ)
8. ✅ **Contact.java** - Contacts/Friends
9. ✅ **GroupPermissions.java** - Group permissions
10. ✅ **InvalidatedToken.java** - Blacklisted JWT tokens
11. ✅ **MessageEditHistory.java** - Message edit history
12. ✅ **RefreshToken.java** - Refresh tokens
13. ✅ **UserNote.java** - User notes (24h status)

---

## 🔧 Cách Sử Dụng Inner Enums

### Trong Entity
```java
@Entity
public class User {
    @Enumerated(EnumType.STRING)
    private User.Gender gender;
    
    @Enumerated(EnumType.STRING)
    private User.ProfileVisibility profileVisibility;
    
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    
    public enum ProfileVisibility {
        PUBLIC, FRIENDS_ONLY, PRIVATE
    }
}
```

### Trong Service/Repository
```java
// Sử dụng với tên đầy đủ
User.Gender gender = User.Gender.MALE;
Call.CallStatus status = Call.CallStatus.RINGING;
UserToken.TokenType type = UserToken.TokenType.VERIFY;
```

### Trong Request/Response DTOs
```java
public class UserResponse {
    private User.Gender gender;
    private User.ProfileVisibility profileVisibility;
}

public class CallResponse {
    private Call.CallType callType;
    private Call.CallStatus status;
}
```

---

## 📝 Cập Nhật Cần Thiết

### 1. Import Statements
Tất cả các file sử dụng enums cần cập nhật imports:

**TRƯỚC:**
```java
import com.chattrix.api.entities.CallType;
import com.chattrix.api.entities.CallStatus;
import com.chattrix.api.entities.Gender;
```

**SAU:**
```java
import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.User;
// Sử dụng: Call.CallType, Call.CallStatus, User.Gender
```

### 2. Services
Cập nhật tất cả references đến enums:

```java
// TRƯỚC
CallType type = CallType.AUDIO;
CallStatus status = CallStatus.RINGING;

// SAU
Call.CallType type = Call.CallType.AUDIO;
Call.CallStatus status = Call.CallStatus.RINGING;
```

### 3. Repositories
Cập nhật JPQL queries:

```java
// TRƯỚC
"SELECT c FROM Call c WHERE c.status = :status"
.setParameter("status", CallStatus.ENDED)

// SAU (không thay đổi - enum vẫn hoạt động)
"SELECT c FROM Call c WHERE c.status = :status"
.setParameter("status", Call.CallStatus.ENDED)
```

### 4. Mappers
MapStruct tự động xử lý inner enums:

```java
@Mapper(componentModel = "cdi")
public interface UserMapper {
    UserResponse toResponse(User user);
    // MapStruct tự động map User.Gender và User.ProfileVisibility
}
```

### 5. DTOs (Requests/Responses)
Cập nhật field types:

```java
public class CreateCallRequest {
    @NotNull
    private Call.CallType callType; // Thay vì CallType
}

public class UserProfileResponse {
    private User.Gender gender; // Thay vì Gender
    private User.ProfileVisibility profileVisibility; // Thay vì ProfileVisibility
}
```

---

## 🧪 Testing

Sau khi cập nhật:

1. **Compile:**
```bash
mvn clean compile
```

2. **Kiểm tra errors:**
- Import statements
- Enum references
- JPQL queries
- DTO mappings

3. **Build & Deploy:**
```bash
docker compose up -d --build
docker compose logs -f api
```

---

## ✅ Lợi Ích

### 1. Nhất Quán
- Tất cả enums đều là inner classes
- Dễ tìm kiếm và quản lý
- Rõ ràng enum thuộc entity nào

### 2. Encapsulation
- Enums gắn chặt với entity
- Giảm namespace pollution
- Tránh conflict tên

### 3. Maintainability
- Ít files hơn (giảm 21 files)
- Dễ refactor
- IDE autocomplete tốt hơn

### 4. Clean Code
- Entity và enum liên quan ở cùng file
- Dễ đọc và hiểu
- Follow best practices

---

## 📊 Thống Kê

### Trước Cleanup
- **Entities:** 25 files
- **Enums độc lập:** 9 files
- **Tổng:** 34 files

### Sau Cleanup
- **Entities:** 13 files (giảm 12 files)
- **Enums độc lập:** 0 files (giảm 9 files)
- **Tổng:** 13 files

**Giảm:** 21 files (62% reduction)

---

## 🔄 Migration Impact

Các thay đổi này **KHÔNG ảnh hưởng đến database**:
- Enums vẫn lưu dưới dạng STRING
- Column types không thay đổi
- Không cần migration SQL

Chỉ cần:
1. Cập nhật code (imports, references)
2. Recompile
3. Redeploy

---

## 📚 Files Cần Cập Nhật

Tìm và thay thế trong toàn bộ project:

### CallType
```bash
# Find
import com.chattrix.api.entities.CallType;
CallType

# Replace with
import com.chattrix.api.entities.Call;
Call.CallType
```

### CallStatus
```bash
# Find
import com.chattrix.api.entities.CallStatus;
CallStatus

# Replace with
import com.chattrix.api.entities.Call;
Call.CallStatus
```

### Gender
```bash
# Find
import com.chattrix.api.entities.Gender;
Gender

# Replace with
import com.chattrix.api.entities.User;
User.Gender
```

### ProfileVisibility
```bash
# Find
import com.chattrix.api.entities.ProfileVisibility;
ProfileVisibility

# Replace with
import com.chattrix.api.entities.User;
User.ProfileVisibility
```

### ParticipantStatus
```bash
# Find
import com.chattrix.api.entities.ParticipantStatus;
ParticipantStatus

# Replace with
import com.chattrix.api.entities.CallParticipant;
CallParticipant.ParticipantStatus
```

### TokenType
```bash
# Find
import com.chattrix.api.entities.TokenType;
TokenType

# Replace with
import com.chattrix.api.entities.UserToken;
UserToken.TokenType
```

---

**Hoàn tất cleanup entities!** 🎉
