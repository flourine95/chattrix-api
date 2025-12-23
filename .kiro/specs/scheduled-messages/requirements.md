# Requirements Document: Scheduled Messages

## Introduction

The Scheduled Messages feature allows users to compose messages and schedule them to be sent automatically at a specified future time. This enables users to plan communications in advance, send messages at optimal times across different timezones, and automate recurring communications. The system will handle message delivery, failure scenarios, and provide real-time updates through WebSocket events.

## Glossary

- **Scheduled_Message**: A message that has been created but is set to be sent automatically at a future timestamp
- **Scheduler_Service**: The background service that monitors scheduled messages and sends them at the appropriate time
- **Message_Processor**: The component responsible for converting scheduled messages into actual messages
- **Sender**: The user who creates and schedules a message
- **Conversation**: A chat context (direct or group) where the scheduled message will be sent
- **Scheduled_Time**: The UTC timestamp when the message should be automatically sent
- **Status**: The current state of a scheduled message (SCHEDULED, SENT, FAILED, CANCELLED)

## Requirements

### Requirement 1: Schedule Message Creation

**User Story:** As a user, I want to schedule a message to be sent at a future time, so that I can plan my communications in advance.

#### Acceptance Criteria

1. WHEN a user submits a valid message with a future scheduled time, THE System SHALL create a scheduled message record with status SCHEDULED
2. WHEN a user provides a scheduled time in the past, THE System SHALL reject the request and return a validation error
3. WHEN a user provides a scheduled time more than 1 year in the future, THE System SHALL reject the request and return a validation error
4. WHEN creating a scheduled message, THE System SHALL support all message types (TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT)
5. WHEN creating a scheduled message with media, THE System SHALL store the media URL, thumbnail URL, file name, file size, and duration
6. WHEN creating a scheduled message as a reply, THE System SHALL store the reply-to message ID
7. WHEN a scheduled message is created, THE System SHALL store it with the sender ID and conversation ID
8. WHEN a scheduled message is successfully created, THE System SHALL return a 201 Created response with the scheduled message details

### Requirement 2: Scheduled Message Retrieval

**User Story:** As a user, I want to view my scheduled messages, so that I can track what messages are pending delivery.

#### Acceptance Criteria

1. WHEN a user requests their scheduled messages, THE System SHALL return a paginated list of scheduled messages ordered by scheduled time
2. WHEN a user filters by conversation ID, THE System SHALL return only scheduled messages for that conversation
3. WHEN a user filters by status, THE System SHALL return only scheduled messages matching that status
4. WHEN retrieving scheduled messages, THE System SHALL include conversation name and type for context
5. WHEN a user requests a specific scheduled message by ID, THE System SHALL return the complete scheduled message details
6. WHEN a user requests a non-existent scheduled message, THE System SHALL return a 404 Not Found response
7. WHEN a user requests another user's scheduled message, THE System SHALL return a 404 Not Found response

### Requirement 3: Scheduled Message Modification

**User Story:** As a user, I want to edit my scheduled messages before they are sent, so that I can correct mistakes or update the content.

#### Acceptance Criteria

1. WHEN a user updates a scheduled message with status SCHEDULED, THE System SHALL apply the changes and update the timestamp
2. WHEN a user attempts to update a scheduled message with status SENT, THE System SHALL reject the request with a 400 Bad Request response
3. WHEN a user attempts to update a scheduled message with status FAILED, THE System SHALL reject the request with a 400 Bad Request response
4. WHEN a user attempts to update a scheduled message with status CANCELLED, THE System SHALL reject the request with a 400 Bad Request response
5. WHEN updating scheduled time, THE System SHALL validate that the new time is in the future
6. WHEN a user updates only specific fields, THE System SHALL preserve all other fields unchanged
7. WHEN a scheduled message is successfully updated, THE System SHALL return a 200 OK response with updated details

### Requirement 4: Scheduled Message Cancellation

**User Story:** As a user, I want to cancel scheduled messages, so that I can prevent messages from being sent if my plans change.

#### Acceptance Criteria

1. WHEN a user cancels a scheduled message with status SCHEDULED, THE System SHALL update the status to CANCELLED
2. WHEN a user attempts to cancel a scheduled message with status SENT, THE System SHALL reject the request with a 400 Bad Request response
3. WHEN a user attempts to cancel a scheduled message with status FAILED, THE System SHALL allow the cancellation
4. WHEN a user attempts to cancel a scheduled message with status CANCELLED, THE System SHALL return a 200 OK response
5. WHEN a user cancels multiple scheduled messages in bulk, THE System SHALL cancel all messages with status SCHEDULED and return the count of successfully cancelled messages
6. WHEN bulk cancelling includes non-existent or already-sent messages, THE System SHALL return the IDs of messages that could not be cancelled
7. WHEN a scheduled message is successfully cancelled, THE System SHALL return a 200 OK response

