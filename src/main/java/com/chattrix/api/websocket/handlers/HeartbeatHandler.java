package com.chattrix.api.websocket.handlers;

import com.chattrix.api.services.user.HeartbeatMonitorService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.HeartbeatAckDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;

@ApplicationScoped
@Slf4j
public class HeartbeatHandler {

    @Inject
    private HeartbeatMonitorService heartbeatMonitorService;

    public void handleHeartbeat(Session session, Long userId) throws IOException, EncodeException {
        heartbeatMonitorService.recordHeartbeat(userId);

        HeartbeatAckDto ackPayload = HeartbeatAckDto.builder()
                .userId(userId)
                .timestamp(Instant.now())
                .build();
        WebSocketMessage<HeartbeatAckDto> ack = new WebSocketMessage<>(WebSocketEventType.HEARTBEAT_ACK, ackPayload);

        session.getBasicRemote().sendObject(ack);
    }
}
