# Implementation Plan - API Spec Audit & Fix

## Phase 1: Schema Fixes ✅ COMPLETE

- [x] 1. Fix Error Response Schema
  - Update ErrorResponse to use flat structure
  - Remove nested `error` object
  - Add `data: null` field
  - _Requirements: 1.1, 1.3, 1.4_

- [x] 2. Add NON_NULL Documentation
  - Add section in API description about Jackson NON_NULL behavior
  - Explain null fields are omitted from responses
  - _Requirements: 2.5_

- [x] 3. Fix Request Schema Names
  - Rename UpdateMessageRequest to EditMessageRequest
  - Verify all other request schema names match code
  - _Requirements: 2.1_

- [x] 4. Verify All Response Schemas
  - Compare UserResponse with User entity
  - Compare MessageResponse with Message entity
  - Compare ConversationResponse with Conversation entity
  - Compare ContactResponse with Contact entity
  - Compare CallResponse with Call entity
  - Add missing fields if any
  - _Requirements: 2.1, 2.4_

## Phase 2: Fix Error Response Examples ✅ COMPLETE

- [x] 5. Fix Authentication Endpoints Error Examples
  - /v1/auth/register - Fix all error examples
  - /v1/auth/login - Fix all error examples
  - /v1/auth/refresh - Fix all error examples
  - /v1/auth/me - Fix error examples
  - /v1/auth/logout - Fix error examples
  - /v1/auth/logout-all - Fix error examples
  - /v1/auth/change-password - Fix error examples
  - /v1/auth/verify-email - Fix error examples
  - /v1/auth/resend-verification - Fix error examples
  - /v1/auth/forgot-password - Fix error examples
  - /v1/auth/reset-password - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 6. Fix User Endpoints Error Examples
  - /v1/users/search - Fix error examples
  - /v1/users/status/batch - Fix error examples
  - /v1/users/status - Fix error examples
  - /v1/profile/me (GET, PUT) - Fix error examples
  - /v1/profile/{userId} - Fix error examples
  - /v1/profile/username/{username} - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 7. Fix Conversation Endpoints Error Examples
  - /v1/conversations (GET, POST) - Fix error examples
  - /v1/conversations/{id} (GET, PUT, DELETE) - Fix error examples
  - /v1/conversations/{id}/messages/search - Fix error examples
  - /v1/conversations/{id}/media - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 8. Fix Message Endpoints Error Examples
  - /v1/conversations/{id}/messages (GET, POST) - Fix error examples
  - /v1/conversations/{id}/messages/{id} (GET, PUT, DELETE) - Fix error examples
  - /v1/conversations/{id}/messages/{id}/history - Fix error examples
  - /v1/conversations/{id}/messages/forward - Fix error examples
  - /v1/conversations/{id}/messages/pinned - Fix error examples
  - /v1/conversations/{id}/messages/{id}/pin - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 9. Fix Reaction Endpoints Error Examples
  - /v1/messages/{id}/reactions (GET, POST) - Fix error examples
  - /v1/messages/{id}/reactions/{emoji} (DELETE) - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 10. Fix Read Receipt Endpoints Error Examples
  - /v1/read-receipts/messages/{id} (GET, POST) - Fix error examples
  - /v1/read-receipts/conversations/{id} (POST) - Fix error examples
  - /v1/read-receipts/unread-count - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 11. Fix Call Endpoints Error Examples
  - /v1/calls/initiate - Fix error examples
  - /v1/calls/{id}/accept - Fix error examples
  - /v1/calls/{id}/reject - Fix error examples
  - /v1/calls/{id}/end - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 12. Fix Contact Endpoints Error Examples
  - /v1/contacts (GET, POST) - Fix error examples
  - /v1/contacts/favorites - Fix error examples
  - /v1/contacts/{id} (PUT, DELETE) - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 13. Fix Friend Request Endpoints Error Examples
  - /v1/friend-requests (POST) - Fix error examples
  - /v1/friend-requests/received - Fix error examples
  - /v1/friend-requests/sent - Fix error examples
  - /v1/friend-requests/{id}/accept - Fix error examples
  - /v1/friend-requests/{id}/reject - Fix error examples
  - /v1/friend-requests/{id} (DELETE) - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 14. Fix Notes Endpoints Error Examples
  - /v1/notes (GET, POST, DELETE) - Fix error examples
  - /v1/notes/contacts - Fix error examples
  - /v1/notes/user/{id} - Fix error examples
  - _Requirements: 1.1, 1.2, 1.3_

