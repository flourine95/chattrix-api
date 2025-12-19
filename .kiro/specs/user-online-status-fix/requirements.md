# Requirements Document

## Introduction

This feature addresses critical issues with user online status functionality in the Chattrix API. Currently, users appear as offline even when actively chatting because the API does not properly expose online status information and does not broadcast status change events via WebSocket. This feature will ensure that online status is correctly included in API responses and that real-time status updates are broadcast to relevant users.

## Glossary

- **User**: A registered account in the Chattrix system
- **Online Status**: A boolean flag indicating whether a user is currently connected to the system
- **Last Seen**: A timestamp indicating when a user was last active
- **Participant**: A user who is a member of a conversation
- **WebSocket Event**: A real-time message sent from server to connected clients
- **Contact**: A user who has an accepted friend relationship with another user
- **Conversation API**: The REST endpoint that returns conversation details including participants
- **User Status Event**: A WebSocket message of type "user.status" that broadcasts online status changes

## Requirements

### Requirement 1

**User Story:** As a user viewing my conversations, I want to see which participants are currently online, so that I know who is available to chat with immediately.

#### Acceptance Criteria

1. WHEN the system returns conversation details, THEN the system SHALL include the online status field for each participant
2. WHEN the system returns conversation details, THEN the system SHALL include the lastSeen timestamp for each participant who is offline
3. WHEN a participant is currently online, THEN the system SHALL set the online field to true
4. WHEN a participant is currently offline, THEN the system SHALL set the online field to false
5. THE system SHALL populate online status from the User entity's online field

### Requirement 2

**User Story:** As a user, I want to receive real-time notifications when my contacts come online or go offline, so that I can see status changes without refreshing.

#### Acceptance Criteria

1. WHEN a user establishes a WebSocket connection, THEN the system SHALL mark that user as online
2. WHEN a user closes their WebSocket connection, THEN the system SHALL mark that user as offline
3. WHEN a user's online status changes to online, THEN the system SHALL broadcast a user.status event to all relevant users
4. WHEN a user's online status changes to offline, THEN the system SHALL broadcast a user.status event to all relevant users
5. WHEN broadcasting a user.status event, THEN the system SHALL include userId, username, fullName, online, and lastSeen fields

### Requirement 3

**User Story:** As a user, I want status change events to be sent only to people who should see my status, so that my privacy is maintained.

#### Acceptance Criteria

1. WHEN a user's status changes, THEN the system SHALL send user.status events to all users who have that user in their contacts
2. WHEN a user's status changes, THEN the system SHALL send user.status events to all users in conversations with that user
3. THE system SHALL NOT send user.status events to users who have no relationship with the status-changing user

### Requirement 4

**User Story:** As a developer, I want the user.status WebSocket event to match the API specification, so that the client can parse it correctly.

#### Acceptance Criteria

1. THE user.status event SHALL have type field set to "user.status"
2. THE user.status event payload SHALL contain a userId field as a string
3. THE user.status event payload SHALL contain a username field as a string
4. THE user.status event payload SHALL contain a fullName field as a string
5. THE user.status event payload SHALL contain an online field as a boolean
6. THE user.status event payload SHALL contain a lastSeen field as an ISO 8601 timestamp or null when user is online

### Requirement 5

**User Story:** As a system administrator, I want the online status to be accurately tracked across multiple sessions, so that users with multiple devices are correctly shown as online.

#### Acceptance Criteria

1. WHEN a user connects from multiple devices, THEN the system SHALL maintain a count of active sessions
2. WHEN a user has at least one active session, THEN the system SHALL mark that user as online
3. WHEN a user's last session disconnects, THEN the system SHALL mark that user as offline
4. THE system SHALL update the lastSeen timestamp whenever a user goes offline
