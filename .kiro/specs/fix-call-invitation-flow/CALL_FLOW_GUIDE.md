# Audio/Video Call Flow: Flutter → Jakarta EE

## Overview
Luồng hoàn chỉnh cho audio/video call trong Chattrix từ Flutter client đến Jakarta EE backend.

---

## 🎯 Phase 1: INITIATE CALL (Người gọi bắt đầu cuộc gọi)

### Flutter Client (Caller)

**1. User nhấn nút Call**
```dart
// File: lib/presentation/screens/conversation_detail_screen.dart
// hoặc lib/features/call/presentation/widgets/call_button.dart

void _initiateCall(BuildContext context, String calleeId, CallType callType) async {
  // Tạo channel ID
  final channelId = 'channel_conv_${conversationId}';
  
  // Gọi repository để initiate call
  final result = await ref.read(callRepositoryProvider)
    .initiateCall(
      calleeId: calleeId,
      channelId: channelId,
      callType: callType, // 'audio' hoặc 'video'
    );
    
  result.fold(
    (failure) => _showError(failure.message),
    (callResponse) => _navigateToCallScreen(callResponse),
  );
}
```

**2. Repository gọi API**
```dart
// File: lib/data/repositories/call_repository_impl.dart

@override
Future<Either<Failure, CallResponse>> initiateCall({
  required String calleeId,
  required String channelId,
  required String callType,
}) async {
  try {
    // POST request đến REST API
    final response = await _dio.post(
      '/api/v1/calls/initiate',
      data: {
        'calleeId': calleeId,
        'channelId': channelId,
        'callType': callType, // 'audio' hoặc 'video'
      },
    );
    
    return Right(CallResponse.fromJson(response.data));
  } catch (e) {
    return Left(ServerFailure(e.toString()));
  }
}
```


### Jakarta EE Backend (Server)

**3. REST API nhận request**
```java
// File: src/main/java/com/chattrix/api/resources/CallResource.java

@POST
@Path("/initiate")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response initiateCall(
    @Context SecurityContext securityContext,
    @Valid InitiateCallRequest request
) {
    String callerId = securityContext.getUserPrincipal().getName();
    
    CallResponse response = callService.initiateCall(callerId, request);
    
    return Response.status(Response.Status.CREATED)
            .entity(response)
            .build();
}
```

**4. CallService xử lý logic**
```java
// File: src/main/java/com/chattrix/api/services/CallService.java

@Transactional
public CallResponse initiateCall(String callerId, InitiateCallRequest request) {
    // 4.1. Validate users exist
    User caller = userRepository.findById(Long.parseLong(callerId))
        .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));
    User callee = userRepository.findById(Long.parseLong(request.getCalleeId()))
        .orElseThrow(() -> new ResourceNotFoundException("Callee not found"));
    
    // 4.2. Check if users are contacts
    if (!areContacts(caller.getId(), callee.getId())) {
        throw new UnauthorizedException("Users are not contacts");
    }
    
    // 4.3. Check if users are already in a call
    if (callRepository.findActiveCallByUserId(caller.getId()).isPresent()) {
        throw new BadRequestException("Caller is already in a call");
    }
    if (callRepository.findActiveCallByUserId(callee.getId()).isPresent()) {
        throw new BadRequestException("Callee is already in a call");
    }
    
    // 4.4. Create call record with INITIATING status
    Call call = new Call();
    call.setId(UUID.randomUUID().toString());
    call.setChannelId(request.getChannelId());
    call.setCallerId(caller.getId());
    call.setCalleeId(callee.getId());
    call.setCallType(request.getCallType()); // "audio" or "video"
    call.setStatus(CallStatus.INITIATING);
    call.setCreatedAt(Instant.now());
    
    call = callRepository.save(call);
    
    // 4.5. Send WebSocket invitation to callee
    CallInvitationData invitationData = new CallInvitationData();
    invitationData.setCallId(call.getId());
    invitationData.setChannelId(call.getChannelId());
    invitationData.setCallerId(callerId);
    invitationData.setCallerName(caller.getFullName());
    invitationData.setCallerAvatar(caller.getAvatarUrl());
    invitationData.setCallType(request.getCallType());
    
    webSocketNotificationService.sendCallInvitation(
        request.getCalleeId(), 
        invitationData
    );
    
    // 4.6. Update status to RINGING
    call.setStatus(CallStatus.RINGING);
    call = callRepository.save(call);
    
    // 4.7. Schedule 60-second timeout
    callTimeoutScheduler.scheduleTimeout(
        call.getId(), 
        callerId, 
        request.getCalleeId()
    );
    
    return buildCallResponse(call, caller, callee);
}
```


