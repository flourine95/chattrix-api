# 🚀 Hướng Dẫn Refactor Nhanh

## Tóm Tắt
Refactor giảm từ **~25 bảng xuống 7 bảng chính** + cleanup **21 entity files**.

## Các Bước Thực Hiện

### 1️⃣ Backup Database
```bash
docker compose exec postgres pg_dump -U postgres chattrix > backup_$(date +%Y%m%d_%H%M%S).sql
```

### 2️⃣ Fix Enum Imports (Tự động)
```powershell
.\fix-enum-imports.ps1
```
Script này sẽ:
- Di chuyển enums vào inner classes
- Cập nhật tất cả imports
- Fix enum references trong code

### 3️⃣ Chạy Migration
```bash
docker compose exec postgres psql -U postgres -d chattrix -f migration-refactor.sql
```

### 4️⃣ Compile & Build
```bash
mvn clean compile
docker compose up -d --build
```

### 5️⃣ Kiểm Tra Logs
```bash
docker compose logs -f api
```

### 6️⃣ Cập Nhật Services
Xem chi tiết trong `CODE-CHANGES-CHECKLIST.md`

---

## 📚 Tài Liệu Chi Tiết

| File | Mô Tả |
|------|-------|
| `REFACTOR-SUMMARY.md` | Tóm tắt tổng quan |
| `REFACTOR-GUIDE.md` | Hướng dẫn chi tiết từng thay đổi |
| `CODE-CHANGES-CHECKLIST.md` | Checklist cập nhật code |
| `ENTITIES-CLEANUP-SUMMARY.md` | Tóm tắt cleanup entities |
| `migration-refactor.sql` | SQL migration script |

---

## ✅ Thay Đổi Chính

### Entities
- ❌ Xóa 12 entities (Poll, Event, ConversationSettings, etc.)
- ❌ Xóa 9 enum files độc lập
- ✅ Giữ 13 entities (7 core + 6 supporting)
- ✅ Tất cả enums là inner classes

### Database
- ✅ User: Xóa `online`, giữ `lastSeen`
- ✅ Conversation: Thêm `metadata` JSONB
- ✅ ConversationParticipant: Gộp settings, thêm `unreadMarkerId`
- ✅ Message: Thêm `metadata` JSONB
- ✅ UserToken: Gộp VerificationToken + PasswordResetToken

### Cache
- ✅ OnlineStatusCache: Caffeine cache cho online status

---

## 🔄 Rollback

Nếu có vấn đề:
```bash
docker compose exec postgres psql -U postgres -d chattrix < backup_YYYYMMDD_HHMMSS.sql
git checkout <commit-before-refactor>
docker compose up -d --build
```

---

## 📊 Kết Quả

| Metric | Trước | Sau | Giảm |
|--------|-------|-----|------|
| Bảng database | ~25 | 7 | 72% |
| Entity files | 25 | 13 | 48% |
| Enum files | 9 | 0 | 100% |
| Tổng files | 34 | 13 | 62% |

---

## ⚠️ Lưu Ý

1. **Backup trước khi migration**
2. **Chạy fix-enum-imports.ps1 trước khi compile**
3. **Test kỹ trên staging trước production**
4. **Monitor Caffeine Cache memory usage**
5. **Kiểm tra JSONB query performance**

---

## 🆘 Hỗ Trợ

Nếu gặp lỗi:
1. Kiểm tra logs: `docker compose logs -f api`
2. Kiểm tra DB: `docker compose exec postgres psql -U postgres -d chattrix`
3. Xem chi tiết trong `REFACTOR-GUIDE.md`

---

**Good luck! 🎉**
