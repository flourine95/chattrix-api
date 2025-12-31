package com.chattrix.api.utils;

import com.chattrix.api.dto.metadata.LocationMetadata;
import com.chattrix.api.dto.metadata.MediaMetadata;
import com.chattrix.api.dto.metadata.SystemMessageMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting between metadata DTOs and Map<String, Object>
 * This provides type-safe access to metadata while maintaining JSONB compatibility
 */
public class MetadataUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // ==================== Media Metadata ====================
    
    public static Map<String, Object> toMap(MediaMetadata media) {
        if (media == null) return new HashMap<>();
        
        Map<String, Object> map = new HashMap<>();
        if (media.getMediaUrl() != null) map.put("mediaUrl", media.getMediaUrl());
        if (media.getThumbnailUrl() != null) map.put("thumbnailUrl", media.getThumbnailUrl());
        if (media.getFileName() != null) map.put("fileName", media.getFileName());
        if (media.getFileSize() != null) map.put("fileSize", media.getFileSize());
        if (media.getDuration() != null) map.put("duration", media.getDuration());
        return map;
    }
    
    public static MediaMetadata toMediaMetadata(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        
        return MediaMetadata.builder()
                .mediaUrl(getString(map, "mediaUrl"))
                .thumbnailUrl(getString(map, "thumbnailUrl"))
                .fileName(getString(map, "fileName"))
                .fileSize(getLong(map, "fileSize"))
                .duration(getInteger(map, "duration"))
                .build();
    }
    
    // ==================== Location Metadata ====================
    
    public static Map<String, Object> toMap(LocationMetadata location) {
        if (location == null) return new HashMap<>();
        
        Map<String, Object> map = new HashMap<>();
        if (location.getLatitude() != null) map.put("latitude", location.getLatitude());
        if (location.getLongitude() != null) map.put("longitude", location.getLongitude());
        if (location.getLocationName() != null) map.put("locationName", location.getLocationName());
        return map;
    }
    
    public static LocationMetadata toLocationMetadata(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        
        return LocationMetadata.builder()
                .latitude(getDouble(map, "latitude"))
                .longitude(getDouble(map, "longitude"))
                .locationName(getString(map, "locationName"))
                .build();
    }
    
    // ==================== System Message Metadata ====================
    
    public static Map<String, Object> toMap(SystemMessageMetadata system) {
        if (system == null) return new HashMap<>();
        
        Map<String, Object> map = new HashMap<>();
        if (system.getKickedBy() != null) map.put("kickedBy", system.getKickedBy());
        if (system.getAddedBy() != null) map.put("addedBy", system.getAddedBy());
        if (system.getAddedUserIds() != null) map.put("addedUserIds", system.getAddedUserIds());
        if (system.getPromotedBy() != null) map.put("promotedBy", system.getPromotedBy());
        if (system.getDemotedBy() != null) map.put("demotedBy", system.getDemotedBy());
        if (system.getMutedBy() != null) map.put("mutedBy", system.getMutedBy());
        if (system.getUnmutedBy() != null) map.put("unmutedBy", system.getUnmutedBy());
        if (system.getInvitedBy() != null) map.put("invitedBy", system.getInvitedBy());
        if (system.getOldName() != null) map.put("oldName", system.getOldName());
        if (system.getNewName() != null) map.put("newName", system.getNewName());
        if (system.getMutedUntil() != null) map.put("mutedUntil", system.getMutedUntil());
        if (system.getFailedReason() != null) map.put("failedReason", system.getFailedReason());
        return map;
    }
    
    public static SystemMessageMetadata toSystemMessageMetadata(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        
        return SystemMessageMetadata.builder()
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
                .build();
    }
    
    // ==================== Combined Metadata ====================
    
    /**
     * Merge media and location metadata into a single map
     */
    public static Map<String, Object> mergeMetadata(MediaMetadata media, LocationMetadata location) {
        Map<String, Object> map = new HashMap<>();
        if (media != null) map.putAll(toMap(media));
        if (location != null) map.putAll(toMap(location));
        return map;
    }
    
    // ==================== Helper Methods ====================
    
    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private static Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static Double getDouble(Map<String, Object> map, String key) {
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
    private static <T> T getList(Map<String, Object> map, String key) {
        return (T) map.get(key);
    }
}
