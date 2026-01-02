# MessageMapper Refactoring Complete

## Problem
`MessageMapper` was overly complex with:
- 30+ `@Mapping` annotations
- 10+ default helper methods for extracting individual metadata fields
- Complex `expression = "java(...)"` syntax
- Flattening JSONB metadata into individual response fields (mediaUrl, fileName, latitude, etc.)
- Difficult to read and maintain
- MapStruct not suitable for this use case

## Solution
Converted `MessageMapper` from **MapStruct interface** → **CDI bean** (`@ApplicationScoped`)

**Key improvement:** Instead of flattening metadata fields, now pass through `metadata` object directly.

### Changes Made

#### 1. MessageResponse.java
**Before (FLAT - 40+ fields):**
```java
public class MessageResponse {
    private Long id;
    private String content;
    // Flattened metadata fields
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String failedReason;
    private List<Map<String, Object>> editHistory;
    // ... 30+ more fields
}
```

**After (NESTED - cleaner):**
```java
public class MessageResponse {
    private Long id;
    private String content;
    // Metadata as object (contains all metadata fields)
    private Map<String, Object> metadata;
    // ... other fields
}
```

#### 2. MessageMapper.java
**Before:**
```java
@Mapper(componentModel = JAKARTA_CDI, uses = {UserMapper.class})
public interface MessageMapper {
    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "mediaUrl", expression = "java(extractMediaUrl(message))")
    @Mapping(target = "fileName", expression = "java(extractFileName(message))")
    // ... 30+ annotations
    MessageResponse toResponse(Message message);
    
    default String extractMediaUrl(Message message) { ... }
    default String extractFileName(Message message) { ... }
    // ... 10+ default methods
}
```

**After:**
```java
@ApplicationScoped
@Slf4j
public class MessageMapper {
    @Inject private PollMapper pollMapper;
    @Inject private EventMapper eventMapper;
    
    public MessageResponse toResponse(Message message) {
        MessageResponse response = new MessageResponse();
        mapBasicFields(message, response);
        mapSender(message, response);
        mapReplyContext(message, response);
        mapForwardInfo(message, response);
        response.setMetadata(message.getMetadata()); // ← SIMPLE!
        mapPollEvent(message, response);
        mapPinnedInfo(message, response);
        mapScheduledInfo(message, response);
        return response;
    }
    
    private void mapBasicFields(...) { }
    private void mapSender(...) { }
    // ... 7 clear private methods
}
```

## Benefits

### Simplicity
- **Before**: Extract 10+ individual fields from metadata Map
- **After**: Just pass through metadata object → `response.setMetadata(message.getMetadata())`

### Readability
- **Before**: 1 interface with 30+ annotations + 10+ default methods = very hard to read
- **After**: 1 class with 7 clear private methods = easy to understand

### Maintainability
- Clear separation of concerns (each method handles one aspect)
- Easy to add new metadata fields (no mapper changes needed)
- No complex MapStruct expressions
- No extraction helper methods needed

### Flexibility
- Can inject other CDI beans (`PollMapper`, `EventMapper`)
- Can add logging with `@Slf4j`
- Can add custom business logic easily
- Frontend gets full metadata object (can access any field)

### Consistency
- Follows same pattern as `PollMapper` and `EventMapper` (both are CDI beans)
- All mappers now use same approach

### Type Safety Trade-off
- **Lost**: Individual typed fields (String mediaUrl, Long fileSize, etc.)
- **Gained**: Flexibility - any metadata field can be added without mapper changes
- Frontend can still access fields: `message.metadata.mediaUrl`, `message.metadata.latitude`

## Structure

### Main Method
```java
public MessageResponse toResponse(Message message)
```

### Private Helper Methods
1. `mapBasicFields()` - id, content, type, reactions, mentions, timestamps
2. `mapSender()` - sender info (id, username, fullName)
3. `mapReplyContext()` - reply-to message
4. `mapForwardInfo()` - forward info
5. `mapPollEvent()` - poll/event preview extraction
6. `mapPinnedInfo()` - pinned message info
7. `mapScheduledInfo()` - scheduled message info

### Extraction Helpers (for Poll/Event only)
- `extractPollPreview()` - delegates to `PollMapper`
- `extractEventPreview()` - delegates to `EventMapper`

**Removed:** All metadata extraction helpers (extractString, extractLong, extractDouble, extractEditHistory, etc.) - no longer needed!

## Response Structure

### Before (Flat)
```json
{
  "id": 1,
  "content": "Check this image",
  "type": "IMAGE",
  "mediaUrl": "https://...",
  "fileName": "photo.jpg",
  "fileSize": 1024000,
  "thumbnailUrl": "https://...",
  "duration": null,
  "latitude": null,
  "longitude": null
}
```

### After (Nested)
```json
{
  "id": 1,
  "content": "Check this image",
  "type": "IMAGE",
  "metadata": {
    "mediaUrl": "https://...",
    "fileName": "photo.jpg",
    "fileSize": 1024000,
    "thumbnailUrl": "https://..."
  }
}
```

**Frontend access:**
```javascript
// Before
const url = message.mediaUrl;
const size = message.fileSize;

// After
const url = message.metadata?.mediaUrl;
const size = message.metadata?.fileSize;
```

## Files Modified
- `src/main/java/com/chattrix/api/responses/MessageResponse.java` - removed 10+ flattened fields, added `metadata` object
- `src/main/java/com/chattrix/api/mappers/MessageMapper.java` - converted to CDI bean, simplified to 7 methods
- `src/main/java/com/chattrix/api/mappers/WebSocketMapper.java` - removed `uses` reference

## Breaking Changes
⚠️ **Frontend must update** to access metadata fields:
- `message.mediaUrl` → `message.metadata?.mediaUrl`
- `message.fileName` → `message.metadata?.fileName`
- `message.latitude` → `message.metadata?.latitude`
- etc.

## Compilation Status
✅ **SUCCESS** - All files compile without errors

## Next Steps
None - refactoring complete and working. Frontend needs to update to use nested metadata structure.
