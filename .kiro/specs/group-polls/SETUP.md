# Hướng dẫn Setup Polls Feature

## 1. Chạy Database Migration

Có 2 cách để chạy migration:

### Cách 1: Sử dụng Docker exec (Khuyến nghị)
```bash
# Migration 1: Tạo bảng polls
docker compose exec postgres psql -U postgres -d chattrix -c "
CREATE TABLE IF NOT EXISTS polls (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    conversation_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    allow_multiple_votes BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS poll_options (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    option_text VARCHAR(200) NOT NULL,
    option_order INTEGER NOT NULL,
    CONSTRAINT fk_poll_option_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS poll_votes (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    poll_option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_vote_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_vote_option FOREIGN KEY (poll_option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_poll_user_option UNIQUE (poll_id, user_id, poll_option_id)
);

CREATE INDEX IF NOT EXISTS idx_polls_conversation_id ON polls(conversation_id);
CREATE INDEX IF NOT EXISTS idx_polls_creator_id ON polls(creator_id);
CREATE INDEX IF NOT EXISTS idx_polls_created_at ON polls(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_poll_options_poll_id ON poll_options(poll_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_poll_id ON poll_votes(poll_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_user_id ON poll_votes(user_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_option_id ON poll_votes(poll_option_id);
"

# Migration 2: Thêm poll_id vào bảng messages
docker compose exec postgres psql -U postgres -d chattrix -c "
ALTER TABLE messages ADD COLUMN IF NOT EXISTS poll_id BIGINT;
ALTER TABLE messages ADD CONSTRAINT fk_message_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_messages_poll_id ON messages(poll_id);
"
```

### Cách 2: Vào container và chạy file SQL
```bash
# Bước 1: Vào container postgres
docker compose exec postgres bash

# Bước 2: Chạy psql
psql -U postgres -d chattrix

# Bước 3: Copy nội dung file create-polls-tables.sql và paste vào psql
# Hoặc nếu đã copy file vào container:
\i /path/to/create-polls-tables.sql

# Bước 4: Thoát
\q
exit
```

## 2. Kiểm tra Tables đã được tạo

```bash
docker compose exec postgres psql -U postgres -d chattrix -c "\dt polls*"
```

Kết quả mong đợi:
```
              List of relations
 Schema |    Name     | Type  |  Owner   
--------+-------------+-------+----------
 public | poll_options| table | postgres
 public | poll_votes  | table | postgres
 public | polls       | table | postgres
```

## 3. Test API Endpoints

Xem file `API_GUIDE.md` để biết chi tiết các endpoints và cách test.

## Troubleshooting

### Lỗi: relation "polls" already exists
- Không sao, tables đã tồn tại rồi. Bỏ qua lỗi này.

### Lỗi: foreign key constraint
- Đảm bảo tables `conversations` và `users` đã tồn tại trước khi chạy migration.

### Lỗi: LazyInitializationException
- Đã được fix trong code. Rebuild và redeploy application.