### Requirement 5: Automatic Message Delivery

**User Story:** As a system administrator, I want scheduled messages to be automatically sent at their scheduled time, so that users don't need to manually trigger delivery.

#### Acceptance Criteria

1. WHEN the Scheduler_Service runs, THE System SHALL query all scheduled messages with status SCHEDULED and scheduled time before current UTC time
2. WHEN a scheduled message is due for delivery, THE System SHALL create a real message with all content from the scheduled message
3. WHEN a message is successfully created from a scheduled message, THE System SHALL update the scheduled message status to SENT
4. WHEN a message is successfully created from a scheduled message, THE System SHALL store the sent message ID and sent timestamp
5. WHEN message creation fails, THE System SHALL update the scheduled message status to FAILED and store the failure reason
6. WHEN the Scheduler_Service processes messages, THE System SHALL run at intervals of 30 seconds or less
7. WHEN multiple scheduled messages are due simultaneously, THE System SHALL process them in order of scheduled time

### Requirement 6: Real-time Notifications

**User Story:** As a user, I want to receive real-time updates when my scheduled messages are sent or fail, so that I know the delivery status immediately.

#### Acceptance Criteria

1. WHEN a scheduled message is successfully sent, THE System SHALL send a WebSocket event "scheduled.message.sent" to the sender
2. WHEN a scheduled message is successfully sent, THE System SHALL send the complete message details to all conversation participants
3. WHEN a scheduled message fails to send, THE System SHALL send a WebSocket event "scheduled.message.failed" to the sender
4. WHEN sending a failure notification, THE System SHALL include the scheduled message ID, conversation ID, failure reason, and failure timestamp
5. WHEN a scheduled message is sent, THE System SHALL create a notification for the sender confirming delivery
6. WHEN a scheduled message fails, THE System SHALL create a notification for the sender with the failure reason

### Requirement 7: Conversation Context Management

**User Story:** As a system administrator, I want scheduled messages to be automatically cancelled when users leave conversations, so that messages are not sent to conversations where the sender is no longer a participant.

#### Acceptance Criteria

1. WHEN a user leaves a conversation, THE System SHALL cancel all scheduled messages from that user in that conversation
2. WHEN cancelling scheduled messages due to user departure, THE System SHALL only cancel messages with status SCHEDULED
3. WHEN a conversation is deleted, THE System SHALL cascade delete all associated scheduled messages
4. WHEN a user is removed from a conversation by an admin, THE System SHALL cancel all their scheduled messages in that conversation

### Requirement 8: Authorization and Security

**User Story:** As a system administrator, I want to ensure users can only manage their own scheduled messages, so that message scheduling is secure and private.

#### Acceptance Criteria

1. WHEN a user creates a scheduled message, THE System SHALL verify the user is a participant in the target conversation
2. WHEN a user attempts to schedule a message in a conversation they are not part of, THE System SHALL return a 401 Unauthorized response
3. WHEN a user retrieves scheduled messages, THE System SHALL only return messages where the user is the sender
4. WHEN a user updates a scheduled message, THE System SHALL verify the user is the original sender
5. WHEN a user cancels a scheduled message, THE System SHALL verify the user is the original sender
6. WHEN a user requests a scheduled message by ID, THE System SHALL verify the user is the original sender

### Requirement 9: Data Persistence and Recovery

**User Story:** As a system administrator, I want scheduled messages to persist across system restarts, so that scheduled deliveries are not lost during maintenance.

#### Acceptance Criteria

1. WHEN the Scheduler_Service starts, THE System SHALL load all pending scheduled messages from the database
2. WHEN the system restarts, THE System SHALL process any scheduled messages that became due during downtime
3. WHEN storing scheduled messages, THE System SHALL use UTC timestamps for all time-related fields
4. WHEN retrieving scheduled messages, THE System SHALL maintain timezone consistency using UTC
5. WHEN a scheduled message is created, THE System SHALL store created_at and updated_at timestamps
6. WHEN a scheduled message is modified, THE System SHALL update the updated_at timestamp

### Requirement 10: Error Handling and Validation

**User Story:** As a user, I want clear error messages when scheduling fails, so that I can understand and correct any issues.

#### Acceptance Criteria

1. WHEN validation fails, THE System SHALL return a 400 Bad Request response with specific field errors
2. WHEN a resource is not found, THE System SHALL return a 404 Not Found response with a descriptive message
3. WHEN authorization fails, THE System SHALL return a 401 Unauthorized response with a descriptive message
4. WHEN message content is empty for TEXT messages, THE System SHALL reject the request with a validation error
5. WHEN required media fields are missing for media messages, THE System SHALL reject the request with a validation error
6. WHEN the conversation does not exist, THE System SHALL return a 404 Not Found response
7. WHEN the reply-to message does not exist, THE System SHALL reject the request with a validation error
