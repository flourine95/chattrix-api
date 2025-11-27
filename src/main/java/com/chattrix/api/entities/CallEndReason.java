package com.chattrix.api.entities;


public enum CallEndReason {
    USER_HANGUP,        // Người dùng bấm tắt (Bình thường)
    NETWORK_DISCONNECT, // Mất mạng
    DEVICE_ERROR,       // Lỗi thiết bị
    TIMEOUT,            // Hết giờ (Client tự ngắt)
    UNKNOWN
}
