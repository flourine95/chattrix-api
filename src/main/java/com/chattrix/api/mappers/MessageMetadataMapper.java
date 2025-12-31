package com.chattrix.api.mappers;

import com.chattrix.api.dto.MessageMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper cho MessageMetadata ↔ Map<String, Object>
 * 
 * Usage:
 * 
 * // DTO → Map (để lưu vào database)
 * MessageMetadata dto = MessageMetadata.builder()...build();
 * Map<String, Object> map = mapper.toMap(dto);
 * message.setMetadata(map);
 * 
 * // Map → DTO (để đọc từ database)
 * Map<String, Object> map = message.getMetadata();
 * MessageMetadata dto = mapper.fromMap(map);
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface MessageMetadataMapper {
    
    /**
     * Convert MessageMetadata DTO to Map for database storage
     */
    default Map<String, Object> toMap(MessageMetadata metadata) {
        if (metadata == null) return new HashMap<>();
        
        Map<String, Object> map = new HashMap<>();
        
        // Media fields
        if (metadata.getMediaUrl() != null) map.put("mediaUrl", metadata.getMediaUrl());
        if (metadata.getThumbnailUrl() != null) map.put("thumbnailUrl", metadata.getThumbnailUrl());
        if (metadata.getFileName() != null) map.put("fileName", metadata.getFileName());
        if (metadata.getFileSize() != null) map.put("fileSize", metadata.getFileSize());
        if (metadata.getDuration() != null) map.put("duration", metadata.getDuration());
        
        // Location fields
        if (metadata.getLatitude() != null) map.put("latitude", metadata.getLatitude());
        if (metadata.getLongitude() != null) map.put("longitude", metadata.getLongitude());
        if (metadata.getLocationName() != null) map.put("locationName", metadata.getLocationName());
        
        // System message fields
        if (metadata.getKickedBy() != null) map.put("kickedBy", metadata.getKickedBy());
        if (metadata.getAddedBy() != null) map.put("addedBy", metadata.getAddedBy());
        if (metadata.getAddedUserIds() != null) map.put("addedUserIds", metadata.getAddedUserIds());
        if (metadata.getPromotedBy() != null) map.put("promotedBy", metadata.getPromotedBy());
        if (metadata.getDemotedBy() != null) map.put("demotedBy", metadata.getDemotedBy());
        if (metadata.getMutedBy() != null) map.put("mutedBy", metadata.getMutedBy());
        if (metadata.getUnmutedBy() != null) map.put("unmutedBy", metadata.getUnmutedBy());
        if (metadata.getInvitedBy() != null) map.put("invitedBy", metadata.getInvitedBy());
        if (metadata.getOldName() != null) map.put("oldName", metadata.getOldName());
        if (metadata.getNewName() != null) map.put("newName", metadata.getNewName());
        if (metadata.getMutedUntil() != null) map.put("mutedUntil", metadata.getMutedUntil());
        if (metadata.getFailedReason() != null) map.put("failedReason", metadata.getFailedReason());
        
        // Nested objects
        if (metadata.getPoll() != null) map.put("poll", metadata.getPoll());
        if (metadata.getEvent() != null) map.put("event", metadata.getEvent());
        
        return map;
    }
    
    /**
     * Convert Map from database to MessageMetadata DTO
     */
    default MessageMetadata fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return new MessageMetadata();
        
        return MessageMetadata.builder()
                // Media
                .mediaUrl(getString(map, "mediaUrl"))
                .thumbnailUrl(getString(map, "thumbnailUrl"))
                .fileName(getString(map, "fileName"))
                .fileSize(getLong(map, "fileSize"))
                .duration(getInteger(map, "duration"))
                // Location
                .latitude(getDouble(map, "latitude"))
                .longitude(getDouble(map, "longitude"))
                .locationName(getString(map, "locationName"))
                // System
                .kickedBy(getLong(map, "kickedBy"))
                .addedBy(getLong(map, "addedBy"))
                .addedUserIds(getList(map, "addedUserIds"))
                .promotedBy(getLong(map, "promotedBy"))
                .demotedBy(getLong(map, "demotedBy"))
                .mutedBy(getLong(map, "mutedBy"))
                .unmutedBy(getLong(map, "unmutedBy"))
                .invitedBy(getLong(map, "invitedBy"))
                .oldName(getString(map, "oldName"))
                .newName(getString(map, "newName"))
                .mutedUntil(getLong(map, "mutedUntil"))
                .failedReason(getString(map, "failedReason"))
                // Nested
                .poll(map.get("poll"))
                .event(map.get("event"))
                .build();
    }
    
    // ==================== Helper Methods ====================
    
    default String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    default Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    default Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    default Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    default <T> List<T> getList(Map<String, Object> map, String key) {
        return (List<T>) map.get(key);
    }
}
