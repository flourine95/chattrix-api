# Remaining Refactor Work

## Status
- Enum imports: ✅ FIXED
- UserStatusService: ✅ FIXED  
- TokenService: ✅ FIXED
- MessageResource edit endpoints: ✅ DISABLED (commented out)
- ConversationService: ⚠️ NEEDS MAJOR REFACTOR

## Critical Issue: ConversationSettings Merged into ConversationParticipant

### Background
The `ConversationSettings` entity was removed and its fields merged into `ConversationParticipant`:
- `muted`, `mutedUntil`, `mutedAt`
- `archived`, `archivedAt`
- `pinned`, `pinOrder`, `pinnedAt`
- `theme`, `customNickname`
- `notificationsEnabled`

### Affected Files
1. **ConversationService.java** - Uses `ConversationSettings` extensively:
   - `getConversationById()` - line 239
   - `getConversationSettings()` - line 545
   - `updateConversationSettings()` - line 566
   - `muteConversation()` - line 608 (deprecated)
   - `blockUser()` - line 637 (deprecated)
   - `unblockUser()` - line 650 (deprecated)
   - `createDefaultSettings()` - line 658

2. **ConversationSettingsService.java** - Entire service needs refactor
3. **ConversationSettingsRepository.java** - DELETE (no longer needed)
4. **MessageReadReceiptRepository.java** - DELETE (no longer needed)

### Required Changes

#### Option 1: Quick Fix (Temporary)
Comment out all ConversationSettings-related methods in ConversationService:
- Lines 239-260 (settings population in getConversationById)
- Lines 545-595 (get/update settings methods)
- Lines 597-655 (deprecated methods)
- Lines 658-683 (createDefaultSettings)

#### Option 2: Proper Refactor (Recommended)
1. Update `getConversationById()` to get settings from `ConversationParticipant`:
```java
// Find participant for current user
ConversationParticipant participant = participantRepository
    .findByUserIdAndConversationId(userId, conv.getId())
    .orElseThrow(() -> BusinessException.notFound("Not a participant", "NOT_PARTICIPANT"));

response.setSettings(ConversationSettingsResponse.builder()
    .conversationId(conv.getId())
    .muted(participant.isMuted())
    .mutedUntil(participant.getMutedUntil())
    .notificationsEnabled(participant.isNotificationsEnabled())
    .customNickname(participant.getCustomNickname())
    .theme(participant.getTheme())
    .pinned(participant.isPinned())
    .pinOrder(participant.getPinOrder())
    .archived(participant.isArchived())
    .blocked(false) // Blocking is now separate - check Contact entity
    .hidden(false) // Hidden might need separate handling
    .build());
```

2. Update `getConversationSettings()` similarly

3. Update `updateConversationSettings()` to modify `ConversationParticipant`:
```java
ConversationParticipant participant = participantRepository
    .findByUserIdAndConversationId(userId, conversationId)
    .orElseThrow(() -> BusinessException.notFound("Not a participant", "NOT_PARTICIPANT"));

if (request.getMuted() != null) participant.setMuted(request.getMuted());
if (request.getMutedUntil() != null) participant.setMutedUntil(request.getMutedUntil());
// ... etc

participantRepository.save(participant);
```

4. Delete `createDefaultSettings()` - no longer needed (participant is created when user joins)

5. Refactor `ConversationSettingsService` to work with `ConversationParticipant`

## Other Remaining Issues

### Message Media Fields
Message entity removed these fields (now in metadata JSONB):
- `mediaUrl`, `thumbnailUrl`, `fileName`, `fileSize`, `duration`
- `latitude`, `longitude`, `locationName`
- `failedReason`

**Affected Services:**
- MessageService - lines 213-222, 526-530
- MessageForwardService - lines 66-71
- ScheduledMessageService - lines 106-110, 208-216, 313, 344

**Fix:** Store/retrieve from `message.metadata` Map:
```java
// Set
Map<String, Object> metadata = message.getMetadata();
if (metadata == null) metadata = new HashMap<>();
metadata.put("mediaUrl", mediaUrl);
metadata.put("fileName", fileName);
message.setMetadata(metadata);

// Get
String mediaUrl = (String) message.getMetadata().get("mediaUrl");
```

### Poll/Event DTO Setters
DTOs use `@Builder` but code calls setters:
- PollService - lines 388-389, 409, 420, 426, 437, 439
- EventService - lines 315-316, 345-348, 372, 374

**Fix:** Use builder pattern:
```java
// Instead of:
response.setCreatorId(creatorId);
response.setCreatorUsername(username);

// Use:
response = PollResponse.builder()
    .creatorId(creatorId)
    .creatorUsername(username)
    .build();
```

## Recommendation

**For immediate compilation:**
1. Comment out ConversationSettings methods in ConversationService
2. Comment out affected endpoints in ConversationResource
3. Add TODO comments for proper refactor

**For proper fix:**
1. Refactor ConversationService to use ConversationParticipant
2. Refactor ConversationSettingsService
3. Update message services to use metadata JSONB
4. Fix Poll/Event services to use builders
5. Test all affected endpoints

## Estimated Effort
- Quick fix: 30 minutes
- Proper refactor: 4-6 hours
