# 🎂 Birthday Feature - Client Implementation Guide

## 📋 Tổng quan

Backend đã implement **2 cách** để handle birthday wishes:
1. **System Messages** - Tự động gửi lúc 00:00 vào GROUP conversations
2. **Manual Wishes** - User chủ động gửi qua API

Client có thể implement theo 3 levels:
- **Level 1 (Minimal):** Chỉ hiển thị system messages - 0 giờ
- **Level 2 (Basic):** Banner + Quick send - 2-3 giờ  
- **Level 3 (Advanced):** Full UI với stickers, animations - 5-6 giờ

---

## 🎯 Backend APIs Available

### 1. GET /v1/birthdays/today
Lấy danh sách users có sinh nhật hôm nay

**Request:**
```bash
GET /v1/birthdays/today
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "userId": 123,
    "username": "john_doe",
    "fullName": "John Doe",
    "avatarUrl": "https://...",
    "dateOfBirth": "1995-12-21T00:00:00Z",
    "age": 28,
    "birthdayMessage": "Hôm nay"
  }
]
```

### 2. GET /v1/birthdays/upcoming?days=7
Lấy danh sách users có sinh nhật trong N ngày tới

**Request:**
```bash
GET /v1/birthdays/upcoming?days=7
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "userId": 124,
    "username": "jane_smith",
    "fullName": "Jane Smith",
    "avatarUrl": "https://...",
    "dateOfBirth": "1992-12-23T00:00:00Z",
    "age": 31,
    "birthdayMessage": "Còn 2 ngày"
  }
]
```

### 3. POST /v1/birthdays/send-wishes
Gửi lời chúc sinh nhật

**Request:**
```bash
POST /v1/birthdays/send-wishes
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 123,
  "conversationIds": [1, 2, 3],
  "customMessage": "Chúc mừng sinh nhật! 🎉"  // Optional
}
```

**Response:**
```json
"Birthday wishes sent successfully"
```

---

## 🎨 Level 1: Minimal Implementation (0 giờ)

### Không cần code gì!

System messages sẽ tự động xuất hiện trong chat như tin nhắn thường.

**Chỉ cần:**
1. Hiển thị messages với type `SYSTEM` khác biệt
2. Render mentions (@username) đúng cách

**Example UI:**
```
┌─────────────────────────────────────────────┐
│ [System Message - Yellow background]        │
│ 🎂 Hôm nay là sinh nhật của @John (28 tuổi)│
│    Hãy cùng chúc mừng nhé! 🎉🎈             │
└─────────────────────────────────────────────┘
```

**Flutter Code:**
```dart
Widget buildMessage(Message message) {
  if (message.type == MessageType.SYSTEM) {
    return Container(
      padding: EdgeInsets.all(12),
      margin: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      decoration: BoxDecoration(
        color: Colors.yellow[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.yellow[300]!),
      ),
      child: Row(
        children: [
          Text('🎂', style: TextStyle(fontSize: 20)),
          SizedBox(width: 8),
          Expanded(
            child: RichText(
              text: TextSpan(
                text: message.content,
                style: TextStyle(color: Colors.black87),
                // Handle mentions here
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  // Regular message UI
  return buildRegularMessage(message);
}
```

---

## 🎨 Level 2: Basic Implementation (2-3 giờ)

### Features:
- ✅ Birthday banner notification
- ✅ Quick send wishes dialog
- ✅ Birthday badge on avatar
- ✅ API integration

### Step 1: Create Birthday Service

```dart
// lib/services/birthday_service.dart
import 'package:dio/dio.dart';

class BirthdayService {
  final Dio _dio;
  
  BirthdayService(this._dio);
  
  Future<List<BirthdayUser>> getTodayBirthdays() async {
    try {
      final response = await _dio.get('/v1/birthdays/today');
      return (response.data as List)
          .map((json) => BirthdayUser.fromJson(json))
          .toList();
    } catch (e) {
      print('Error fetching birthdays: $e');
      return [];
    }
  }
  
  Future<List<BirthdayUser>> getUpcomingBirthdays({int days = 7}) async {
    try {
      final response = await _dio.get(
        '/v1/birthdays/upcoming',
        queryParameters: {'days': days},
      );
      return (response.data as List)
          .map((json) => BirthdayUser.fromJson(json))
          .toList();
    } catch (e) {
      print('Error fetching upcoming birthdays: $e');
      return [];
    }
  }
  
  Future<void> sendBirthdayWishes({
    required int userId,
    required List<int> conversationIds,
    String? customMessage,
  }) async {
    await _dio.post('/v1/birthdays/send-wishes', data: {
      'userId': userId,
      'conversationIds': conversationIds,
      if (customMessage != null) 'customMessage': customMessage,
    });
  }
}
```

