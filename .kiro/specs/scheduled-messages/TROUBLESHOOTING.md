# Scheduled Messages - Troubleshooting Guide

## Issue: Status SENT Returns Empty Results

### Problem Description
When querying scheduled messages with `status=SENT`, the API returns an empty list even though the database contains messages with `scheduled_status='SENT'`.

**Example:**
```bash
curl --location 'http://localhost:8080/api/v1/messages/scheduled?status=SENT' \
  --header 'Authorization: Bearer YOUR_TOKEN'

# Returns: {"data": [], "total": 0}
```

### Root Cause
Earlier version of the backend code incorrectly set `scheduled=false` when messages were sent. The current code is correct and keeps `scheduled=true`, but existing data in the database is inconsistent.

**How the API works:**
- All scheduled message queries filter by `m.scheduled = true`
- If a message has `scheduled_status='SENT'` but `scheduled=false`, it won't be returned

**Database evidence:**
```sql
-- Messages with inconsistent data
SELECT id, scheduled, scheduled_status 
FROM messages 
WHERE scheduled_status IN ('SENT', 'FAILED', 'CANCELLED') 
  AND scheduled IS NOT true;

-- Example result:
-- id=11, scheduled=false, scheduled_status=SENT  ❌ Won't be returned by API
-- id=6,  scheduled=false, scheduled_status=SENT  ❌ Won't be returned by API
```

### Solution

**Step 1: Run the data fix script**

Connect to your database and run:
```bash
# Using docker compose
docker compose exec postgres psql -U postgres -d chattrix
```

Then execute:
```sql
-- Fix inconsistent scheduled message data
UPDATE messages 
SET scheduled = true 
WHERE scheduled_status IN ('SENT', 'FAILED', 'CANCELLED') 
  AND scheduled IS NOT true;

-- Check how many rows were updated
-- Should show: UPDATE X (where X is the number of fixed messages)
```

**Step 2: Verify the fix**

Check the data:
```sql
-- Verify all scheduled messages now have scheduled=true
SELECT 
    COUNT(*) as total_scheduled_messages,
    SUM(CASE WHEN scheduled_status = 'PENDING' THEN 1 ELSE 0 END) as pending,
    SUM(CASE WHEN scheduled_status = 'SENT' THEN 1 ELSE 0 END) as sent,
    SUM(CASE WHEN scheduled_status = 'FAILED' THEN 1 ELSE 0 END) as failed,
    SUM(CASE WHEN scheduled_status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled
FROM messages 
WHERE scheduled = true;
```

**Step 3: Test the API**

```bash
# Should now return SENT messages
curl --location 'http://localhost:8080/api/v1/messages/scheduled?status=SENT' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```

### Prevention

The current backend code (as of the latest version) is correct and will NOT create this issue for new messages. The `processScheduledMessages()` method in `ScheduledMessageService.java` explicitly keeps `scheduled=true`:

```java
// Line 267 in ScheduledMessageService.java
scheduledMsg.setScheduledStatus(Message.ScheduledStatus.SENT);
scheduledMsg.setSentAt(Instant.now());
// Keep scheduled=true to maintain history that this was a scheduled message
messageRepository.save(scheduledMsg);
```

This fix only needs to be run once to correct historical data.

---

## Issue: Scheduled Messages Not Appearing in Conversation

### Problem Description
Scheduled messages (especially PENDING ones) don't appear in the correct chronological order when fetching conversation messages.

### Root Cause
The conversation messages query was ordering by `m.sentAt`, but PENDING scheduled messages have `sentAt=NULL`.

### Solution
This has been fixed in the latest code. The query now uses:
```sql
ORDER BY COALESCE(m.sentAt, m.scheduledTime, m.createdAt)
```

This ensures:
- Regular messages: ordered by `sentAt`
- Pending scheduled messages: ordered by `scheduledTime`
- Fallback: ordered by `createdAt`

**Affected methods in MessageRepository.java:**
- `findByConversationIdWithSort()` - line ~140
- `searchMessages()` - line ~180
- `findMediaByConversationId()` - line ~200

No action needed - this is already fixed in the current code.

---

## Issue: Missing Fields in Response

### Problem Description
API response doesn't include scheduled-related fields: `scheduled`, `scheduledTime`, `scheduledStatus`, `failedReason`.

### Solution
This has been fixed. `MessageResponse.java` now includes all 4 fields:

```java
private Boolean scheduled;
private Instant scheduledTime;
private String scheduledStatus;  // Mapped from enum to String
private String failedReason;
```

The `MessageMapper.java` correctly maps the `scheduledStatus` enum to String.

No action needed - this is already fixed in the current code.

---

## Verification Checklist

After running the data fix, verify everything works:

### 1. List all scheduled messages
```bash
curl --location 'http://localhost:8080/api/v1/messages/scheduled' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```
**Expected:** Returns all scheduled messages with `scheduled=true`

### 2. Filter by status PENDING
```bash
curl --location 'http://localhost:8080/api/v1/messages/scheduled?status=PENDING' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```
**Expected:** Returns only PENDING messages

### 3. Filter by status SENT
```bash
curl --location 'http://localhost:8080/api/v1/messages/scheduled?status=SENT' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```
**Expected:** Returns SENT messages (should not be empty if you have sent scheduled messages)

### 4. Check response includes all fields
```json
{
  "id": 11,
  "content": "Test message",
  "scheduled": true,              // ✓ Present
  "scheduledTime": "2025-12-23T10:00:00Z",  // ✓ Present
  "scheduledStatus": "SENT",      // ✓ Present
  "failedReason": null,           // ✓ Present
  "sentAt": "2025-12-22T05:39:00.004268Z"
}
```

### 5. Verify conversation message ordering
```bash
curl --location 'http://localhost:8080/api/v1/conversations/1/messages' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```
**Expected:** Messages appear in chronological order, including pending scheduled messages

---

## Database Queries for Debugging

### Check scheduled message data consistency
```sql
-- Find messages with inconsistent data
SELECT id, content, scheduled, scheduled_status, scheduled_time, sent_at
FROM messages
WHERE scheduled_status IS NOT NULL
ORDER BY id;
```

### Count scheduled messages by status
```sql
SELECT 
    scheduled_status,
    COUNT(*) as count,
    SUM(CASE WHEN scheduled = true THEN 1 ELSE 0 END) as with_scheduled_true,
    SUM(CASE WHEN scheduled IS NOT true THEN 1 ELSE 0 END) as with_scheduled_false
FROM messages
WHERE scheduled_status IS NOT NULL
GROUP BY scheduled_status;
```

### Find messages that won't be returned by API
```sql
-- These messages exist but won't show up in API results
SELECT id, content, scheduled, scheduled_status
FROM messages
WHERE scheduled_status IN ('SENT', 'FAILED', 'CANCELLED', 'PENDING')
  AND scheduled IS NOT true;
```

---

## Summary

**The backend code is correct.** The issue was with historical data created by an earlier version. Run the fix script once to correct the data:

```sql
UPDATE messages 
SET scheduled = true 
WHERE scheduled_status IN ('SENT', 'FAILED', 'CANCELLED') 
  AND scheduled IS NOT true;
```

After this fix, all scheduled message features will work correctly.
