# Requirements Document

## Introduction

This feature refactors the WebSocket message handling in ChatServerEndpoint to use a cleaner, more maintainable architecture. Currently, the endpoint uses a large switch statement in `onMessage()` to route messages, which makes the code difficult to maintain and extend. This refactor will implement a handler-based pattern where each message type has its own dedicated handler class, and message routing is handled through a registry pattern.

## Glossary

- **Message Handler**: A dedicated class responsible for processing a specific message type
- **Handler Registry**: A service that maps message types to their corresponding handlers
- **Message Decoder**: A Jakarta WebSocket decoder that deserializes JSON into typed message objects
- **ChatServerEndpoint**: The main WebSocket endpoint that delegates message processing to handlers
- **Message Type**: A string identifier that determines which handler processes the message
- **Handler Interface**: A common interface that all message handlers implement

## Requirements

### Requirement 1

**User Story:** As a developer, I want each message type to have its own handler class, so that the code is organized and easy to maintain.

#### Acceptance Criteria

1. WHEN implementing message handlers THEN each handler SHALL be a separate class
2. WHEN creating a handler THEN the handler SHALL implement a common MessageHandler interface
3. WHEN the interface is defined THEN it SHALL have a handle() method that accepts Session, userId, and payload
4. WHEN a handler is created THEN it SHALL be annotated with @ApplicationScoped for CDI
5. WHEN a handler processes a message THEN it SHALL handle its own exceptions and error responses

### Requirement 2

**User Story:** As a developer, I want a registry to map message types to handlers, so that adding new message types doesn't require modifying the main endpoint.

#### Acceptance Criteria

1. WHEN creating the registry THEN it SHALL be a CDI-managed service
2. WHEN the registry initializes THEN it SHALL discover all MessageHandler implementations
3. WHEN looking up a handler THEN the registry SHALL return the handler for the given message type
4. WHEN a message type has no handler THEN the registry SHALL return an empty Optional
5. WHEN handlers are registered THEN each handler SHALL declare which message type it handles

### Requirement 3

**User Story:** As a developer, I want the ChatServerEndpoint to delegate message processing to handlers, so that the endpoint remains clean and focused.

#### Acceptance Criteria

1. WHEN a message is received THEN the endpoint SHALL look up the appropriate handler from the registry
2. WHEN a handler is found THEN the endpoint SHALL delegate processing to that handler
3. WHEN no handler is found THEN the endpoint SHALL log a warning about unknown message type
4. WHEN delegating to a handler THEN the endpoint SHALL pass the Session, userId, and message payload
5. WHEN a handler throws an exception THEN the endpoint SHALL catch it and continue processing other messages

### Requirement 4

**User Story:** As a developer, I want to separate chat-related handlers from call-related handlers, so that concerns are properly separated.

#### Acceptance Criteria

1. WHEN organizing handlers THEN chat handlers SHALL be in a chat package
2. WHEN organizing handlers THEN call handlers SHALL be in a call package
3. WHEN organizing handlers THEN system handlers (heartbeat) SHALL be in a system package
4. WHEN organizing handlers THEN typing handlers SHALL be in a typing package
5. WHEN a new feature is added THEN its handlers SHALL be in a dedicated package

### Requirement 5

**User Story:** As a developer, I want handlers to have access to required services through dependency injection, so that they can perform their business logic.

#### Acceptance Criteria

1. WHEN a handler needs a service THEN it SHALL inject the service using @Inject
2. WHEN a handler needs CallService THEN it SHALL inject CallService
3. WHEN a handler needs ChatSessionService THEN it SHALL inject ChatSessionService
4. WHEN a handler needs repositories THEN it SHALL inject the required repositories
5. WHEN a handler needs mappers THEN it SHALL inject the required mappers

### Requirement 6

**User Story:** As a developer, I want handlers to be testable in isolation, so that I can write unit tests for each handler independently.

#### Acceptance Criteria

1. WHEN testing a handler THEN it SHALL be testable without starting the WebSocket endpoint
2. WHEN testing a handler THEN dependencies SHALL be mockable
3. WHEN testing a handler THEN the test SHALL verify the handler's behavior in isolation
4. WHEN testing error cases THEN the test SHALL verify proper error handling
5. WHEN testing a handler THEN the test SHALL not require a full application context

### Requirement 7

**User Story:** As a developer, I want clear separation between message parsing and message handling, so that each concern is handled independently.

#### Acceptance Criteria

1. WHEN a message arrives THEN the decoder SHALL parse it into a WebSocketMessage object
2. WHEN the message is parsed THEN the endpoint SHALL extract the type and payload
3. WHEN the payload is extracted THEN the handler SHALL convert it to the appropriate DTO
4. WHEN conversion fails THEN the handler SHALL handle the error gracefully
5. WHEN the DTO is created THEN the handler SHALL process the business logic

### Requirement 8

**User Story:** As a developer, I want to add new message types without modifying existing code, so that the system follows the Open/Closed Principle.

#### Acceptance Criteria

1. WHEN adding a new message type THEN I SHALL create a new handler class
2. WHEN the handler is created THEN it SHALL be automatically discovered by the registry
3. WHEN the handler is discovered THEN it SHALL be available for message routing
4. WHEN adding the handler THEN I SHALL NOT modify ChatServerEndpoint
5. WHEN adding the handler THEN I SHALL NOT modify the HandlerRegistry

### Requirement 9

**User Story:** As a developer, I want consistent error handling across all handlers, so that errors are handled uniformly.

#### Acceptance Criteria

1. WHEN a handler encounters an error THEN it SHALL use a common error handling pattern
2. WHEN sending error responses THEN handlers SHALL use a shared error response builder
3. WHEN logging errors THEN handlers SHALL use consistent log levels and formats
4. WHEN an error occurs THEN the WebSocket connection SHALL remain open
5. WHEN an error is sent THEN it SHALL include the message type that caused the error

### Requirement 10

**User Story:** As a system administrator, I want proper logging of message routing, so that I can debug issues and monitor system behavior.

#### Acceptance Criteria

1. WHEN a message is received THEN the endpoint SHALL log the message type
2. WHEN a handler is found THEN the endpoint SHALL log which handler is processing the message
3. WHEN no handler is found THEN the endpoint SHALL log a warning with the unknown message type
4. WHEN a handler completes THEN the endpoint SHALL log successful processing
5. WHEN an error occurs THEN the endpoint SHALL log the error with full context