### Step 2: Create Birthday Models

```dart
// lib/models/birthday_user.dart
class BirthdayUser {
  final int userId;
  final String username;
  final String fullName;
  final String? avatarUrl;
  final DateTime? dateOfBirth;
  final int? age;
  final String birthdayMessage; // "Hôm nay", "Còn 2 ngày"
  
  BirthdayUser({
    required this.userId,
    required this.username,
    required this.fullName,
    this.avatarUrl,
    this.dateOfBirth,
    this.age,
    required this.birthdayMessage,
  });
  
  factory BirthdayUser.fromJson(Map<String, dynamic> json) {
    return BirthdayUser(
      userId: json['userId'],
      username: json['username'],
      fullName: json['fullName'],
      avatarUrl: json['avatarUrl'],
      dateOfBirth: json['dateOfBirth'] != null 
          ? DateTime.parse(json['dateOfBirth']) 
          : null,
      age: json['age'],
      birthdayMessage: json['birthdayMessage'],
    );
  }
  
  bool get isBirthdayToday => birthdayMessage == 'Hôm nay';
}
```

### Step 3: Create Birthday Banner Widget

```dart
// lib/widgets/birthday_banner.dart
import 'package:flutter/material.dart';

class BirthdayBanner extends StatelessWidget {
  final List<BirthdayUser> users;
  final VoidCallback onTap;
  
  const BirthdayBanner({
    Key? key,
    required this.users,
    required this.onTap,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    if (users.isEmpty) return SizedBox.shrink();
    
    final todayBirthdays = users.where((u) => u.isBirthdayToday).toList();
    if (todayBirthdays.isEmpty) return SizedBox.shrink();
    
    return Material(
      color: Colors.yellow[100],
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              Container(
                padding: EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                ),
                child: Text('🎂', style: TextStyle(fontSize: 24)),
              ),
              SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      todayBirthdays.length == 1
                          ? 'Hôm nay là sinh nhật của ${todayBirthdays[0].fullName}!'
                          : 'Hôm nay có ${todayBirthdays.length} người sinh nhật!',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Nhấn để gửi lời chúc',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.black54,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: Colors.black54),
            ],
          ),
        ),
      ),
    );
  }
}
```

### Step 4: Create Birthday Wishes Dialog