**5. WebSocketNotificationService gửi invitation**
```java
// File: src/main/java/com/chattrix/api/services/WebSocketNotificationService.java

public void sendCallInvitation(String calleeId, CallInvitationData data) {
    try {
        Long calleeIdLong = Long.parseLong(calleeId);
        
        // Tạo message wrapper
        CallInvitationMessage message = new CallInvitationMessage(data);
        WebSocketMessage<CallInvitationMessage> wsMessage = 
            new WebSocketMessage<>("call_invitation", message);
        
        // Gửi qua ChatSessionService
        chatSessionService.sendMessageToUser(calleeIdLong, wsMessage);
        
        LOGGER.log(Level.INFO, "Sent call invitation to user {0}", calleeId);
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to send call invitation", e);
    }
}
```

**6. ChatSessionService gửi qua WebSocket**
```java
// File: src/main/java/com/chattrix/api/services/ChatSessionService.java

public void sendMessageToUser(Long userId, WebSocketMessage<?> message) {
    Set<Session> sessions = userSessions.get(userId);
    
    if (sessions == null || sessions.isEmpty()) {
        LOGGER.log(Level.WARNING, "No active sessions for user {0}", userId);
        return;
    }
    
    sessions.forEach(session -> {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendObject(message);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send message", e);
        }
    });
}
```

---

## 🎯 Phase 2: RECEIVE INVITATION (Người nhận nhận được cuộc gọi)

### Flutter Client (Callee)

**7. WebSocket listener nhận message**
```dart
// File: lib/data/datasources/websocket_datasource.dart

void _handleWebSocketMessage(dynamic message) {
  final data = jsonDecode(message);
  final messageType = data['type'];
  
  switch (messageType) {
    case 'call_invitation':
      _handleCallInvitation(data['payload']);
      break;
    // ... other cases
  }
}

void _handleCallInvitation(Map<String, dynamic> payload) {
  final invitation = CallInvitationModel.fromJson(payload);
  
  // Emit event hoặc update state
  _callInvitationController.add(invitation);
  
  // Show incoming call UI
  _showIncomingCallNotification(invitation);
}
```

**8. Show incoming call screen**
```dart
// File: lib/features/call/presentation/screens/incoming_call_screen.dart

class IncomingCallScreen extends HookConsumerWidget {
  final CallInvitation invitation;
  
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: Column(
        children: [
          // Caller info
          Text(invitation.callerName),
          CircleAvatar(backgroundImage: NetworkImage(invitation.callerAvatar)),
          Text(invitation.callType == 'audio' ? 'Audio Call' : 'Video Call'),
          
          // Action buttons
          Row(
            children: [
              // Accept button
              IconButton(
                icon: Icon(Icons.call),
                onPressed: () => _acceptCall(context, ref),
              ),
              // Reject button
              IconButton(
                icon: Icon(Icons.call_end),
                onPressed: () => _rejectCall(context, ref),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
```


---

## 🎯 Phase 3: ACCEPT CALL (Người nhận chấp nhận cuộc gọi)

### Flutter Client (Callee)

**9. User nhấn Accept**
```dart
void _acceptCall(BuildContext context, WidgetRef ref) async {
  // Gửi accept message qua WebSocket
  final websocket = ref.read(websocketServiceProvider);
  
  websocket.send({
    'type': 'call.accept',
    'payload': {
      'callId': invitation.callId,
    },
  });
  
  // Navigate to call screen
  Navigator.pushReplacement(
    context,
    MaterialPageRoute(
      builder: (_) => CallScreen(
        callId: invitation.callId,
        channelId: invitation.channelId,
        isVideo: invitation.callType == 'video',
      ),
    ),
  );
}
```

### Jakarta EE Backend (Server)

