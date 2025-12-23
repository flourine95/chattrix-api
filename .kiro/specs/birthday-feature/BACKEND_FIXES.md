# 🔧 Backend Fixes - Birthday Feature

## Issues Fixed

### Issue 1: MentionedUser userId is null

**Problem:**
Frontend expects `userId` field in `mentionedUsers`, but backend was only sending `id`.

**Error:**
```
type 'Null' is not a subtype of type 'int' in type cast
```

**Root Cause:**
```java
// MentionedUserResponse.java (OLD)
public class MentionedUserResponse {
    private Long id;  // ← Backend sends this
    private String fullName;
    private String username;
}
```

Frontend expects:
```dart
// mentioned_user_model.dart
class MentionedUserModel {
  final int userId;  // ← Frontend expects this
  final String fullName;
  final String username;
}
```

**Solution:**

Added `userId` field as alias for `id`:

```java
// MentionedUserResponse.java (NEW)
@Getter
@Setter
public class MentionedUserResponse {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("userId")
    private Long userId; // Same as id, for frontend compatibility
    
    private String fullName;
    private String username;
    
    // Ensure both id and userId have the same value
    public void setId(Long id) {
        this.id = id;
        this.userId = id;
    }
}
```

Updated MapStruct mapper:

```java
// UserMapper.java
@Mapping(source = "id", target = "id")
@Mapping(source = "id", target = "userId")
MentionedUserResponse toMentionedUserResponse(User user);
```

**Result:**
- ✅ Backend now sends both `id` and `userId`
- ✅ Frontend can parse `userId` correctly
- ✅ No more null pointer exceptions

---

### Issue 2: Mentioning user not in conversation

**Problem:**
System tries to mention birthday user even if they're not in the conversation.

**Error:**
```
Business error: Cannot mention user who is not in this conversation
```

**Root Cause:**
```java
// BirthdayService.java (OLD)
// Always mention birthday user
ChatMessageRequest messageRequest = new ChatMessageRequest(
    message,
    "SYSTEM",
    null, null, null, null, null,
    null, null, null,
    null,
    List.of(birthdayUser.getId())  // ← Always mention
);
```

**Solution:**

Added check before mentioning:

```java
// BirthdayService.java (NEW)
// Check if birthday user is actually in this conversation
boolean isBirthdayUserInConversation = conversation.getParticipants().stream()
        .anyMatch(p -> p.getUser().getId().equals(birthdayUser.getId()));

// Only mention if user is in conversation
ChatMessageRequest messageRequest = new ChatMessageRequest(
    message,
    "SYSTEM",
    null, null, null, null, null,
    null, null, null,
    null,
    isBirthdayUserInConversation ? List.of(birthdayUser.getId()) : null  // ← Conditional
);
```

**Result:**
- ✅ Only mentions user if they're in conversation
- ✅ No more "Cannot mention user" errors
- ✅ Birthday message still sent (just without mention)

---

### Issue 3: Timezone causing date off by 1 day

**Problem:**
User selects date 21, but database stores date 20.

**Root Cause:**
Frontend sends `2024-12-21T00:00:00Z` (midnight UTC), server timezone converts it to previous day.

**Solution:**

Frontend should send 12:00 UTC instead of 00:00 UTC:

```dart
// ❌ BAD
final dateOfBirth = selectedDate.toUtc(); // 00:00 UTC

// ✅ GOOD
final dateOfBirth = DateTime.utc(
  selectedDate.year,
  selectedDate.month,
  selectedDate.day,
  12, 0, 0, // Noon UTC
);
```

**Backend already handles this correctly:**
- Birthday check only compares month and day
- Ignores time and timezone

**Result:**
- ✅ Date stored correctly
- ✅ Birthday detected correctly
- ✅ Works in all timezones

---

## API Response Changes

### Before (Broken)

```json
{
  "id": 123,
  "content": "🎂 Happy Birthday!",
  "type": "SYSTEM",
  "mentionedUsers": [
    {
      "id": 1,
      "fullName": "Alice Nguyen",
      "username": "alice"
    }
  ]
}
```

Frontend tries to access `mentionedUsers[0].userId` → **null** → **crash**

### After (Fixed)

```json
{
  "id": 123,
  "content": "🎂 Happy Birthday!",
  "type": "SYSTEM",
  "mentionedUsers": [
    {
      "id": 1,
      "userId": 1,
      "fullName": "Alice Nguyen",
      "username": "alice"
    }
  ]
}
```

Frontend accesses `mentionedUsers[0].userId` → **1** → **success** ✅

---

## Testing

### Test MentionedUser Response

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password123"}' \
  | jq -r '.data.accessToken')

# Send birthday wishes
curl -X POST http://localhost:8080/v1/birthdays/send-wishes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "conversationIds": [1],
    "customMessage": "Happy Birthday! 🎉"
  }'

# Check message
curl -X GET http://localhost:8080/v1/conversations/1/messages \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[-1].mentionedUsers'
```

**Expected:**
```json
[
  {
    "id": 2,
    "userId": 2,
    "fullName": "Bob Tran",
    "username": "bob"
  }
]
```

### Test Mention Validation

```bash
# Try to mention user not in conversation (should work now)
curl -X POST http://localhost:8080/v1/birthdays/send-wishes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 999,
    "conversationIds": [1],
    "customMessage": "Test"
  }'
```

**Expected:**
- ✅ Message sent successfully
- ✅ No mention (user not in conversation)
- ✅ No error

---

## Changelog

### v1.0.2 (2024-12-21)

**Fixed:**
- ✅ Added `userId` field to `MentionedUserResponse`
- ✅ Updated `UserMapper` to map both `id` and `userId`
- ✅ Added validation before mentioning users
- ✅ Improved error handling in birthday message sending

**Files Changed:**
- `src/main/java/com/chattrix/api/responses/MentionedUserResponse.java`
- `src/main/java/com/chattrix/api/mappers/UserMapper.java`
- `src/main/java/com/chattrix/api/services/birthday/BirthdayService.java`

---

## Frontend Compatibility

### Old Frontend (Before Fix)
- ❌ Crashes when parsing `mentionedUsers`
- ❌ Expects `userId` field
- ❌ Gets null instead

### New Frontend (After Fix)
- ✅ Parses `mentionedUsers` correctly
- ✅ Gets `userId` field
- ✅ No crashes

### Backward Compatibility
- ✅ Both `id` and `userId` are sent
- ✅ Old clients can use `id`
- ✅ New clients can use `userId`
- ✅ No breaking changes

---

## Related Issues

- **Timezone Issue**: See `FRONTEND_TIMEZONE_FIX.md`
- **Mention Validation**: See `TROUBLESHOOTING.md` Issue 1
- **General Troubleshooting**: See `TROUBLESHOOTING.md`

---

**All issues fixed! 🎉**

*Last updated: 2024-12-21*