```dart
// lib/widgets/birthday_wishes_dialog.dart
import 'package:flutter/material.dart';

class BirthdayWishesDialog extends StatefulWidget {
  final BirthdayUser user;
  final List<Conversation> conversations;
  final Function(List<int> conversationIds, String? message) onSend;
  
  const BirthdayWishesDialog({
    Key? key,
    required this.user,
    required this.conversations,
    required this.onSend,
  }) : super(key: key);
  
  @override
  _BirthdayWishesDialogState createState() => _BirthdayWishesDialogState();
}

class _BirthdayWishesDialogState extends State<BirthdayWishesDialog> {
  final Set<int> _selectedConversations = {};
  final TextEditingController _messageController = TextEditingController();
  bool _isLoading = false;
  
  final List<String> _templates = [
    '🎂 Chúc mừng sinh nhật! 🎉',
    '🎈 Happy Birthday! Chúc bạn tuổi mới vui vẻ! 🎊',
    '🎁 Sinh nhật vui vẻ! Chúc bạn luôn hạnh phúc! 💝',
    '🎉 Chúc mừng sinh nhật! Tuổi mới nhiều niềm vui! 🎂',
  ];
  
  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Row(
        children: [
          Text('🎂'),
          SizedBox(width: 8),
          Expanded(
            child: Text(
              'Gửi lời chúc cho ${widget.user.fullName}',
              style: TextStyle(fontSize: 18),
            ),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // User info
            Container(
              padding: EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  CircleAvatar(
                    backgroundImage: widget.user.avatarUrl != null
                        ? NetworkImage(widget.user.avatarUrl!)
                        : null,
                    child: widget.user.avatarUrl == null
                        ? Text(widget.user.fullName[0])
                        : null,
                  ),
                  SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.user.fullName,
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        if (widget.user.age != null)
                          Text(
                            '${widget.user.age} tuổi',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey[600],
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 16),
            
            // Select conversations
            Text(
              'Chọn nhóm chat:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 8),
            Container(
              constraints: BoxConstraints(maxHeight: 200),
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: widget.conversations.length,
                itemBuilder: (context, index) {
                  final conv = widget.conversations[index];
                  final isSelected = _selectedConversations.contains(conv.id);
                  
                  return CheckboxListTile(
                    value: isSelected,
                    onChanged: (value) {
                      setState(() {
                        if (value == true) {
                          _selectedConversations.add(conv.id);
                        } else {
                          _selectedConversations.remove(conv.id);
                        }
                      });
                    },
                    title: Text(conv.name ?? 'Conversation'),
                    secondary: CircleAvatar(
                      backgroundImage: conv.avatarUrl != null
                          ? NetworkImage(conv.avatarUrl!)
                          : null,
                    ),
                  );
                },
              ),
            ),
            SizedBox(height: 16),
            
            // Message templates
            Text(
              'Chọn mẫu tin nhắn:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _templates.map((template) {
                return ActionChip(
                  label: Text(template),
                  onPressed: () {
                    _messageController.text = template;
                  },
                );
              }).toList(),
            ),
            SizedBox(height: 16),
            
            // Custom message
            TextField(
              controller: _messageController,
              decoration: InputDecoration(
                labelText: 'Hoặc nhập lời chúc của bạn',
                hintText: 'Nhập lời chúc...',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isLoading ? null : () => Navigator.pop(context),
          child: Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: _isLoading || _selectedConversations.isEmpty
              ? null
              : _sendWishes,
          child: _isLoading
              ? SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text('Gửi'),
        ),
      ],
    );
  }
  
  Future<void> _sendWishes() async {
    setState(() => _isLoading = true);
    
    try {
      await widget.onSend(
        _selectedConversations.toList(),
        _messageController.text.isEmpty ? null : _messageController.text,
      );
      
      Navigator.pop(context);
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('✅ Đã gửi lời chúc thành công!'),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('❌ Lỗi: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() => _isLoading = false);
    }
  }
  
  @override
  void dispose() {
    _messageController.dispose();
    super.dispose();
  }
}
```

### Step 5: Integrate into Home Screen

```dart
// lib/screens/home_screen.dart
class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<BirthdayUser> _birthdayUsers = [];
  bool _isLoadingBirthdays = false;
  
  @override
  void initState() {
    super.initState();
    _loadBirthdays();
  }
  
  Future<void> _loadBirthdays() async {
    setState(() => _isLoadingBirthdays = true);
    
    try {
      final birthdayService = context.read<BirthdayService>();
      final users = await birthdayService.getTodayBirthdays();
      
      setState(() {
        _birthdayUsers = users;
        _isLoadingBirthdays = false;
      });
    } catch (e) {
      print('Error loading birthdays: $e');
      setState(() => _isLoadingBirthdays = false);
    }
  }
  
  void _showBirthdayList() {
    showModalBottomSheet(
      context: context,
      builder: (context) => BirthdayListSheet(
        users: _birthdayUsers,
        onSendWishes: _showBirthdayWishesDialog,
      ),
    );
  }
  
  void _showBirthdayWishesDialog(BirthdayUser user) async {
    // Get user's conversations
    final conversations = await _getUserConversations();
    
    showDialog(
      context: context,
      builder: (context) => BirthdayWishesDialog(
        user: user,
        conversations: conversations,
        onSend: (conversationIds, message) async {
          final birthdayService = context.read<BirthdayService>();
          await birthdayService.sendBirthdayWishes(
            userId: user.userId,
            conversationIds: conversationIds,
            customMessage: message,
          );
        },
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Chattrix')),
      body: Column(
        children: [
          // Birthday banner
          BirthdayBanner(
            users: _birthdayUsers,
            onTap: _showBirthdayList,
          ),
          
          // Rest of your UI
          Expanded(
            child: ConversationList(),
          ),
        ],
      ),
    );
  }
}
```

