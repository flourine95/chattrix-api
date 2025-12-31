# 🚀 Deployment Checklist - Refactor Complete

## Pre-Deployment Verification

### ✅ 1. Code Verification
- [x] All 3 mappers deleted (PollMapper, EventMapper, CallHistoryMapper)
- [x] No references to removed entities (MessageReadReceipt, ConversationSettings, etc.)
- [x] No references to removed repositories
- [x] All services use proper delegation pattern
- [x] Logging added to all refactored services (@Slf4j)

### ✅ 2. Compilation Check
```bash
mvn clean compile
```

**Expected:** No compilation errors

**If errors occur:**
- Check for missing imports
- Verify all enum references use inner classes (e.g., `Call.CallStatus`)
- Check MapStruct generated sources in `target/generated-sources/annotations/`

---

## Deployment Steps

### Step 1: Build Application
```powershell
# Windows
.\build-and-deploy.ps1

# Or manually
mvn clean package
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

### Step 2: Deploy to Docker
```bash
# Stop existing containers
docker compose down

# Rebuild and start
docker compose up -d --build
```

### Step 3: Monitor Startup
```bash
# Watch logs
docker compose logs -f api

# Wait for:
# "WildFly Full 38.0.0.Final (WildFly Core 26.0.0.Final) started"
# "Deployed ROOT.war"
```

**Startup time:** ~30-60 seconds

---

## Post-Deployment Testing

### ✅ 1. Health Check
```bash
# Check API is running
curl http://localhost:8080/

# Expected: 200 OK
```

### ✅ 2. Cache Stats
```bash
curl http://localhost:8080/v1/admin/cache-stats

# Expected: JSON with cache statistics
# {
#   "onlineStatusCache": {...},
#   "userProfileCache": {...},
#   "conversationCache": {...},
#   "messageCache": {...}
# }
```

### ✅ 3. Authentication
```bash
# Login
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Expected: 200 OK with access_token
```

### ✅ 4. Messages (JSONB Metadata)
```bash
# Get messages
curl http://localhost:8080/v1/conversations/{conversationId}/messages \
  -H "Authorization: Bearer {token}"

# Expected: Messages with poll/event data from JSONB
```

### ✅ 5. Polls (JSONB)
```bash
# Create poll
curl -X POST http://localhost:8080/v1/conversations/{conversationId}/polls \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test poll?",
    "options": ["Option 1", "Option 2"],
    "allowMultipleVotes": false
  }'

# Expected: 201 Created with poll data
```

### ✅ 6. Events (JSONB)
```bash
# Create event
curl -X POST http://localhost:8080/v1/conversations/{conversationId}/events \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Meeting",
    "startTime": "2025-01-15T10:00:00Z",
    "location": "Conference Room"
  }'

# Expected: 201 Created with event data
```

### ✅ 7. Conversation Settings (ConversationParticipant)
```bash
# Get settings
curl http://localhost:8080/v1/conversations/{conversationId}/settings \
  -H "Authorization: Bearer {token}"

# Expected: Settings from ConversationParticipant
# {
#   "conversationId": 1,
#   "muted": false,
#   "pinned": false,
#   "archived": false,
#   "notificationsEnabled": true
# }
```

### ✅ 8. Mute Conversation
```bash
# Mute
curl -X POST http://localhost:8080/v1/conversations/{conversationId}/mute \
  -H "Authorization: Bearer {token}"

# Expected: 200 OK with muted: true
```

### ✅ 9. Read Receipts (ConversationParticipant)
```bash
# Mark as read
curl -X POST http://localhost:8080/v1/conversations/{conversationId}/messages/{messageId}/read \
  -H "Authorization: Bearer {token}"

# Expected: 200 OK with updated lastReadMessageId
```

### ✅ 10. Invite Links (JSONB)
```bash
# Create invite link
curl -X POST http://localhost:8080/v1/conversations/{conversationId}/invite-links \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"maxUses": 100}'

# Expected: 201 Created with token in conversation.metadata
```

---

## Database Verification

### Connect to Database
```bash
docker compose exec postgres psql -U postgres -d chattrix
```

### Check Tables
```sql
-- Should have 7 main tables
\dt

-- Expected tables:
-- users
-- conversations
-- conversation_participants (with settings fields)
-- messages (with metadata JSONB)
-- calls
-- user_tokens
-- contacts
```

### Verify JSONB Data
```sql
-- Check poll in message metadata
SELECT id, type, metadata->'poll' as poll_data 
FROM messages 
WHERE type = 'POLL' 
LIMIT 1;

-- Check event in message metadata
SELECT id, type, metadata->'event' as event_data 
FROM messages 
WHERE type = 'EVENT' 
LIMIT 1;

