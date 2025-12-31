# Metadata Implementation - Final Summary

## ✅ Đã hoàn thành

### 1. Tạo MessageMetadata DTO
**Location:** `src/main/java/com/chattrix/api/dto/MessageMetadata.java`

Type-safe wrapper cho JSONB metadata column với các fields:
- **Media**: mediaUrl, thumbnailUrl, fileName, fileSize, duration
- **Location**: latitude, longitude, locationName  
- **System**: kickedBy, addedBy, promotedBy, demotedBy, mutedBy, unmutedBy, invitedBy, oldName, newName, mutedUntil, failedReason
- **Nested**: poll, event (Object type cho flexibility)

### 2. Tạo MessageMetadataMapper (MapStruct)
**Location:** `src/main/java/com/chattrix/api/mappers/MessageMetadataMapper.java`

MapStruct mapper với CDI injection:
- `toMap(MessageMetadata)` - DTO → Map cho database
- `fromMap(Map)` - Map → DTO để đọc từ database
- Helper methods cho type conversion (getString, getLong, getInteger, getDouble)

### 3. Refactor SystemMessageService
**Location:** `src/main/java/com/chattrix/api/services/message/SystemMessageService.java`

Đã thay thế tất cả `Map.put()` bằng type-safe DTO:

**Trước:**
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("kickedBy", userId);
```

**Sau:**
```java
MessageMetadata metadata = MessageMetadata.builder()
    .kickedBy(userId)
    .build();
message.setMetadata(metadataMapper.toMap(metadata));
```

### 4. Tạo DATABASE-JSONB-STRUCTURE.md
Chi tiết documentation về:
- Structure của metadata cho từng MessageType
- Structure của reactions và mentions
- SQL query examples
- Migration guidelines
- Best practices

---

## 🎯 Kiến trúc

### Database Layer (PostgreSQL JSONB)
```
messages table:
├── metadata (JSONB)    ← Flexible data theo message type
├── reactions (JSONB)   ← Social interactions: {"👍": [123, 456]}
└── mentions (JSONB)    ← User references: [123, 456, 789]
```

### Application Layer (Java)
```
Map<String, Object> (Database)
         ↕ MessageMetadataMapper (MapStruct)
MessageMetadata DTO (Type-safe)
```

---

## 📋 Tại sao giữ reactions và mentions riêng?

### ✅ Đúng - 3 columns riêng biệt:

1. **metadata** - Message-type specific data
   - Phụ thuộc vào type (IMAGE có mediaUrl, LOCATION có latitude)
   - Ít khi query trực tiếp
   - Flexible structure

2. **reactions** - Social interaction data
   - Query thường xuyên: "Tìm messages user X đã react"
   - Cần index riêng: `CREATE INDEX ON messages USING GIN (reactions)`
   - Fixed structure: `{"emoji": [userIds]}`

3. **mentions** - User reference data
   - Query thường xuyên: "Tìm messages mention user Y"
   - Cần index riêng: `CREATE INDEX ON messages USING GIN (mentions)`
   - Fixed structure: `[userIds]`

### ❌ Sai - Gộp vào metadata:
- Query chậm hơn (phải scan toàn bộ metadata)
- Không thể index hiệu quả
- Mixing concerns (message data vs social data)

---

## 💡 Cách sử dụng

### Writing metadata (Service layer)
```java
@Inject
private MessageMetadataMapper metadataMapper;

// Build DTO
MessageMetadata metadata = MessageMetadata.builder()
    .mediaUrl("https://...")
    .fileSize(1024000L)
    .latitude(10.762622)
    .longitude(106.660172)
    .build();

// Convert to Map and save
message.setMetadata(metadataMapper.toMap(metadata));
messageRepository.save(message);
```

### Reading metadata (Service layer)
```java
// Read from database
Message message = messageRepository.findById(id);

// Convert to DTO
MessageMetadata metadata = metadataMapper.fromMap(message.getMetadata());

// Type-safe access
if (metadata.hasMedia()) {
    String url = metadata.getMediaUrl();
    Long size = metadata.getFileSize();
}

