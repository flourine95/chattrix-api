# 🕐 Frontend Fix: Birthday Timezone Issue

## 🐛 Problem

Khi user chọn ngày 21, sau khi lưu database hiển thị ngày 20.

**Root Cause:**
- Frontend gửi: `2024-12-21T00:00:00Z` (midnight UTC)
- Server timezone: GMT+7 (hoặc khác)
- Khi convert: `2024-12-21T00:00:00Z` → `2024-12-20T17:00:00-07:00` (nếu GMT-7)
- Database lưu: `2024-12-20` (do trừ timezone offset)

## ✅ Solution: Gửi 12:00 UTC thay vì 00:00 UTC

### Flutter/Dart Code

**❌ BAD - Causes -1 day issue:**
```dart
// DON'T DO THIS
final selectedDate = DateTime(2024, 12, 21); // Local time
final dateOfBirth = selectedDate.toUtc(); // 2024-12-21T00:00:00Z

await dio.put('/v1/profile/me', data: {
  'dateOfBirth': dateOfBirth.toIso8601String(),
});
```

**✅ GOOD - Fixes the issue:**
```dart
// DO THIS INSTEAD
final selectedDate = DateTime(2024, 12, 21); // Local time

// Set to noon (12:00) UTC to avoid timezone issues
final dateOfBirth = DateTime.utc(
  selectedDate.year,
  selectedDate.month,
  selectedDate.day,
  12, // Noon
  0,
  0,
);

await dio.put('/v1/profile/me', data: {
  'dateOfBirth': dateOfBirth.toIso8601String(), // 2024-12-21T12:00:00Z
});
```

### Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:dio/dio.dart';

class BirthdayPicker extends StatefulWidget {
  @override
  _BirthdayPickerState createState() => _BirthdayPickerState();
}

class _BirthdayPickerState extends State<BirthdayPicker> {
  DateTime? selectedDate;
  final dio = Dio();

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now().subtract(Duration(days: 365 * 25)),
      firstDate: DateTime(1900),
      lastDate: DateTime.now(),
    );

    if (picked != null && picked != selectedDate) {
      setState(() {
        selectedDate = picked;
      });
      
      // Save to backend
      await _updateBirthday(picked);
    }
  }

  Future<void> _updateBirthday(DateTime date) async {
    try {
      // ✅ IMPORTANT: Set to noon UTC to avoid timezone issues
      final dateOfBirth = DateTime.utc(
        date.year,
        date.month,
        date.day,
        12, // Noon
        0,
        0,
      );

      final response = await dio.put(
        'http://localhost:8080/v1/profile/me',
        data: {
          'dateOfBirth': dateOfBirth.toIso8601String(),
        },
        options: Options(
          headers: {
            'Authorization': 'Bearer YOUR_TOKEN',
            'Content-Type': 'application/json',
          },
        ),
      );

      print('✅ Birthday updated: ${response.data['data']['dateOfBirth']}');
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Cập nhật ngày sinh thành công!')),
      );
    } catch (e) {
      print('❌ Error: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Lỗi: Không thể cập nhật ngày sinh')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          selectedDate == null
              ? 'Chưa chọn ngày sinh'
              : 'Ngày sinh: ${selectedDate!.day}/${selectedDate!.month}/${selectedDate!.year}',
        ),
        ElevatedButton(
          onPressed: () => _selectDate(context),
          child: Text('Chọn ngày sinh'),
        ),
      ],
    );
  }
}
```

## 🧪 Testing

### Test Script (Bash)

```bash
# Get JWT token first
TOKEN=$(curl -s -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"your_username","password":"your_password"}' \
  | jq -r '.data.accessToken')

# Test with 00:00 UTC (BAD)
curl -X PUT http://localhost:8080/v1/profile/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"dateOfBirth":"2024-12-21T00:00:00Z"}'

# Check result - will show 2024-12-20 (WRONG!)
curl -X GET http://localhost:8080/v1/birthdays/debug/1 \
  -H "Authorization: Bearer $TOKEN" | jq '.'

# Test with 12:00 UTC (GOOD)
curl -X PUT http://localhost:8080/v1/profile/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"dateOfBirth":"2024-12-21T12:00:00Z"}'

# Check result - will show 2024-12-21 (CORRECT!)
curl -X GET http://localhost:8080/v1/birthdays/debug/1 \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Test in Flutter

```dart
void testBirthdayUpdate() async {
  final dio = Dio();
  
  // Test 1: 00:00 UTC (BAD)
  print('Test 1: 00:00 UTC');
  await dio.put(
    'http://localhost:8080/v1/profile/me',
    data: {'dateOfBirth': '2024-12-21T00:00:00Z'},
    options: Options(headers: {'Authorization': 'Bearer $token'}),
  );
  
  var debug = await dio.get(
    'http://localhost:8080/v1/birthdays/debug/1',
    options: Options(headers: {'Authorization': 'Bearer $token'}),
  );
  print('Result: ${debug.data['dateOfBirth_localDate_systemTZ']}'); // Will be 2024-12-20
  
  // Test 2: 12:00 UTC (GOOD)
  print('Test 2: 12:00 UTC');
  await dio.put(
    'http://localhost:8080/v1/profile/me',
    data: {'dateOfBirth': '2024-12-21T12:00:00Z'},
    options: Options(headers: {'Authorization': 'Bearer $token'}),
  );
  
  debug = await dio.get(
    'http://localhost:8080/v1/birthdays/debug/1',
    options: Options(headers: {'Authorization': 'Bearer $token'}),
  );
  print('Result: ${debug.data['dateOfBirth_localDate_systemTZ']}'); // Will be 2024-12-21
}
```

## 📊 Debug Endpoint

Backend có endpoint debug để kiểm tra timezone:

```bash
GET /v1/birthdays/debug/{userId}
```

**Response:**
```json
{
  "userId": 1,
  "username": "user1",
  "dateOfBirth_instant": "2024-12-21T12:00:00Z",
  "dateOfBirth_localDate_systemTZ": "2024-12-21",
  "dateOfBirth_localDate_UTC": "2024-12-21",
  "today_systemTZ": "2024-12-21",
  "today_UTC": "2024-12-21",
  "systemTimezone": "UTC",
  "month_match": true,
  "day_match": true,
  "is_birthday_today": true
}
```

## 🎯 Summary

**Problem:** Frontend gửi `00:00 UTC` → Database lưu ngày trước 1 ngày

**Solution:** Frontend gửi `12:00 UTC` → Database lưu đúng ngày

**Code Change:**
```dart
// Before
final dateOfBirth = selectedDate.toUtc();

// After
final dateOfBirth = DateTime.utc(
  selectedDate.year,
  selectedDate.month,
  selectedDate.day,
  12, 0, 0, // Noon
);
```

## 📝 Notes

1. **Why 12:00?** Giữa ngày (noon) đảm bảo không bị lệch timezone ở bất kỳ múi giờ nào (-12 đến +14)
2. **Backend already handles it:** Birthday check chỉ so sánh month/day, không quan tâm time
3. **No backend change needed:** Chỉ cần fix frontend
4. **Works for all timezones:** UTC+14 (Kiribati) đến UTC-12 (Baker Island)

---

**Last updated:** 2024-12-21