**10. ChatServerEndpoint nhận message**
```java
// File: src/main/java/com/chattrix/api/websocket/ChatServerEndpoint.java

@OnMessage
@Transactional
public void onMessage(Session session, WebSocketMessage<?> message) {
    Long userId = (Long) session.getUserProperties().get("userId");
    
    switch (message.getType()) {
        case "call.accept" -> processCallAccept(session, userId, message);
        case "call.reject" -> processCallReject(session, userId, message);
        case "call.end" -> processCallEnd(session, userId, message);
        // ... other cases
    }
}

private void processCallAccept(Session session, Long userId, WebSocketMessage<?> message) {
    try {
        // Parse payload
        CallAcceptDto dto = objectMapper.convertValue(
            message.getPayload(), 
            CallAcceptDto.class
        );
        
        // Call service
        callService.acceptCallViaWebSocket(dto.getCallId(), String.valueOf(userId));
        
    } catch (ResourceNotFoundException e) {
        sendCallError(session, null, "call_not_found", e.getMessage());
    } catch (UnauthorizedException e) {
        sendCallError(session, null, "unauthorized", e.getMessage());
    } catch (Exception e) {
        sendCallError(session, null, "service_error", "Unexpected error");
    }
}
```

**11. CallService xử lý accept**
```java
// File: src/main/java/com/chattrix/api/services/CallService.java

@Transactional
public CallResponse acceptCallViaWebSocket(String callId, String userId) {
    // 11.1. Find call
    Call call = callRepository.findById(callId)
        .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    
    // 11.2. Verify user is callee
    if (!call.getCalleeId().equals(Long.parseLong(userId))) {
        throw new UnauthorizedException("User is not the callee");
    }
    
    // 11.3. Check status is RINGING
    if (call.getStatus() != CallStatus.RINGING) {
        throw new BadRequestException("Call cannot be accepted");
    }
    
    // 11.4. Check not timed out (< 60 seconds)
    Duration timeSinceCreation = Duration.between(call.getCreatedAt(), Instant.now());
    if (timeSinceCreation.getSeconds() > 60) {
        throw new BadRequestException("Call has timed out");
    }
    
    // 11.5. Update status to CONNECTING
    call.setStatus(CallStatus.CONNECTING);
    call = callRepository.save(call);
    
    // 11.6. Send notification to caller
    webSocketNotificationService.sendCallAccepted(
        String.valueOf(call.getCallerId()),
        callId,
        userId
    );
    
    return buildCallResponse(call);
}
```


**12. WebSocketNotificationService gửi accepted notification**
```java
public void sendCallAccepted(String callerId, String callId, String acceptedBy) {
    CallAcceptedData data = new CallAcceptedData();
    data.setCallId(callId);
    data.setAcceptedBy(acceptedBy);
    
    CallAcceptedMessage message = new CallAcceptedMessage();
    message.setType("call_accepted");
    message.setData(data);
    message.setTimestamp(Instant.now());
    
    WebSocketMessage<CallAcceptedMessage> wsMessage = 
        new WebSocketMessage<>("call_accepted", message);
    
    chatSessionService.sendMessageToUser(Long.parseLong(callerId), wsMessage);
}
```

### Flutter Client (Caller)

**13. Caller nhận accepted notification**
```dart
// File: lib/data/datasources/websocket_datasource.dart

void _handleWebSocketMessage(dynamic message) {
  final data = jsonDecode(message);
  
  switch (data['type']) {
    case 'call_accepted':
      _handleCallAccepted(data['payload']);
      break;
  }
}

void _handleCallAccepted(Map<String, dynamic> payload) {
  final acceptedData = CallAcceptedModel.fromJson(payload);
  
  // Update call state
  _callStateController.add(CallState.connecting);
  
  // Both users now join Agora channel
}
```

---

## 🎯 Phase 4: CONNECT TO AGORA (Cả 2 người join Agora channel)

### Flutter Client (Both Caller & Callee)

**14. Join Agora channel**
```dart
// File: lib/features/call/presentation/screens/call_screen.dart

class CallScreen extends HookConsumerWidget {
  final String callId;
  final String channelId;
  final bool isVideo;
  
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    useEffect(() {
      _initializeAgora();
      return () => _disposeAgora();
    }, []);
    
    return Scaffold(
      body: Stack(
        children: [
          if (isVideo) _buildVideoView(),
          if (!isVideo) _buildAudioView(),
          _buildControlButtons(),
        ],
      ),
    );
  }
  
  Future<void> _initializeAgora() async {
    // 14.1. Get Agora token from server
    final tokenResult = await ref.read(callRepositoryProvider)
      .generateAgoraToken(
        channelId: channelId,
        userId: currentUserId,
      );
    
    final token = tokenResult.fold(
      (failure) => throw Exception(failure.message),
      (response) => response.token,
    );
    
    // 14.2. Initialize Agora engine
    final agoraService = ref.read(agoraServiceProvider);
    await agoraService.initialize();
    
    // 14.3. Join channel
    await agoraService.joinChannel(
      token: token,
      channelId: channelId,
      uid: currentUserId,
      isVideo: isVideo,
    );
  }
}
```

