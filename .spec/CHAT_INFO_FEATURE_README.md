# Chat Info Feature - Hướng dẫn sử dụng

## 📋 Tổng quan

Tính năng **Chat Info** (Thông tin hội thoại) cho phép người dùng xem và quản lý thông tin chi tiết của cuộc trò chuyện, bao gồm:

- ✅ Thông tin cơ bản (avatar, tên, trạng thái)
- ✅ Quản lý media (ảnh, video, file, audio)
- ✅ Tìm kiếm tin nhắn với filter và sort
- ✅ Quản lý thành viên (cho group chat)
- ✅ Cài đặt cuộc trò chuyện (notifications, mute, block, delete)

---

## 🎯 Cách sử dụng

### 1. Mở Chat Info Page

Có 2 cách để mở trang thông tin hội thoại:

**Cách 1:** Click vào avatar/tên trong AppBar của ChatViewPage
```dart
// Tự động navigate khi click vào title
```

**Cách 2:** Click vào nút info icon ở góc phải AppBar
```dart
IconButton(
  icon: const Icon(Icons.info_outline),
  onPressed: () {
    if (conversation != null) {
      context.push('/chat-info', extra: conversation);
    }
  },
)
```

---

### 2. Các Tab trong Chat Info

#### Tab 1: Tổng quan (Overview)
Hiển thị các cài đặt và tùy chỉnh:
- Chủ đề & màu sắc
- Biệt danh (Direct chat)
- Đổi ảnh/tên nhóm (Group chat)
- Bật/tắt thông báo
- Tắt tiếng
- Chặn người dùng (Direct chat)
- Báo cáo
- Rời nhóm (Group chat)
- Xóa cuộc trò chuyện

#### Tab 2: Media
Hiển thị tất cả media đã chia sẻ:
- Filter theo loại: Tất cả, Ảnh, Video, File, Audio
- Grid view 3 cột
- Click để xem full screen
- Tự động load từ messages hiện có

#### Tab 3: Tìm kiếm
Tìm kiếm tin nhắn trong cuộc trò chuyện:
- Tìm kiếm theo nội dung
- Filter theo loại tin nhắn
- Sort theo thời gian (mới nhất/cũ nhất)
- Highlight từ khóa tìm kiếm
- Click để quay lại chat và jump đến tin nhắn

#### Tab 4: Thành viên (Group only)
Quản lý thành viên nhóm:
- Tìm kiếm thành viên
- Hiển thị role (Admin/Member)
- Hiển thị trạng thái online/offline
- Thêm thành viên (Admin)
- Xóa thành viên (Admin)
- Đặt/gỡ quyền Admin (Admin)

---

## 🏗️ Cấu trúc Code

### Files đã tạo:

```
lib/features/chat/presentation/
├── pages/
│   └── chat_info_page.dart                    # Main page với tabs
└── widgets/
    └── chat_info/
        ├── chat_info_header.dart              # Header với avatar, name, quick actions
        ├── media_grid_widget.dart             # Media grid với filter
        ├── media_grid_item.dart               # Media item component
        ├── message_search_widget.dart         # Search với filter & sort
        ├── settings_section_widget.dart       # Settings & customization
        └── members_list_widget.dart           # Members management (group)
```

### Router:
```dart
// lib/core/router/app_router.dart
GoRoute(
  path: '/chat-info',
  name: 'chat-info',
  builder: (context, state) {
    final conversation = state.extra as Conversation;
    return ChatInfoPage(conversation: conversation);
  },
),
```

---

## 🔧 Tích hợp API

### Hiện tại:
- ✅ Sử dụng dữ liệu từ `messagesProvider` để hiển thị media
- ✅ Sử dụng dữ liệu từ `conversation.participants` để hiển thị members
- ✅ UI hoàn chỉnh với tất cả tính năng

### Cần backend implement:
Xem file `.spec/CHAT_INFO_API_PROPOSAL.md` để biết chi tiết các API cần thiết:

1. **Conversation Management:**
   - `PUT /v1/conversations/{id}` - Update conversation
   - `DELETE /v1/conversations/{id}` - Delete conversation
   - `POST /v1/conversations/{id}/leave` - Leave group

2. **Members Management:**
   - `POST /v1/conversations/{id}/members` - Add members
   - `DELETE /v1/conversations/{id}/members/{userId}` - Remove member
   - `PUT /v1/conversations/{id}/members/{userId}/role` - Update role

3. **Settings:**
   - `GET /v1/conversations/{id}/settings` - Get settings
   - `PUT /v1/conversations/{id}/settings` - Update settings
   - `POST /v1/conversations/{id}/mute` - Mute/unmute
   - `POST /v1/conversations/{id}/block` - Block user (direct)

4. **Search & Media:**
   - `GET /v1/conversations/{id}/messages/search` - Search messages
   - `GET /v1/conversations/{id}/media` - Get media files
   - `GET /v1/conversations/{id}/links` - Get shared links

---

## 🎨 Customization

### Thay đổi màu sắc:
```dart
// Trong _showThemeCustomization
final colors = [
  Colors.blue,
  Colors.green,
  Colors.red,
  Colors.purple,
  Colors.orange,
];
```

### Thay đổi số cột trong Media Grid:
```dart
// Trong MediaGridWidget
gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
  crossAxisCount: 3, // Thay đổi số này
  crossAxisSpacing: 8,
  mainAxisSpacing: 8,
),
```

---

## 🐛 Known Issues & TODOs

### TODOs:
- [ ] Implement API integration khi backend ready
- [ ] Implement full-screen media viewer
- [ ] Implement scroll to message khi click search result
- [ ] Implement theme customization persistence
- [ ] Implement add member dialog với user search
- [ ] Implement call & video call features

### Notes:
- Tất cả các actions hiện tại đều có placeholder dialogs
- Cần thêm error handling khi integrate API
- Cần thêm loading states cho async operations

---

## 📱 Screenshots

### Chat Info Header
- Avatar lớn ở giữa
- Tên cuộc trò chuyện
- Số thành viên (group) hoặc trạng thái online (direct)
- Quick actions: Call, Video, Mute

### Media Tab
- Filter chips: Tất cả, Ảnh, Video, File, Audio
- Grid 3x3 với thumbnails
- Play icon cho video
- Duration badge cho video/audio

### Search Tab
- Search bar với clear button
- Dropdown filter theo loại tin nhắn
- Sort button (ASC/DESC)
- Search results với highlighted keywords
- Message type badges

### Members Tab (Group)
- Search bar
- Add member button (Admin only)
- Member list với avatar, name, role badge
- Online status indicator
- More menu cho Admin actions

---

## 🚀 Next Steps

1. **Backend Development:**
   - Implement các API endpoints theo proposal
   - Test với Postman/Thunder Client

2. **Frontend Integration:**
   - Tạo use cases cho các API mới
   - Tạo providers cho state management
   - Integrate API vào widgets

3. **Testing:**
   - Unit tests cho widgets
   - Integration tests cho flows
   - E2E tests

4. **Polish:**
   - Animations & transitions
   - Error handling
   - Loading states
   - Empty states

---

**Version:** 1.0.0  
**Last Updated:** 2025-11-05
