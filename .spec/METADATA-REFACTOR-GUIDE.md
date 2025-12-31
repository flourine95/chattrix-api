# Metadata Refactoring Guide

## Vấn đề với cách cũ

### ❌ Cách cũ (không type-safe):
```java
// Dễ typo, không có autocomplete, không có validation
Map<String, Object> metadata = new HashMap<>();
metadata.put("mediaUrl", url);        // Có thể typo thành "mediaURL"
metadata.put("fileSize", size);       // Có thể put sai kiểu dữ liệu
metadata.put("duraton", duration);    // Typo: duraton thay vì duration
```

### ✅ Cách mới (type-safe):
```java
// Type-safe, có autocomplete, có validation
MediaMetadata media = MediaMetadata.builder()
    .mediaUrl(url)
    .fileSize(size)
    .duration(duration)
    .build();

Map<String, Object> metadata = MetadataUtils.toMap(media);
```

## Các DTO Metadata đã tạo

### 1. MediaMetadata
Dùng cho: IMAGE, VIDEO, AUDIO, FILE
```java
MediaMetadata media = MediaMetadata.builder()
    .mediaUrl("https://...")
    .thumbnailUrl("https://...")
    .fileName("document.pdf")
    .fileSize(1024000L)
    .duration(120) // seconds
    .build();
```

### 2. LocationMetadata
Dùng cho: LOCATION
```java
LocationMetadata location = LocationMetadata.builder()
    .latitude(10.762622)
    .longitude(106.660172)
    .locationName("Saigon Centre")
    .build();
```

### 3. SystemMessageMetadata
Dùng cho: System messages (user_kicked, user_added, etc.)
```java
SystemMessageMetadata system = SystemMessageMetadata.builder()
    .kickedBy(userId)
    .failedReason("User has left")
    .build();
```

## Ví dụ Refactoring

### MessageService - Trước khi refactor:
```java
Map<String, Object> metadata = new HashMap<>();
if (request.mediaUrl() != null) metadata.put("mediaUrl", request.mediaUrl());
if (request.thumbnailUrl() != null) metadata.put("thumbnailUrl", request.thumbnailUrl());
if (request.fileName() != null) metadata.put("fileName", request.fileName());
if (request.fileSize() != null) metadata.put("fileSize", request.fileSize());
if (request.duration() != null) metadata.put("duration", request.duration());
if (request.latitude() != null) metadata.put("latitude", request.latitude());
if (request.longitude() != null) metadata.put("longitude", request.longitude());
if (request.locationName() != null) metadata.put("locationName", request.locationName());
message.setMetadata(metadata);
```

### MessageService - Sau khi refactor:
```java
// Build media metadata
MediaMetadata media = MediaMetadata.builder()
    .mediaUrl(request.mediaUrl())
    .thumbnailUrl(request.thumbnailUrl())
    .fileName(request.fileName())
    .fileSize(request.fileSize())
    .duration(request.duration())
    .build();

// Build location metadata
LocationMetadata location = LocationMetadata.builder()
    .latitude(request.latitude())
    .longitude(request.longitude())
    .locationName(request.locationName())
    .build();

// Merge and set
Map<String, Object> metadata = MetadataUtils.mergeMetadata(media, location);
message.setMetadata(metadata);
```

### SystemMessageService - Trước khi refactor:
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("kickedBy", kickedByUserId);
return createSystemMessage(conversationId, kickedUserId, "user_kicked", metadata);
```

### SystemMessageService - Sau khi refactor:
```java
SystemMessageMetadata system = SystemMessageMetadata.builder()
    .kickedBy(kickedByUserId)
    .build();

Map<String, Object> metadata = MetadataUtils.toMap(system);
return createSystemMessage(conversationId, kickedUserId, "user_kicked", metadata);
```

## Lợi ích

1. **Type Safety**: Compiler kiểm tra kiểu dữ liệu
2. **Autocomplete**: IDE gợi ý các fields
3. **Refactoring**: Đổi tên field tự động update toàn bộ code
4. **Documentation**: DTO classes là documentation
5. **Validation**: Có thể thêm Bean Validation annotations
6. **Testing**: Dễ mock và test hơn

## Migration Plan

### Phase 1: Refactor Services (Ưu tiên cao)
- [ ] MessageService
- [ ] ScheduledMessageService  
- [ ] SystemMessageService
- [ ] PollService
- [ ] EventService

### Phase 2: Refactor WebSocket
- [ ] ChatServerEndpoint

### Phase 3: Refactor Other Services
- [ ] GroupInviteLinkService

### Phase 4: Cleanup
- [ ] Remove old metadata.put() patterns
- [ ] Add validation annotations to metadata DTOs
- [ ] Update documentation

## Cách sử dụng MetadataUtils

### Convert DTO → Map (để lưu vào database):
```java
MediaMetadata media = MediaMetadata.builder()...build();
Map<String, Object> map = MetadataUtils.toMap(media);
message.setMetadata(map);
```

### Convert Map → DTO (để đọc từ database):
```java
Map<String, Object> map = message.getMetadata();
MediaMetadata media = MetadataUtils.toMediaMetadata(map);
if (media != null) {
    String url = media.getMediaUrl();
    Long size = media.getFileSize();
}
```

### Merge nhiều metadata types:
```java
MediaMetadata media = ...;
LocationMetadata location = ...;
Map<String, Object> merged = MetadataUtils.mergeMetadata(media, location);
```

## Lưu ý

- Các DTO này chỉ dùng cho **internal processing**, không expose ra API
- Database vẫn lưu dạng `Map<String, Object>` (JSONB)
- Chỉ thay đổi cách **tạo và đọc** metadata, không thay đổi database schema
- Backward compatible: Code cũ vẫn hoạt động bình thường