---

## 🎨 Level 3: Advanced Implementation (5-6 giờ)

### Additional Features:
- ✅ Birthday badge on avatar in chat list
- ✅ Birthday stickers (Giphy integration)
- ✅ Birthday animations
- ✅ Upcoming birthdays screen
- ✅ Birthday countdown
- ✅ Birthday calendar view

### Feature 1: Birthday Badge on Avatar

```dart
// lib/widgets/user_avatar_with_birthday.dart
class UserAvatarWithBirthday extends StatelessWidget {
  final User user;
  final double size;
  
  const UserAvatarWithBirthday({
    Key? key,
    required this.user,
    this.size = 40,
  }) : super(key: key);
  
  bool get isBirthdayToday {
    if (user.dateOfBirth == null) return false;
    
    final now = DateTime.now();
    final birthday = user.dateOfBirth!;
    
    return now.month == birthday.month && now.day == birthday.day;
  }
  
  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        CircleAvatar(
          radius: size / 2,
          backgroundImage: user.avatarUrl != null
              ? NetworkImage(user.avatarUrl!)
              : null,
          child: user.avatarUrl == null
              ? Text(user.fullName[0])
              : null,
        ),
        
        // Birthday badge
        if (isBirthdayToday)
          Positioned(
            right: 0,
            bottom: 0,
            child: Container(
              padding: EdgeInsets.all(2),
              decoration: BoxDecoration(
                color: Colors.yellow,
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 2),
              ),
              child: Text(
                '🎂',
                style: TextStyle(fontSize: size * 0.3),
              ),
            ),
          ),
      ],
    );
  }
}
```

### Feature 2: Birthday Stickers (Giphy)

```dart
// lib/widgets/birthday_sticker_picker.dart
import 'package:giphy_get/giphy_get.dart';

class BirthdaySticker
Picker extends StatelessWidget {
  final Function(String gifUrl) onStickerSelected;
  
  const BirthdayStickerPicker({
    Key? key,
    required this.onStickerSelected,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    return ElevatedButton.icon(
      icon: Icon(Icons.gif),
      label: Text('Birthday Stickers'),
      onPressed: () async {
        final gif = await GiphyGet.getGif(
          context: context,
          apiKey: 'YOUR_GIPHY_API_KEY',
          searchText: 'happy birthday',
        );
        
        if (gif != null) {
          onStickerSelected(gif.images!.original!.url);
        }
      },
    );
  }
}
```

### Feature 3: Upcoming Birthdays Screen