## Phase 3: Add Missing Endpoints

- [x] 15. Add Call History Endpoints





  - GET /v1/calls/history - Get call history with pagination and filters
  - DELETE /v1/calls/history/{callId} - Delete call history entry
  - Add CallHistoryResponse schema if missing
  - Add PaginatedResponse wrapper
  - _Requirements: 4.1_

- [x] 16. Add Conversation Member Endpoints





  - GET /v1/conversations/{id}/members - Get conversation members
  - POST /v1/conversations/{id}/members - Add members to conversation
  - DELETE /v1/conversations/{id}/members/{userId} - Remove member
  - POST /v1/conversations/{id}/members/leave - Leave conversation
  - PUT /v1/conversations/{id}/members/{userId}/role - Update member role
  - Add ConversationMemberResponse schema if missing
  - Add AddMembersRequest and UpdateMemberRoleRequest schemas
  - _Requirements: 4.1_

- [x] 17. Add Conversation Settings Endpoints




  - GET /v1/conversations/{id}/settings - Get conversation settings
  - PUT /v1/conversations/{id}/settings - Update conversation settings
  - POST /v1/conversations/{id}/settings/mute - Mute conversation
  - POST /v1/conversations/{id}/settings/unmute - Unmute conversation
  - POST /v1/conversations/{id}/settings/pin - Pin conversation
  - POST /v1/conversations/{id}/settings/unpin - Unpin conversation
  - POST /v1/conversations/{id}/settings/archive - Archive conversation
  - POST /v1/conversations/{id}/settings/unarchive - Unarchive conversation
  - POST /v1/conversations/{id}/settings/hide - Hide conversation
  - POST /v1/conversations/{id}/settings/unhide - Unhide conversation
  - POST /v1/conversations/{id}/settings/block - Block user in conversation
  - POST /v1/conversations/{id}/settings/unblock - Unblock user in conversation
  - Add ConversationSettingsResponse schema if missing
  - Add UpdateConversationSettingsRequest schema
  - _Requirements: 4.1_


- [x] 18. Add Typing Indicator Endpoints



  - POST /v1/conversations/{id}/typing/start - Start typing indicator
  - POST /v1/conversations/{id}/typing/stop - Stop typing indicator
  - GET /v1/conversations/{id}/typing/status - Get typing users
  - _Requirements: 4.1_

## Phase 4: Verify Status Codes

- [x] 19. Check POST Endpoints Return 201





  - Verify all POST endpoints that create resources return 201
  - Update any that incorrectly show 200
  - _Requirements: 5.1_


- [x] 20. Verify DELETE Endpoints Return Correct Status




  - Verify DELETE endpoints return 200 with success message or 204 No Content
  - _Requirements: 5.3_


- [x] 21. Verify Error Status Codes




  - 400 for validation errors
  - 401 for authentication errors
  - 403 for authorization errors
  - 404 for not found errors
  - _Requirements: 5.4, 5.5, 5.6, 5.7_

## Phase 5: Final Validation

- [x] 22. Run OpenAPI Validator





  - Use openapi-generator or swagger-cli to validate spec
  - Fix any validation errors
  - _Requirements: All_

- [x] 23. Verify Examples Are Realistic






  - Check all examples use realistic data
  - Ensure consistency across examples
  - _Requirements: All_


- [x] 24. Final Review





  - Read through entire spec
  - Check for consistency
  - Verify documentation is clear
  - _Requirements: All_