-- Check invite link in conversation metadata
SELECT id, name, metadata->'inviteLink' as invite_link 
FROM conversations 
WHERE metadata->'inviteLink' IS NOT NULL 
LIMIT 1;

-- Check settings in conversation_participants
SELECT user_id, conversation_id, muted, pinned, archived, theme 
FROM conversation_participants 
LIMIT 5;
```

---

## Performance Testing

### ✅ 1. Cache Hit Rates
```bash
# Monitor cache stats
curl http://localhost:8080/v1/admin/cache-stats

# Expected hit rates after warm-up:
# - OnlineStatusCache: >90%
# - UserProfileCache: >85%
# - ConversationCache: >80%
# - MessageCache: >75%
```

### ✅ 2. Batch Processing
```bash
# Check batch stats
curl http://localhost:8080/v1/admin/batch-stats

# Expected:
# {
#   "bufferSize": <100,
#   "maxBufferSize": 10000,
#   "batchSize": 100,
#   "flushIntervalSeconds": 5
# }
```

### ✅ 3. Session Management
```bash
# Check session stats
curl http://localhost:8080/v1/admin/session-stats

# Expected:
# {
#   "activeUsers": X,
#   "totalSessions": Y,
#   "hitRate": >0.95
# }
```

### ✅ 4. Query Performance
```sql
-- Check query execution time
EXPLAIN ANALYZE 
SELECT m.* FROM messages m 
WHERE m.conversation_id = 1 
ORDER BY m.sent_at DESC 
LIMIT 50;

-- Expected: <10ms for 50 messages
```

---

## Rollback Plan

### If Issues Occur:

#### 1. Check Logs
```bash
docker compose logs -f api | grep ERROR
```

#### 2. Database Issues
```bash
# Restore from backup
docker compose exec postgres psql -U postgres -d chattrix < backup.sql
```

#### 3. Application Issues
```bash
# Rollback to previous version
git checkout <previous-commit>
mvn clean package
docker compose up -d --build
```

#### 4. Critical Errors
```bash
# Stop everything
docker compose down

# Reset database
docker compose down -v

# Restore from backup and restart
docker compose up -d
```

---

## Monitoring

### Key Metrics to Watch

#### 1. Application Logs
```bash
docker compose logs -f api | grep -E "(ERROR|WARN)"
```

**Watch for:**
- NullPointerException
- ClassNotFoundException
- SQL errors
- Cache errors

#### 2. Database Connections
```sql
SELECT count(*) FROM pg_stat_activity WHERE datname = 'chattrix';
```

**Expected:** <50 connections

#### 3. Memory Usage
```bash
docker stats api
```

**Expected:**
- Memory: <2GB
- CPU: <50% (idle)

#### 4. Response Times
```bash
# Test endpoint response time
time curl http://localhost:8080/v1/conversations
```

**Expected:** <500ms

---

## Success Criteria

### ✅ All Tests Pass
- [ ] Health check returns 200
- [ ] Authentication works
- [ ] Messages load correctly
- [ ] Polls work (JSONB)
- [ ] Events work (JSONB)
- [ ] Settings work (ConversationParticipant)
- [ ] Read receipts work
- [ ] Invite links work (JSONB)
- [ ] Cache stats available
- [ ] Batch processing works

### ✅ Performance Acceptable
- [ ] Cache hit rates >80%
- [ ] Response times <500ms
- [ ] No memory leaks
- [ ] No database connection issues

### ✅ No Errors
- [ ] No compilation errors
- [ ] No runtime exceptions
- [ ] No SQL errors
- [ ] No cache errors

---

## Post-Deployment Tasks

### ✅ 1. Update Documentation
- [ ] Update API documentation
- [ ] Update database schema docs
- [ ] Update deployment guide

### ✅ 2. Monitor for 24 Hours
- [ ] Check logs every 4 hours
- [ ] Monitor error rates
- [ ] Monitor performance metrics
- [ ] Check cache hit rates

### ✅ 3. User Acceptance Testing
- [ ] Test all major features
- [ ] Test edge cases
- [ ] Test error handling
- [ ] Test performance under load

### ✅ 4. Cleanup
- [ ] Remove old backup files
- [ ] Archive old logs
- [ ] Update version numbers
- [ ] Tag release in git

---

## Contact Information

### If Issues Occur:
1. Check logs first: `docker compose logs -f api`
2. Check database: `docker compose exec postgres psql -U postgres -d chattrix`
3. Review refactor docs: `REFACTOR-COMPLETE-SUMMARY.md`
4. Review phase docs: `PHASE1-*.md`, `PHASE2-*.md`, `PHASE3-*.md`

---

## 🎉 Deployment Complete!

Once all checks pass:
- ✅ Refactor is 100% complete
- ✅ All 3 phases deployed successfully
- ✅ System is running smoothly
- ✅ Ready for production use

**Congratulations!** 🚀
