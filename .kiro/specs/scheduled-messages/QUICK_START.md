# Scheduled Messages - Quick Start Guide

## 1. Database Setup

Run the migration script to create the `scheduled_messages` table:

```bash
# Using Docker
docker compose exec postgres psql -U postgres -d chattrix -f /app/scheduled-messages-migration.sql

# Or copy the file into container first
docker cp scheduled-messages-migration.sql chattrix-postgres:/tmp/
docker compose exec postgres psql -U postgres -d chattrix -f /tmp/scheduled-messages-migration.sql
```

## 2. Build and Deploy

Rebuild the application to include the new code:

```bash
docker compose up -d --build
```

Check the logs to verify the scheduler is running:

```bash
docker compose logs -f api
```

You should see: `Processing scheduled messages...` every 30 seconds.

## 3. Test the API

### Get Access Token

First, login to get an access token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'
```

Save the `accessToken` from the response.

### Schedule a Message

Schedule a message to be sent 2 minutes from now:

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "This is a scheduled message!",
    "type": "TEXT",
    "scheduledTime": "2025-12-22T15:30:00Z"
  }'
```

**Note**: Replace the `scheduledTime` with a timestamp 1-2 minutes in the future. Use UTC format.

### List Scheduled Messages

```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=SCHEDULED&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Get Single Scheduled Message

```bash
curl -X GET http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Update Scheduled Message

```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Updated scheduled message content",
    "scheduledTime": "2025-12-22T15:35:00Z"
  }'
```

### Cancel Scheduled Message

```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Bulk Cancel Scheduled Messages

```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "scheduledMessageIds": [1, 2, 3]
  }'
```

## 4. Test WebSocket Events

Create an HTML file to test WebSocket events:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Scheduled Messages WebSocket Test</title>
</head>
<body>
    <h1>Scheduled Messages WebSocket Test</h1>
    <div id="messages"></div>

    <script>
        const token = 'YOUR_ACCESS_TOKEN'; // Replace with your token
        const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

        ws.onopen = () => {
            console.log('WebSocket connected');
            document.getElementById('messages').innerHTML += '<p>✅ Connected</p>';
        };

        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Received:', data);

            if (data.type === 'scheduled.message.sent') {
                document.getElementById('messages').innerHTML += 
                    `<p>✅ Scheduled message sent: ${data.payload.message.content}</p>`;
            } else if (data.type === 'scheduled.message.failed') {
                document.getElementById('messages').innerHTML += 
                    `<p>❌ Scheduled message failed: ${data.payload.failedReason}</p>`;
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            document.getElementById('messages').innerHTML += '<p>❌ Error</p>';
        };

        ws.onclose = () => {
            console.log('WebSocket disconnected');
            document.getElementById('messages').innerHTML += '<p>🔌 Disconnected</p>';
        };
    </script>
</body>
</html>
```

## 5. Verify Scheduler Behavior

### Test Automatic Sending

1. Schedule a message 1 minute in the future
2. Wait for the scheduler to process it (runs every 30 seconds)
3. Check the conversation - the message should appear
4. Check WebSocket events - you should receive `scheduled.message.sent`

### Test Failure Scenario

1. Schedule a message in a conversation
2. Leave the conversation (or have another user leave)
3. Wait for the scheduled time
4. The message should fail with reason "User has left the conversation"
5. Check WebSocket events - you should receive `scheduled.message.failed`

## 6. Database Verification

Check the scheduled messages in the database:

```bash
docker compose exec postgres psql -U postgres -d chattrix
```

```sql
-- View all scheduled messages
SELECT id, sender_id, conversation_id, content, scheduled_time, status, created_at 
FROM scheduled_messages 
ORDER BY created_at DESC;

-- View scheduled messages by status
SELECT status, COUNT(*) 
FROM scheduled_messages 
GROUP BY status;

-- View upcoming scheduled messages
SELECT id, content, scheduled_time, status 
FROM scheduled_messages 
WHERE status = 'SCHEDULED' AND scheduled_time > NOW() 
ORDER BY scheduled_time ASC;
```

## 7. Common Issues

### Scheduler Not Running

**Symptom**: Messages not being sent automatically

**Solution**: 
- Check logs: `docker compose logs -f api`
- Look for "Processing scheduled messages..." every 30 seconds
- If not present, restart the container: `docker compose restart api`

### Messages Not Sending

**Symptom**: Messages stay in SCHEDULED status past their scheduled time

**Check**:
1. Verify scheduled_time is in UTC
2. Check if user is still a participant in the conversation
3. Look for errors in the logs
4. Verify the scheduler is running

### WebSocket Events Not Received

**Symptom**: No WebSocket notifications

**Check**:
1. Verify WebSocket connection is established
2. Check if token is valid
3. Verify user is a participant in the conversation
4. Check browser console for errors

### Permission Errors

**Symptom**: 401 Unauthorized or 404 Not Found

**Check**:
1. Verify user is a participant in the conversation
2. Verify user is the sender of the scheduled message
3. Check if token is valid and not expired

## 8. Monitoring

### Check Scheduler Health

```bash
# View scheduler logs
docker compose logs -f api | grep "Processing scheduled messages"

# Count scheduled messages by status
docker compose exec postgres psql -U postgres -d chattrix -c \
  "SELECT status, COUNT(*) FROM scheduled_messages GROUP BY status;"
```

### Performance Monitoring

```bash
# Check query performance
docker compose exec postgres psql -U postgres -d chattrix -c \
  "EXPLAIN ANALYZE SELECT * FROM scheduled_messages 
   WHERE status = 'SCHEDULED' AND scheduled_time <= NOW();"
```

## 9. Cleanup

To remove test data:

```sql
-- Delete all scheduled messages
DELETE FROM scheduled_messages;

-- Or delete only completed messages
DELETE FROM scheduled_messages WHERE status IN ('SENT', 'CANCELLED');
```

## 10. Production Considerations

Before deploying to production:

1. **Backup database** before running migration
2. **Test scheduler** with various scenarios
3. **Monitor performance** under load
4. **Set up alerts** for failed messages
5. **Configure logging** for troubleshooting
6. **Review security** settings
7. **Test WebSocket** event delivery at scale

## Support

For issues or questions:
1. Check the logs: `docker compose logs -f api`
2. Review the IMPLEMENTATION_SUMMARY.md
3. Check the requirements.md for expected behavior
4. Verify database schema matches migration script