**15. AgoraService handles media**
```dart
// File: lib/data/services/agora_service.dart

class AgoraService {
  late RtcEngine _engine;
  
  Future<void> initialize() async {
    _engine = createAgoraRtcEngine();
    await _engine.initialize(RtcEngineContext(
      appId: agoraAppId,
    ));
    
    // Register event handlers
    _engine.registerEventHandler(RtcEngineEventHandler(
      onJoinChannelSuccess: _onJoinChannelSuccess,
      onUserJoined: _onUserJoined,
      onUserOffline: _onUserOffline,
      onError: _onError,
    ));
  }
  
  Future<void> joinChannel({
    required String token,
    required String channelId,
    required int uid,
    required bool isVideo,
  }) async {
    if (isVideo) {
      await _engine.enableVideo();
      await _engine.startPreview();
    } else {
      await _engine.enableAudio();
      await _engine.disableVideo();
    }
    
    await _engine.joinChannel(
      token: token,
      channelId: channelId,
      uid: uid,
      options: ChannelMediaOptions(
        clientRoleType: ClientRoleType.clientRoleBroadcaster,
        channelProfile: ChannelProfileType.channelProfileCommunication,
      ),
    );
  }
}
```


### Jakarta EE Backend (Server)

**16. Generate Agora token**
```java
// File: src/main/java/com/chattrix/api/resources/AgoraResource.java

@POST
@Path("/token/generate")
@Secured
@Produces(MediaType.APPLICATION_JSON)
public Response generateToken(
    @Context SecurityContext securityContext,
    @Valid GenerateTokenRequest request
) {
    String userId = securityContext.getUserPrincipal().getName();
    
    AgoraTokenResponse response = agoraTokenService.generateToken(
        userId,
        request.getChannelId(),
        request.getRole()
    );
    
    return Response.ok(response).build();
}
```

```java
// File: src/main/java/com/chattrix/api/services/AgoraTokenService.java

public AgoraTokenResponse generateToken(String userId, String channelId, String role) {
    // Generate UID from userId
    int uid = generateUidFromUserId(Long.parseLong(userId));
    
    // Generate token using Agora SDK
    RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
    int timestamp = (int)(System.currentTimeMillis() / 1000);
    int privilegeExpiredTs = timestamp + 3600; // 1 hour
    
    String token = tokenBuilder.buildTokenWithUid(
        agoraAppId,
        agoraAppCertificate,
        channelId,
        uid,
        role.equals("publisher") ? RtcTokenBuilder.Role.Role_Publisher 
                                  : RtcTokenBuilder.Role.Role_Subscriber,
        privilegeExpiredTs
    );
    
    return new AgoraTokenResponse(token, uid, channelId, privilegeExpiredTs);
}
```

---

## 🎯 Phase 5: END CALL (Kết thúc cuộc gọi)

### Flutter Client (Either User)

**17. User nhấn End Call**
```dart
void _endCall(BuildContext context, WidgetRef ref) async {
  // 17.1. Leave Agora channel
  final agoraService = ref.read(agoraServiceProvider);
  await agoraService.leaveChannel();
  
  // 17.2. Calculate duration
  final duration = DateTime.now().difference(callStartTime).inSeconds;
  
  // 17.3. Send end message via WebSocket
  final websocket = ref.read(websocketServiceProvider);
  websocket.send({
    'type': 'call.end',
    'payload': {
      'callId': callId,
      'durationSeconds': duration,
    },
  });
  
  // 17.4. Navigate back
  Navigator.pop(context);
}
```

### Jakarta EE Backend (Server)

