# Phase 3 Cleanup - Fix Plan

## Summary
After refactor, we have ~70 compilation errors due to removed fields that are now in JSONB metadata or cache.

## Removed Fields

### User Entity
- `online` (boolean) - Now managed in memory via UserStatusService.activeSessionsCount

### Message Entity  
- `mediaUrl`, `thumbnailUrl`, `fileName`, `fileSize`, `duration` - Now in metadata JSONB
- `latitude`, `longitude`, `locationName` - Now in metadata JSONB
- `failedReason` - Now in metadata JSONB

### ConversationParticipant Entity
- `updatedAt` - Never existed

## Fix Strategy

### 1. UserStatusService (3 errors)
**Issue:** Calls `user.setOnline(true/false)` which doesn't exist
**Fix:** Remove database updates, keep only in-memory tracking
- Lines 30, 50, 90: Remove `user.setOnline()` calls
- Keep `user.setLastSeen()` calls (this field still exists)

### 2. FriendRequestService (1 error)
**Issue:** Calls `otherUser.isOnline()` 
**Fix:** Use `userStatusService.isUserOnline(otherUser.getId())`

### 3. TokenService (1 error)
**Issue:** `new RefreshToken(user, expiresAt)` - wrong constructor
**Fix:** Use RefreshToken.builder() pattern

### 4. MessageService (15+ errors)
**Issue:** Sets/gets media fields that are now in metadata
**Fix:** Store/retrieve from metadata JSONB map
- `setMediaUrl()` → `metadata.put("mediaUrl", value)`
- `getMediaUrl()` → `metadata.get("mediaUrl")`
- Same for: thumbnailUrl, fileName, fileSize, duration, latitude, longitude, locationName

### 5. PollService (10+ errors)
**Issue:** DTOs use setters but have @Builder
**Fix:** Use builder pattern instead of setters
- `response.setCreatorId()` → Use builder
- `option.setText()` → Use builder

### 6. EventService (10+ errors)
**Issue:** Same as PollService - setters on @Builder DTOs
**Fix:** Use builder pattern

### 7. ScheduledMessageService (8+ errors)
**Issue:** Sets media fields that don't exist
**Fix:** Store in metadata JSONB

### 8. MessageForwardService (6 errors)
**Issue:** Gets media fields that don't exist
**Fix:** Retrieve from metadata JSONB

### 9. ReadReceiptService (1 error)
**Issue:** Calls `participant.getUpdatedAt()` which doesn't exist
**Fix:** Use `participant.getLastReadAt()` or remove

### 10. ConversationService (3 errors)
**Issue:** Missing `conversationRepository.delete()` method
**Fix:** Add delete method to repository or use EntityManager.remove()

### 11. MessageReadResource (1 error)
**Issue:** Wrong method signature for `markConversationAsUnread()`
**Fix:** Check method signature and fix call

## Priority Order
1. Fix UserStatusService - simple removal
2. Fix TokenService - use builder
3. Fix MessageService - critical for messaging
4. Fix repository methods
5. Fix DTO builders (Poll/Event services)
6. Fix remaining services

## Next Steps
1. Fix files one by one
2. Compile after each major fix
3. Document any breaking API changes
