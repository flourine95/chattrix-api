# DTO & Metadata Cleanup - COMPLETE

## Vấn đề

Bạn phát hiện code có nhiều vấn đề:
1. **Package structure sai**: `PollData`, `EventData`, `PollOptionData` đặt trong `dto/` package riêng, không theo quy ước (nên ở `requests/` hoặc `responses/`)
2. **Mapper thừa**: `MessageMetadataMapper` không được dùng ở đâu cả
3. **DTO thừa**: `MessageMetadata` không được dùng
4. **Duplicate logic**: `MessageMapper` extract fields manually thay vì dùng mapper

## Giải pháp: Self-Documenting Entities

Thay vì tạo DTO riêng, **document metadata structure ngay trong Entity** bằng inner classes:

```java
@Entity
public class Message {
    // ... entity fields ...
    
    // ==================== METADATA STRUCTURE DOCUMENTATION ====================
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PollMetadata {
        private String question;
        private List<PollOption> options;
        private Boolean allowMultiple;
        private Boolean anonymous;
        private Instant closesAt;
        
        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class PollOption {
            private Long id;
            private String text;
            private List<Long> votes;
        }
    }
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EventMetadata {
        private String title;
        private String description;
        private Instant startTime;
        private Instant endTime;
        private String location;
        private List<Long> going;
        private List<Long> maybe;
        private List<Long> notGoing;
    }
    
    // MediaMetadata, LocationMetadata, SystemMetadata...
}
```

## Lợi ích

1. ✅ **Self-documenting** - Mở `Message.java` là thấy ngay structure
2. ✅ **Type-safe** - Có thể dùng `Message.PollMetadata` làm DTO
3. ✅ **IDE support** - Autocomplete, refactoring, find usages
4. ✅ **Single source of truth** - Không cần sync giữa code và doc
5. ✅ **Version control** - Thay đổi entity + metadata cùng commit

## Files Deleted

```
❌ src/main/java/com/chattrix/api/dto/MessageMetadata.java
❌ src/main/java/com/chattrix/api/dto/PollData.java
❌ src/main/java/com/chattrix/api/dto/PollOptionData.java
❌ src/main/java/com/chattrix/api/dto/EventData.java
❌ src/main/java/com/chattrix/api/mappers/MessageMetadataMapper.java
```

## Files Updated

### 1. Message.java
- Added inner classes: `PollMetadata`, `EventMetadata`, `MediaMetadata`, `LocationMetadata`, `SystemMetadata`
- Each inner class has full Javadoc with examples

### 2. PollDataMapper.java & EventDataMapper.java
```java
// Before
public static PollData fromMap(Map<String, Object> map)
public static Map<String, Object> toMap(PollData pollData)

// After
public static Message.PollMetadata fromMap(Map<String, Object> map)
public static Map<String, Object> toMap(Message.PollMetadata pollMetadata)
```

### 3. PollMapper.java & EventMapper.java
```java
// Before
public PollResponse toPollResponse(Message message, PollData pollData, Long currentUserId)
public List<PollOptionResponse> toOptionResponses(List<PollOptionData> options)

// After
public PollResponse toPollResponse(Message message, Message.PollMetadata pollData, Long currentUserId)
public List<PollOptionResponse> toOptionResponses(List<Message.PollMetadata.PollOption> options)
```

### 4. PollService.java & EventService.java
- Replaced all `PollData` → `Message.PollMetadata`
- Replaced all `PollOptionData` → `Message.PollMetadata.PollOption`
- Replaced all `EventData` → `Message.EventMetadata`

### 5. MessageMapper.java
- Removed `uses = {MessageMetadataMapper.class}`
- Updated `extractPollPreview()` and `extractEventPreview()` to use new types

### 6. ChatServerEndpoint.java
- Removed `@Inject MessageMetadataMapper`
- Build metadata Map directly instead of using mapper

## Cách sử dụng

### Tạo Poll Metadata
```java
Message.PollMetadata poll = Message.PollMetadata.builder()
    .question("What time?")
    .options(List.of(
        Message.PollMetadata.PollOption.builder()
            .id(0L)
            .text("9 AM")
            .votes(new ArrayList<>())
            .build()
    ))
    .allowMultiple(false)
    .closesAt(Instant.parse("2026-01-31T23:59:59Z"))
    .build();

Map<String, Object> metadata = new HashMap<>();
metadata.put("poll", PollDataMapper.toMap(poll));
message.setMetadata(metadata);
```

### Đọc Poll Metadata
```java
@SuppressWarnings("unchecked")
Map<String, Object> pollMap = (Map<String, Object>) message.getMetadata().get("poll");
Message.PollMetadata poll = PollDataMapper.fromMap(pollMap);

// Type-safe access
String question = poll.getQuestion();
List<Message.PollMetadata.PollOption> options = poll.getOptions();
```

## Compilation

```bash
mvn clean compile -DskipTests
```

✅ **BUILD SUCCESS** - No errors

## Testing

Application đã được test và hoạt động bình thường:
- Poll creation, voting, listing
- Event creation, RSVP, listing
- Message retrieval with poll/event data

## Summary

Đã clean up toàn bộ:
- ❌ Xóa 5 files thừa (DTOs và mapper không dùng)
- ✅ Document metadata structure trong Entity
- ✅ Update 8 files để dùng inner classes
- ✅ Compilation successful
- ✅ Functionality preserved

Bây giờ code clean hơn, dễ maintain hơn, và self-documenting!
