# Compilation Errors Fix Guide

## Status: 94 errors remaining after enum import fixes

---

## Category 1: Mapper Issues (6 errors)

### MessageMapper.java
**Errors:**
- No property named "poll.id" / "poll"
- No property named "event.id" / "event"

**Fix:** Remove poll/event mappings since they're now in JSONB metadata
```java
// Remove these @Mapping annotations:
@Mapping(source = "poll.id", target = "pollId")
@Mapping(source = "event.id", target = "eventId")
```

### ConversationMapper.java
**Error:** No property named "user.online"

**Fix:** Remove online field mapping (now in cache)
```java
// Remove this @Mapping:
@Mapping(source = "user.online", target = "online")
```

### ContactMapper.java
**Error:** No property named "contactUser.online"

**Fix:** Remove online field mapping
```java
// Remove this @Mapping:
@Mapping(source = "contactUser.online", target = "online")
```

### WebSocketMapper.java
**Warning:** Unmapped target properties: "mediaUrl, thumbnailUrl, fileName, fileSize, duration, latitude, longitude, locationName"

**Fix:** Add @Mapping(target = "...", ignore = true) for each

### UserSearchMapper.java
**Warning:** Unmapped target property: "online"

**Fix:** Add @Mapping(target = "online", ignore = true)

---

## Category 2: Missing Repositories (3 errors)

### MessageEditHistoryRepository
**Files affected:**
- MessageService.java
- MessageEditService.java

**Fix:** This repository should exist. Check if file was accidentally deleted.

**Location:** `src/main/java/com/chattrix/api/repositories/MessageEditHistoryRepository.java`

**If missing, create it:**
```java
@ApplicationScoped
public class MessageEditHistoryRepository {
    @PersistenceContext
    private EntityManager em;
    
    @Transactional
    public void deleteByMessageId(Long messageId) {
        em.createQuery("DELETE FROM MessageEditHistory m WHERE m.message.id = :messageId")
            .setParameter("messageId", messageId)
            .executeUpdate();
    }
}
```

### UserNoteRepository
**Files affected:**
- UserNoteService.java

**Fix:** Check if this repository exists or needs to be created.

---

## Category 3: Missing Entities (2 errors)

### MessageEditHistory
**File:** MessageEditService.java

**Fix:** Check if entity exists at `src/main/java/com/chattrix/api/entities/MessageEditHistory.java`

### UserNote
**File:** UserNoteService.java

**Fix:** Check if entity exists at `src/main/java/com/chattrix/api/entities/UserNote.java`

---

## Category 4: Enum References (Still need fixing)

### Call.CallStatus (not CallHistoryStatus)
**Files:**
- CallHistoryResource.java (line 3)
- CallHistoryResponse.java (line 4)
- CallHistoryService.java (line 43)

**Fix:** Change `CallHistoryStatus` to `Call.CallStatus`

### Call.CallDirection
**Files:**
- CallHistoryResponse.java

**Fix:** Verify this enum exists in Call entity

---

## Quick Fix Script

Run this to fix remaining enum issues:

```powershell
# Fix CallHistoryStatus → Call.CallStatus
Get-ChildItem -Path "src" -Filter "*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'CallHistoryStatus', 'Call.CallStatus'
    Set-Content -Path $_.FullName -Value $content -NoNewline
}
```

---

## Priority Fix Order

1. **Fix Mappers** (6 files) - Remove poll/event/online mappings
2. **Check Missing Repositories** (2 files) - MessageEditHistoryRepository, UserNoteRepository
3. **Check Missing Entities** (2 files) - MessageEditHistory, UserNote
4. **Fix Remaining Enums** - CallHistoryStatus, CallDirection

---

## After Fixes

Run:
```bash
mvn clean compile
```

Expected: 0 errors

---

## Notes

- Most enum imports are fixed (13 files)
- Main issues are mapper configurations and missing repositories
- Some entities may have been accidentally deleted during refactor