**18. ChatServerEndpoint nhận end message**
```java
private void processCallEnd(Session session, Long userId, WebSocketMessage<?> message) {
    try {
        CallEndDto dto = objectMapper.convertValue(
            message.getPayload(), 
            CallEndDto.class
        );
        
        callService.endCallViaWebSocket(
            dto.getCallId(), 
            String.valueOf(userId), 
            dto.getDurationSeconds()
        );
        
    } catch (Exception e) {
        sendCallError(session, null, "service_error", e.getMessage());
    }
}
```

**19. CallService xử lý end**
```java
@Transactional
public CallResponse endCallViaWebSocket(String callId, String userId, Integer durationSeconds) {
    // 19.1. Find call
    Call call = callRepository.findById(callId)
        .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    
    // 19.2. Verify user is participant
    Long userIdLong = Long.parseLong(userId);
    if (!call.getCallerId().equals(userIdLong) && 
        !call.getCalleeId().equals(userIdLong)) {
        throw new UnauthorizedException("User is not a participant");
    }
    
    // 19.3. Calculate duration if not provided
    Instant endTime = Instant.now();
    if (durationSeconds == null && call.getStartTime() != null) {
        durationSeconds = (int) Duration.between(call.getStartTime(), endTime).getSeconds();
    }
    
    // 19.4. Update call status to ENDED
    call.setStatus(CallStatus.ENDED);
    call.setEndTime(endTime);
    call.setDurationSeconds(durationSeconds);
    call = callRepository.save(call);
    
    // 19.5. Notify other participant
    Long otherUserId = call.getCallerId().equals(userIdLong) 
        ? call.getCalleeId() 
        : call.getCallerId();
    
    webSocketNotificationService.sendCallEnded(
        String.valueOf(otherUserId),
        callId,
        userId,
        durationSeconds
    );
    
    // 19.6. Create call history entries
    createCallHistoryEntries(call);
    
    return buildCallResponse(call);
}
```


### Flutter Client (Other User)

**20. Other user nhận end notification**
```dart
void _handleCallEnded(Map<String, dynamic> payload) {
  final endedData = CallEndedModel.fromJson(payload);
  
  // Leave Agora channel
  final agoraService = ref.read(agoraServiceProvider);
  agoraService.leaveChannel();
  
  // Show notification
  _showSnackbar('Call ended by ${endedData.endedBy}');
  
  // Navigate back
  Navigator.pop(context);
}
```

---

## 🎯 Phase 6: REJECT CALL (Từ chối cuộc gọi)

### Flutter Client (Callee)

**21. User nhấn Reject**
```dart
void _rejectCall(BuildContext context, WidgetRef ref) {
  final websocket = ref.read(websocketServiceProvider);
  
  websocket.send({
    'type': 'call.reject',
    'payload': {
      'callId': invitation.callId,
      'reason': 'declined', // 'declined', 'busy', 'unavailable'
    },
  });
  
  Navigator.pop(context);
}
```

### Jakarta EE Backend (Server)

**22. Process reject**
```java
private void processCallReject(Session session, Long userId, WebSocketMessage<?> message) {
    try {
        CallRejectDto dto = objectMapper.convertValue(
            message.getPayload(), 
            CallRejectDto.class
        );
        
        callService.rejectCallViaWebSocket(
            dto.getCallId(), 
            String.valueOf(userId), 
            dto.getReason()
        );
        
    } catch (Exception e) {
        sendCallError(session, null, "service_error", e.getMessage());
    }
}
```

```java
@Transactional
public CallResponse rejectCallViaWebSocket(String callId, String userId, String reason) {
    Call call = callRepository.findById(callId)
        .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    
    // Verify user is callee
    if (!call.getCalleeId().equals(Long.parseLong(userId))) {
        throw new UnauthorizedException("User is not the callee");
    }
    
    // Update status to REJECTED
    call.setStatus(CallStatus.REJECTED);
    call.setEndTime(Instant.now());
    call = callRepository.save(call);
    
    // Notify caller
    webSocketNotificationService.sendCallRejected(
        String.valueOf(call.getCallerId()),
        callId,
        userId,
        reason
    );
    
    return buildCallResponse(call);
}
```

### Flutter Client (Caller)

**23. Caller nhận rejected notification**
```dart
void _handleCallRejected(Map<String, dynamic> payload) {
  final rejectedData = CallRejectedModel.fromJson(payload);
  
  // Show notification
  String message = switch (rejectedData.reason) {
    'declined' => 'Call declined',
    'busy' => 'User is busy',
    'unavailable' => 'User is unavailable',
    _ => 'Call rejected',
  };
  
  _showSnackbar(message);
  
  // Navigate back
  Navigator.pop(context);
}
```