```dart
// lib/screens/birthday_screen.dart
class BirthdayScreen extends StatefulWidget {
  @override
  _BirthdayScreenState createState() => _BirthdayScreenState();
}

class _BirthdayScreenState extends State<BirthdayScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<BirthdayUser> _todayBirthdays = [];
  List<BirthdayUser> _upcomingBirthdays = [];
  bool _isLoading = false;
  
  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadBirthdays();
  }
  
  Future<void> _loadBirthdays() async {
    setState(() => _isLoading = true);
    
    try {
      final birthdayService = context.read<BirthdayService>();
      
      final today = await birthdayService.getTodayBirthdays();
      final upcoming = await birthdayService.getUpcomingBirthdays(days: 30);
      
      setState(() {
        _todayBirthdays = today;
        _upcomingBirthdays = upcoming.where((u) => !u.isBirthdayToday).toList();
        _isLoading = false;
      });
    } catch (e) {
      print('Error loading birthdays: $e');
      setState(() => _isLoading = false);
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('🎂 Sinh nhật'),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(text: 'Hôm nay (${_todayBirthdays.length})'),
            Tab(text: 'Sắp tới (${_upcomingBirthdays.length})'),
          ],
        ),
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : TabBarView(
              controller: _tabController,
              children: [
                _buildTodayTab(),
                _buildUpcomingTab(),
              ],
            ),
    );
  }
  
  Widget _buildTodayTab() {
    if (_todayBirthdays.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('🎂', style: TextStyle(fontSize: 64)),
            SizedBox(height: 16),
            Text(
              'Không có ai sinh nhật hôm nay',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
          ],
        ),
      );
    }
    
    return ListView.builder(
      padding: EdgeInsets.all(16),
      itemCount: _todayBirthdays.length,
      itemBuilder: (context, index) {
        final user = _todayBirthdays[index];
        return _buildBirthdayCard(user, isToday: true);
      },
    );
  }
  
  Widget _buildUpcomingTab() {
    if (_upcomingBirthdays.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('📅', style: TextStyle(fontSize: 64)),
            SizedBox(height: 16),
            Text(
              'Không có sinh nhật sắp tới',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
          ],
        ),
      );
    }
    
    return ListView.builder(
      padding: EdgeInsets.all(16),
      itemCount: _upcomingBirthdays.length,
      itemBuilder: (context, index) {
        final user = _upcomingBirthdays[index];
        return _buildBirthdayCard(user, isToday: false);
      },
    );
  }
  
  Widget _buildBirthdayCard(BirthdayUser user, {required bool isToday}) {
    return Card(
      margin: EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: UserAvatarWithBirthday(user: user, size: 50),
        title: Text(
          user.fullName,
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (user.age != null)
              Text('${user.age} tuổi'),
            Text(
              user.birthdayMessage,
              style: TextStyle(
                color: isToday ? Colors.orange : Colors.grey,
                fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ],
        ),
        trailing: isToday
            ? ElevatedButton.icon(
                icon: Icon(Icons.cake, size: 18),
                label: Text('Gửi lời chúc'),
                onPressed: () => _showBirthdayWishesDialog(user),
              )
            : null,
      ),
    );
  }
  
  void _showBirthdayWishesDialog(BirthdayUser user) {
    // Same as Level 2
  }
  
  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }
}
```

### Feature 4: Birthday Animation

```dart
// lib/widgets/birthday_animation.dart
import 'package:confetti/confetti.dart';

class BirthdayAnimation extends StatefulWidget {
  final Widget child;
  
  const BirthdayAnimation({
    Key? key,
    required this.child,
  }) : super(key: key);
  
  @override
  _BirthdayAnimationState createState() => _BirthdayAnimationState();
}

class _BirthdayAnimationState extends State<BirthdayAnimation> {
  late ConfettiController _confettiController;
  
  @override
  void initState() {
    super.initState();
    _confettiController = ConfettiController(duration: Duration(seconds: 3));
    _confettiController.play();
  }
  
  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        widget.child,
        
        // Confetti animation
        Align(
          alignment: Alignment.topCenter,
          child: ConfettiWidget(
            confettiController: _confettiController,
            blastDirectionality: BlastDirectionality.explosive,
            emissionFrequency: 0.05,
            numberOfParticles: 20,
            colors: [
              Colors.yellow,
              Colors.orange,
              Colors.pink,
              Colors.purple,
            ],
          ),
        ),
      ],
    );
  }
  
  @override
  void dispose() {
    _confettiController.dispose();
    super.dispose();
  }
}
```

---

## 📱 UI/UX Best Practices

### 1. System Message Styling

```dart
// Distinguish system messages from user messages
if (message.type == MessageType.SYSTEM) {
  return Container(
    padding: EdgeInsets.all(12),
    margin: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
    decoration: BoxDecoration(
      color: Colors.yellow[50],
      borderRadius: BorderRadius.circular(8),
      border: Border.all(color: Colors.yellow[300]!),
    ),
    child: Row(
      children: [
        Text('🎂', style: TextStyle(fontSize: 20)),
        SizedBox(width: 8),
        Expanded(
          child: Text(
            message.content,
            style: TextStyle(
              color: Colors.black87,
              fontStyle: FontStyle.italic,
            ),
          ),
        ),
      ],
    ),
  );
}
```

### 2. Birthday Notification

