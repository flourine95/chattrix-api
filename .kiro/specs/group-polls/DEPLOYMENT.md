# Poll Feature - Deployment Guide

## 1. Database Migration

Chạy SQL script để tạo tables:

```bash
# Vào container postgres
docker compose exec postgres psql -U postgres -d chattrix

# Hoặc copy file vào container và chạy
docker compose cp create-polls-tables.sql postgres:/tmp/
docker compose exec postgres psql -U postgres -d chattrix -f /tmp/create-polls-tables.sql
```

Hoặc chạy trực tiếp từ host:

```bash
psql -h localhost -p 5432 -U postgres -d chattrix -f create-polls-tables.sql
```

## 2. Rebuild & Deploy Application

```bash
# Build lại application
mvn clean package -DskipTests

# Rebuild và restart Docker containers
docker compose up -d --build

# Xem logs để check deployment
docker compose logs -f api
```

## 3. Verify Deployment

### Check Tables Created
```sql
-- Vào postgres
docker compose exec postgres psql -U postgres -d chattrix

-- Check tables
\dt polls*

-- Check structure
\d polls
\d poll_options
\d poll_votes
```

### Test API Endpoints

```bash
# 1. Login để lấy token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'

# Lưu token vào biến
TOKEN="your_access_token_here"

# 2. Tạo poll
curl -X POST http://localhost:8080/api/conversations/1/polls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test poll?",
    "options": ["Option 1", "Option 2", "Option 3"],
    "allowMultipleVotes": false,
    "expiresAt": null
  }'

# 3. Vote
curl -X POST http://localhost:8080/api/polls/1/vote \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [1]}'

# 4. Get poll
curl -X GET http://localhost:8080/api/polls/1 \
  -H "Authorization: Bearer $TOKEN"

# 5. List polls
curl -X GET "http://localhost:8080/api/conversations/1/polls?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

## 4. Rollback (nếu cần)

```sql
-- Drop tables nếu cần rollback
DROP TABLE IF EXISTS poll_votes CASCADE;
DROP TABLE IF EXISTS poll_options CASCADE;
DROP TABLE IF EXISTS polls CASCADE;
```

## 5. Monitoring

### Check Application Logs
```bash
docker compose logs -f api
```

### Check Database
```sql
-- Count polls
SELECT COUNT(*) FROM polls;

-- Count votes
SELECT COUNT(*) FROM poll_votes;

-- Recent polls
SELECT id, question, created_at FROM polls ORDER BY created_at DESC LIMIT 10;
```

## 6. Common Issues

### Issue: Tables already exist
**Solution:** Drop existing tables first hoặc sử dụng `CREATE TABLE IF NOT EXISTS`

### Issue: Foreign key constraint fails
**Solution:** Đảm bảo conversations và users tables tồn tại trước

### Issue: WebSocket notifications không hoạt động
**Solution:** 
- Check WebSocket connection đang active
- Check ChatSessionService đang hoạt động
- Xem logs để debug

### Issue: Build fails
**Solution:**
```bash
# Clean và rebuild
mvn clean compile
mvn clean package -DskipTests
```

## 7. Performance Tuning

### Add more indexes nếu cần
```sql
-- Index cho filtering polls by status
CREATE INDEX idx_polls_active ON polls(conversation_id, is_closed, expires_at) 
WHERE is_closed = false;

-- Index cho vote counting
CREATE INDEX idx_poll_votes_count ON poll_votes(poll_option_id, user_id);
```

### Monitor query performance
```sql
-- Enable query logging
ALTER DATABASE chattrix SET log_statement = 'all';
ALTER DATABASE chattrix SET log_duration = on;

-- Check slow queries
SELECT * FROM pg_stat_statements 
WHERE query LIKE '%poll%' 
ORDER BY total_exec_time DESC 
LIMIT 10;
```

## 8. Security Checklist

✅ Authentication required cho tất cả endpoints  
✅ Authorization check: chỉ participants mới access polls  
✅ Creator-only actions: close/delete polls  
✅ Input validation: question length, option count  
✅ SQL injection prevention: sử dụng parameterized queries  
✅ Rate limiting: áp dụng cho create/vote endpoints  

## 9. Feature Flags (Optional)

Nếu muốn enable/disable feature:

```java
// Add to application.properties
feature.polls.enabled=true

// Check trong PollResource
@Inject
@ConfigProperty(name = "feature.polls.enabled", defaultValue = "true")
private boolean pollsEnabled;

// Validate
if (!pollsEnabled) {
    throw BusinessException.badRequest("Polls feature is disabled", "FEATURE_DISABLED");
}
```

## 10. Next Steps

- [ ] Add analytics tracking cho poll usage
- [ ] Add notification preferences
- [ ] Add poll templates
- [ ] Add poll export functionality
- [ ] Add admin dashboard cho poll statistics
