package com.chattrix.api.websocket;

public final class WebSocketEventType {

    public static final String CHAT_MESSAGE = "chat.message";
    public static final String MESSAGE_UPDATED = "message.updated";
    public static final String MESSAGE_DELETED = "message.deleted";
    public static final String MESSAGE_MENTION = "message.mention";
    public static final String MESSAGE_REACTION = "message.reaction";
    public static final String MESSAGE_PIN = "message.pin";

    public static final String CONVERSATION_UPDATE = "conversation.update";
    public static final String TYPING_INDICATOR = "typing.indicator";

    public static final String USER_STATUS = "user.status";
    public static final String HEARTBEAT_ACK = "heartbeat.ack";

    public static final String FRIEND_REQUEST_RECEIVED = "friend.request.received";
    public static final String FRIEND_REQUEST_ACCEPTED = "friend.request.accepted";
    public static final String FRIEND_REQUEST_REJECTED = "friend.request.rejected";
    public static final String FRIEND_REQUEST_CANCELLED = "friend.request.cancelled";

    public static final String CALL_INCOMING = "call.incoming";
    public static final String CALL_ACCEPTED = "call.accepted";
    public static final String CALL_REJECTED = "call.rejected";
    public static final String CALL_ENDED = "call.ended";
    public static final String CALL_TIMEOUT = "call.timeout";
    public static final String CALL_PARTICIPANT_UPDATE = "call.participant_update";

    public static final String SCHEDULED_MESSAGE_SENT = "scheduled.message.sent";
    public static final String SCHEDULED_MESSAGE_FAILED = "scheduled.message.failed";

    public static final String ANNOUNCEMENT_CREATED = "announcement.created";
    public static final String ANNOUNCEMENT_DELETED = "announcement.deleted";

    public static final String POLL_CREATED = "poll.created";
    public static final String POLL_VOTED = "poll.voted";
    public static final String POLL_CLOSED = "poll.closed";
    public static final String POLL_UPDATED = "poll.updated";

    public static final String EVENT_CREATED = "event.created";
    public static final String EVENT_UPDATED = "event.updated";
    public static final String EVENT_DELETED = "event.deleted";
    public static final String EVENT_RSVP = "event.rsvp";

}