---

## 📊 Complete Flow Diagram

```
CALLER (Flutter)                    SERVER (Jakarta EE)                    CALLEE (Flutter)
     |                                      |                                      |
     | 1. POST /api/v1/calls/initiate      |                                      |
     |------------------------------------->|                                      |
     |                                      | 2. Create Call (INITIATING)         |
     |                                      | 3. Save to DB                       |
     |                                      | 4. Update to RINGING                |
     |                                      |                                      |
     |                                      | 5. WebSocket: call_invitation       |
     |                                      |------------------------------------->|
     |                                      |                                      | 6. Show incoming call UI
     |                                      |                                      |
     |                                      | 7. WebSocket: call.accept           |
     |                                      |<-------------------------------------|
     |                                      | 8. Update Call (CONNECTING)         |
     |                                      |                                      |
     | 9. WebSocket: call_accepted         |                                      |
     |<-------------------------------------|                                      |
     |                                      |                                      |
     | 10. GET /api/v1/agora/token         |                                      |
     |------------------------------------->|                                      |
     | 11. Agora Token Response            |                                      |
     |<-------------------------------------|                                      |
     |                                      |                                      | 12. GET /api/v1/agora/token
     |                                      |<-------------------------------------|
     |                                      | 13. Agora Token Response            |
     |                                      |------------------------------------->|
     |                                      |                                      |
     | 14. Join Agora Channel              |                                      | 15. Join Agora Channel
     |================== AGORA MEDIA STREAM (Audio/Video) ========================|
     |                                      |                                      |
     | 16. WebSocket: call.end             |                                      |
     |------------------------------------->|                                      |
     |                                      | 17. Update Call (ENDED)             |
     |                                      | 18. Create Call History             |
     |                                      |                                      |
     |                                      | 19. WebSocket: call_ended           |
     |                                      |------------------------------------->|
     |                                      |                                      | 20. Leave Agora & Close UI
     | 21. Leave Agora & Close UI          |                                      |
```

---

## 🔑 Key Points

### ✅ ĐÚNG (Correct Flow)
1. **Initiate**: Client gọi REST API `/api/v1/calls/initiate`
2. **Invitation**: Server gửi WebSocket `call_invitation` đến callee
3. **Accept/Reject**: Client gửi WebSocket `call.accept` hoặc `call.reject`
4. **Notification**: Server gửi WebSocket `call_accepted` hoặc `call_rejected` đến caller
5. **Media**: Cả 2 join Agora channel với token từ server
6. **End**: Client gửi WebSocket `call.end`, server notify người kia

### ❌ SAI (Wrong Flow - Current Bug)
1. ❌ Client gửi WebSocket `call.invitation` (KHÔNG ĐƯỢC PHÉP)
2. ❌ Server không tạo call record
3. ❌ Callee không nhận được notification

---

## 📝 Message Types Summary

### Client → Server (WebSocket)
- `call.accept` - Accept incoming call
- `call.reject` - Reject incoming call  
- `call.end` - End active call
- ❌ `call.invitation` - NOT ALLOWED (use REST API instead)

### Server → Client (WebSocket)
- `call_invitation` - Incoming call notification
- `call_accepted` - Call was accepted
- `call_rejected` - Call was rejected
- `call_ended` - Call was ended
- `call_timeout` - Call timed out (60s)
- `call_error` - Error occurred

### Client → Server (REST API)
- `POST /api/v1/calls/initiate` - Start new call
- `POST /api/v1/agora/token/generate` - Get Agora token

---

## 🐛 Debugging Tips

1. **Check WebSocket connection**: User phải connected trước khi nhận invitation
2. **Check logs**: Server log sẽ show "Sent call invitation to user X"
3. **Check call status**: Call phải ở status RINGING để accept được
4. **Check timeout**: Call chỉ có 60s để accept
5. **Check Agora token**: Token phải valid và chưa expired

---

Đây là luồng hoàn chỉnh! Vấn đề hiện tại của bạn là client đang gửi `call.invitation` qua WebSocket thay vì gọi REST API. Hãy fix theo spec đã tạo ở trên.
