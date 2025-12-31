# Chat Functionality Audit - Issues Found

## CRITICAL ISSUES

### ✅ ISSUE #1: ChatServerEndpoint không dùng MessageMetadataMapper
**Location**: `ChatServerEndpoint.handleChatMessage()`
**Status**: **FIXED**

**Changes Made**:
- Added `@Inject MessageMetadataMapper metadataMapper`
- Replaced manual `metadata.put()` with `MessageMetadata.builder()` + `metadataMapper.toMap()`
- Type-safe metadata handling

---

### ✅ ISSUE #2: Cache invalidation missing in ChatServerEndpoint
**Location**: `ChatServerEndpoint.handleChatMessage()`
**Status**: **FIXED**

**Changes Made**:
- Added `@Inject CacheManager cacheManager`
- Added `@Inject MessageCache messageCache`
- Call `cacheManager.invalidateConversationCaches()` after saving message
- Call `messageCache.invalidate()` after saving message

---

### ⚠️ ISSUE #3: UserProfileCache usage incomplete
**Location**: `ChatServerEndpoint.handleChatMessage()`
**Problem**:
- Chỉ cache mentioned users
- Không cache sender info
- Không cache typing users

**Fix Required**:
- Cache sender khi broadcast message
- Cache typing users khi broadcast typing indicator

**Impact**: MEDIUM - miss cache opportunities

---

## MODERATE ISSUES

### ✅ ISSUE #4: MessageMapper metadata mapping
**Location**: `MessageMapper.toResponse()`
**Status**: **FIXED**

**Changes Made**:
- Added `MessageMetadataMapper` to uses clause
- Added @Mapping expressions for all metadata fields:
  - mediaUrl, thumbnailUrl, fileName, fileSize, duration
  - latitude, longitude, locationName
  - failedReason
- Added helper methods to extract from JSONB Map

**Impact**: MessageResponse now properly includes metadata fields

---

### ✅ ISSUE #5: WebSocketMapper metadata mapping
**Location**: `WebSocketMapper.toOutgoingMessageResponse()`
**Status**: **ALREADY CORRECT**

**Finding**: WebSocketMapper already has proper metadata extraction with helper methods
- extractMetadataString()
- extractMetadataLong()
- extractMetadataInteger()
- extractMetadataDouble()

**No changes needed**

---

## MODERATE ISSUES

### ❌ ISSUE #6: SystemMessageService không dùng MessageMetadataMapper
**Location**: `SystemMessageService` - all create methods
**Problem**:
- Đang manually `metadata.put("kickedBy", kickedByUserId)` 
- Có `SystemMessageMetadata` DTO nhưng không dùng
- Không consistent với architecture

**Fix Required**:
- Inject `MessageMetadataMapper`
- Use `MessageMetadata.builder()` instead of manual Map operations

**Impact**: MEDIUM - affects all system messages

---

## MINOR ISSUES

### ℹ️ ISSUE #7: Missing @Slf4j in some services
**Location**: Various service classes
**Problem**: Một số service không có logging

**Fix**: Add `@Slf4j` annotation

**Impact**: LOW - affects debugging

---

### ℹ️ ISSUE #7: Inconsistent error messages
**Location**: Various exception throws
**Problem**: Some use "User not found", some use "RESOURCE_NOT_FOUND"

**Fix**: Standardize error messages

**Impact**: LOW - UX consistency

---

## VALIDATION NEEDED

### 🔍 CHECK #1: MessageRepository queries
- [ ] Verify JPQL queries work with JSONB metadata
- [ ] Check if indexes needed on metadata fields
- [ ] Test performance with large metadata

### 🔍 CHECK #2: Cache TTL values
- [ ] OnlineStatusCache: 5 min - OK?
- [ ] UserProfileCache: 1 hour - OK?
- [ ] ConversationCache: 10 min - OK?
- [ ] MessageCache: 5 min - OK?

### 🔍 CHECK #3: WebSocket event broadcasting
- [ ] All events use WebSocketEventType constants ✅
- [ ] All events use proper DTOs ✅
- [ ] No Map<String, Object> payloads ✅

---

## PRIORITY FIX ORDER

1. **CRITICAL #1**: Fix ChatServerEndpoint to use MessageMetadataMapper
2. **CRITICAL #2**: Add cache invalidation in ChatServerEndpoint
3. **MODERATE #4**: Fix MessageMapper metadata mapping
4. **MODERATE #5**: Fix WebSocketMapper metadata mapping
5. **MODERATE #3**: Complete UserProfileCache usage
6. **MINOR #6-7**: Logging and error message consistency

---

## NEXT ACTIONS

1. Fix ChatServerEndpoint.handleChatMessage()
2. Fix MessageMapper and WebSocketMapper
3. Add cache invalidation calls
4. Test end-to-end message flow
5. Continue audit on REST endpoints
