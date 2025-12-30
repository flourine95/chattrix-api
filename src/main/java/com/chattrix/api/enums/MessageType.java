package com.chattrix.api.enums;

public enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,      // Dùng chung cho cả Voice và Music tệp tin
    FILE,       // Thay cho DOCUMENT để bao quát hơn (pdf, docx, zip...)
    STICKER,    // Tin nhắn nhãn dán
    LOCATION,   // Chia sẻ vị trí

    // Nhóm tính năng tương tác (Gộp vào metadata)
    POLL,
    EVENT,

    // Nhóm hệ thống và trạng thái
    CALL,       // Hiển thị lịch sử cuộc gọi trong box chat
    SYSTEM,     // Tin nhắn hệ thống (ví dụ: "An đã thêm Bình vào nhóm")
    ANNOUNCEMENT // Tin nhắn thông báo quan trọng từ Admin
}