```dart
// Show local notification when app is in background
void showBirthdayNotification(BirthdayUser user) {
  final FlutterLocalNotificationsPlugin notifications = 
      FlutterLocalNotificationsPlugin();
  
  notifications.show(
    user.userId,
    '🎂 Sinh nhật',
    'Hôm nay là sinh nhật của ${user.fullName}!',
    NotificationDetails(
      android: AndroidNotificationDetails(
        'birthday_channel',
        'Birthday Notifications',
        importance: Importance.high,
        priority: Priority.high,
      ),
      iOS: DarwinNotificationDetails(),
    ),
    payload: 'birthday:${user.userId}',
  );
}
```

### 3. Birthday Badge Colors

```dart
// Use consistent colors for birthday elements
class BirthdayColors {
  static const primary = Color(0xFFFFD700); // Gold
  static const background = Color(0xFFFFF9E6); // Light yellow
  static const border = Color(0xFFFFE066); // Yellow
  static const text = Color(0xFF8B6914); // Dark gold
}
```

---

## 🔄 WebSocket Integration

### Handle Birthday System Messages

```dart
// lib/services/websocket_service.dart
class WebSocketService {
  void handleMessage(WebSocketMessage message) {
    switch (message.type) {
      case 'chat.message':
        final messageDto = OutgoingMessageDto.fromJson(message.payload);
        
        // Check if it's a birthday system message
        if (messageDto.type == MessageType.SYSTEM && 
            messageDto.content.contains('sinh nhật')) {
          // Show special UI for birthday messages
          _showBirthdayMessageNotification(messageDto);
        }
        
        // Add to chat
        _addMessageToChat(messageDto);
        break;
        
      // ... other cases
    }
  }
  
  void _showBirthdayMessageNotification(OutgoingMessageDto message) {
    // Show in-app notification or animation
    Get.snackbar(
      '🎂 Sinh nhật',
      message.content,
      backgroundColor: BirthdayColors.background,
      colorText: BirthdayColors.text,
      duration: Duration(seconds: 5),
    );
  }
}
```

---

## 🧪 Testing

### Unit Tests

```dart
// test/services/birthday_service_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';

void main() {
  group('BirthdayService', () {
    late BirthdayService birthdayService;
    late MockDio mockDio;
    
    setUp(() {
      mockDio = MockDio();
      birthdayService = BirthdayService(mockDio);
    });
    
    test('getTodayBirthdays returns list of users', () async {
      // Arrange
      when(mockDio.get('/v1/birthdays/today')).thenAnswer(
        (_) async => Response(
          data: [
            {
              'userId': 123,
              'username': 'john_doe',
              'fullName': 'John Doe',
              'birthdayMessage': 'Hôm nay',
            }
          ],
          statusCode: 200,
          requestOptions: RequestOptions(path: ''),
        ),
      );
      
      // Act
      final result = await birthdayService.getTodayBirthdays();
      
      // Assert
      expect(result.length, 1);
      expect(result[0].fullName, 'John Doe');
      expect(result[0].isBirthdayToday, true);
    });
    
    test('sendBirthdayWishes calls API correctly', () async {
      // Arrange
      when(mockDio.post(any, data: anyNamed('data'))).thenAnswer(
        (_) async => Response(
          data: 'Birthday wishes sent successfully',
          statusCode: 200,
          requestOptions: RequestOptions(path: ''),
        ),
      );
      
      // Act
      await birthdayService.sendBirthdayWishes(
        userId: 123,
        conversationIds: [1, 2],
        customMessage: 'Happy Birthday!',
      );
      
      // Assert
      verify(mockDio.post(
        '/v1/birthdays/send-wishes',
        data: {
          'userId': 123,
          'conversationIds': [1, 2],
          'customMessage': 'Happy Birthday!',
        },
      )).called(1);
    });
  });
}
```

### Widget Tests