if (metadata.hasLocation()) {
    Double lat = metadata.getLatitude();
    Double lng = metadata.getLongitude();
}
```

### Convenience methods
```java
metadata.hasMedia()     // Check if has media fields
metadata.hasLocation()  // Check if has location fields
metadata.hasPoll()      // Check if has poll data
metadata.hasEvent()     // Check if has event data
```

---

## 🚀 Next Steps - Services cần refactor

### High Priority
1. **MessageService** - 8 lần `metadata.put()`
2. **ScheduledMessageService** - 10 lần `metadata.put()`
3. **ChatServerEndpoint** - 8 lần `metadata.put()`

### Medium Priority
4. **PollService** - Nested poll object
5. **EventService** - Nested event object (đã thêm RSVP)
6. **GroupInviteLinkService** - Conversation metadata

### Pattern để refactor:
```java
// 1. Inject mapper
@Inject
private MessageMetadataMapper metadataMapper;

// 2. Replace Map.put() với DTO builder
MessageMetadata metadata = MessageMetadata.builder()
    .field1(value1)
    .field2(value2)
    .build();

// 3. Use mapper
message.setMetadata(metadataMapper.toMap(metadata));
```

---

## 📊 Lợi ích đạt được

### Type Safety
- ❌ Trước: `metadata.put("mediaUrl", url)` - có thể typo
- ✅ Sau: `metadata.mediaUrl(url)` - compiler check

### IDE Support
- ❌ Trước: Không có autocomplete cho keys
- ✅ Sau: Full autocomplete cho tất cả fields

### Refactoring
- ❌ Trước: Find/replace toàn bộ codebase
- ✅ Sau: Rename trong DTO, IDE tự động refactor

### Documentation
- ❌ Trước: Phải đọc code để biết structure
- ✅ Sau: Xem DTO class và DATABASE-JSONB-STRUCTURE.md

### Maintainability
- ❌ Trước: 40+ lần `metadata.put()` scattered
- ✅ Sau: Centralized trong DTO + mapper

---

## 🔄 Backward Compatibility

✅ **100% compatible**
- Database schema không đổi
- Vẫn lưu dạng JSONB Map
- Code cũ vẫn hoạt động
- Chỉ thay đổi cách tạo/đọc metadata

---

## 📝 Event RSVP Structure

Event metadata đã được update với RSVP responses:

```json
{
  "event": {
    "title": "Team Meeting",
    "description": "Discuss Q1 roadmap",
    "startTime": 1735632000000,
    "endTime": 1735635600000,
    "location": "Office - Meeting Room A",
    "participants": [123, 456, 789],
    "rsvp": {
      "123": "GOING",
      "456": "MAYBE", 
      "789": "NOT_GOING"
    }
  }
}
```

**RSVP Status:**
- `GOING` - User will attend
- `MAYBE` - User might attend
- `NOT_GOING` - User won't attend
- `null` - User hasn't responded

---

## 🎓 Best Practices

### ✅ DO:
1. **Luôn dùng MessageMetadataMapper** (không manual conversion)
2. **Inject mapper trong services** với `@Inject`
3. **Dùng builder pattern** cho DTO
4. **Check null với convenience methods** (`hasMedia()`, `hasLocation()`)
5. **Update DATABASE-JSONB-STRUCTURE.md** khi thêm fields mới

### ❌ DON'T:
1. **Không dùng `Map.put()` trực tiếp**
2. **Không manual conversion** (dùng mapper)
3. **Không gộp reactions/mentions vào metadata**
4. **Không cast without null checks**
5. **Không quên inject mapper**

---

## 📚 Files Created

1. `src/main/java/com/chattrix/api/dto/MessageMetadata.java` - DTO wrapper
2. `src/main/java/com/chattrix/api/mappers/MessageMetadataMapper.java` - MapStruct mapper
3. `DATABASE-JSONB-STRUCTURE.md` - Complete JSONB documentation
4. `METADATA-REFACTOR-GUIDE.md` - Refactoring guide
5. `METADATA-DTO-SUMMARY.md` - Summary (old approach)
6. `METADATA-IMPLEMENTATION-FINAL.md` - This file

---

## ✨ Summary

Đã implement type-safe metadata handling với:
- ✅ MessageMetadata DTO trong `dto/` package
- ✅ MessageMetadataMapper với MapStruct + CDI
- ✅ Refactored SystemMessageService làm example
- ✅ Complete documentation về JSONB structure
- ✅ Giữ nguyên reactions và mentions columns (đúng design)
- ✅ Event có RSVP responses
- ✅ 100% backward compatible
- ✅ Build thành công

**Next:** Refactor các services còn lại theo pattern này để loại bỏ hoàn toàn `Map.put()`.
