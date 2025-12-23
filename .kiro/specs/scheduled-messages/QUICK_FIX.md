# Quick Fix for Status SENT Issue

## Problem
API returns empty list when querying `status=SENT` even though database has SENT messages.

## Cause
Earlier code version set `scheduled=false` when sending. Current code is correct but old data is inconsistent.

## Fix (Run Once)

### Step 1: Connect to database
```bash
docker compose exec postgres psql -U postgres -d chattrix
```

### Step 2: Run fix
```sql
UPDATE messages 
SET scheduled = true 
WHERE scheduled_status IN ('SENT', 'FAILED', 'CANCELLED') 
  AND scheduled IS NOT true;
```

### Step 3: Verify
```sql
SELECT COUNT(*) FROM messages WHERE scheduled = true AND scheduled_status = 'SENT';
```

### Step 4: Test API
```bash
curl --location 'http://localhost:8080/api/v1/messages/scheduled?status=SENT' \
  --header 'Authorization: Bearer YOUR_TOKEN'
```

## Done!
The backend code is already correct. This fix only corrects historical data.