```dart
// test/widgets/birthday_banner_test.dart
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('BirthdayBanner shows when users have birthday', 
      (WidgetTester tester) async {
    // Arrange
    final users = [
      BirthdayUser(
        userId: 123,
        username: 'john',
        fullName: 'John Doe',
        birthdayMessage: 'Hôm nay',
      ),
    ];
    
    // Act
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: BirthdayBanner(
            users: users,
            onTap: () {},
          ),
        ),
      ),
    );
    
    // Assert
    expect(find.text('🎂'), findsOneWidget);
    expect(find.textContaining('John Doe'), findsOneWidget);
  });
  
  testWidgets('BirthdayBanner hides when no birthdays', 
      (WidgetTester tester) async {
    // Act
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: BirthdayBanner(
            users: [],
            onTap: () {},
          ),
        ),
      ),
    );
    
    // Assert
    expect(find.text('🎂'), findsNothing);
  });
}
```

---

## 📦 Dependencies

Add these to `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # HTTP & API
  dio: ^5.4.0
  
  # State Management (choose one)
  provider: ^6.1.1
  # or
  get: ^4.6.6
  
  # UI Components
  flutter_local_notifications: ^16.3.0
  
  # Optional (for Level 3)
  giphy_get: ^3.5.0  # Birthday stickers
  confetti: ^0.7.0   # Birthday animation
  
dev_dependencies:
  flutter_test:
    sdk: flutter
  mockito: ^5.4.4
  build_runner: ^2.4.7
```

---

## 🚀 Implementation Checklist

### Level 1 (Minimal) - 0 giờ
- [ ] Style system messages differently
- [ ] Render mentions correctly
- [ ] Test with backend system messages

### Level 2 (Basic) - 2-3 giờ
- [ ] Create BirthdayService
- [ ] Create BirthdayUser model
- [ ] Create BirthdayBanner widget
- [ ] Create BirthdayWishesDialog
- [ ] Integrate into HomeScreen
- [ ] Test API integration
- [ ] Handle errors gracefully

### Level 3 (Advanced) - 5-6 giờ
- [ ] Add birthday badge on avatars
- [ ] Integrate Giphy for stickers
- [ ] Add birthday animations
- [ ] Create BirthdayScreen with tabs
- [ ] Add birthday countdown
- [ ] Add local notifications
- [ ] Add birthday calendar view
- [ ] Polish UI/UX

---

## 🐛 Common Issues & Solutions

### Issue 1: Birthday not showing today
**Problem:** User's birthday is today but not appearing in list

**Solution:**
```dart
// Check timezone handling
final now = DateTime.now();
final birthday = DateTime.parse(user.dateOfBirth);

// Compare only month and day
final isBirthday = now.month == birthday.month && 
                   now.day == birthday.day;
```

### Issue 2: API returns 401 Unauthorized
**Problem:** Birthday APIs require authentication

**Solution:**
```dart
// Ensure JWT token is included in headers
final dio = Dio();
dio.interceptors.add(
  InterceptorsWrapper(
    onRequest: (options, handler) {
      final token = getStoredToken();
      options.headers['Authorization'] = 'Bearer $token';
      return handler.next(options);
    },
  ),
);
```

### Issue 3: System messages not styled
**Problem:** Birthday system messages look like regular messages

**Solution:**
```dart
// Check message type in your message builder
if (message.type == 'SYSTEM') {
  return buildSystemMessage(message);
} else {
  return buildRegularMessage(message);
}
```

---

## 📞 Support & Resources

### Backend APIs
- Swagger/OpenAPI: `http://localhost:8080/swagger-ui`
- API Spec: `api-spec.yaml`

### Documentation
- Backend Implementation: `.kiro/specs/birthday-feature/IMPLEMENTATION_COMPLETE.md`
- API Summary: `BIRTHDAY_FEATURE_SUMMARY.md`

### Contact
- Backend Team: [Your contact]
- Frontend Team: [Your contact]

---

## ✨ Tips & Best Practices

1. **Cache birthday data** - Don't fetch on every screen load
2. **Handle timezones** - Use UTC for dateOfBirth
3. **Optimize images** - Use cached_network_image for avatars
4. **Error handling** - Show friendly messages when API fails
5. **Loading states** - Show skeleton loaders while fetching
6. **Empty states** - Show nice UI when no birthdays
7. **Accessibility** - Add semantic labels for screen readers
8. **Testing** - Write tests for critical flows
9. **Performance** - Lazy load birthday list if many users
10. **Analytics** - Track birthday wishes sent

---

**Happy Coding! 🎂🎉**
