package com.chattrix.api.resources.user;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.UserNoteRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.UserNoteResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.user.UserNoteService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Messenger Notes API
 * Status messages (max 60 chars) that appear on user avatars
 * Auto-expire after 24 hours
 * <p>
 * Reply & React: Mỗi reply/reaction là 1 tin nhắn riêng trong conversation
 */
@Path("/v1/notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserNoteResource {

    @Inject
    private UserNoteService noteService;

    @Inject
    private UserContext userContext;

    /**
     * Create or update current user's note
     * POST /v1/notes
     */
    @POST
    public Response createOrUpdateNote(@Valid UserNoteRequest request) {
        UserNoteResponse response = noteService.createOrUpdateNote(
                userContext.getCurrentUserId(),
                request
        );
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Note created successfully"))
                .build();
    }

    /**
     * Get current user's active note
     * GET /v1/notes/me
     */
    @GET
    @Path("/me")
    public Response getMyNote() {
        Optional<UserNoteResponse> note = noteService.getMyNote(userContext.getCurrentUserId());

        if (note.isPresent()) {
            return Response.ok(ApiResponse.success(note.get(), "Note retrieved successfully")).build();
        } else {
            return Response.ok(ApiResponse.success(null, "No active note found")).build();
        }
    }

    /**
     * Get all active notes from contacts
     * Displayed in inbox/chat list
     * GET /v1/notes/contacts
     */
    @GET
    @Path("/contacts")
    public Response getContactsNotes() {
        List<UserNoteResponse> notes = noteService.getContactsNotes(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(notes, "Notes retrieved successfully")).build();
    }

    /**
     * Get a specific user's note
     * GET /v1/notes/users/{userId}
     */
    @GET
    @Path("/users/{userId}")
    public Response getUserNote(@PathParam("userId") Long userId) {
        Optional<UserNoteResponse> note = noteService.getUserNote(
                userContext.getCurrentUserId(),
                userId
        );

        if (note.isPresent()) {
            return Response.ok(ApiResponse.success(note.get(), "Note retrieved successfully")).build();
        } else {
            return Response.ok(ApiResponse.success(null, "No active note found")).build();
        }
    }

    /**
     * Delete current user's note
     * DELETE /v1/notes
     */
    @DELETE
    public Response deleteNote() {
        noteService.deleteNote(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(null, "Note deleted successfully")).build();
    }

    /**
     * Reply to a note
     * Tạo tin nhắn TEXT trong conversation
     * POST /v1/notes/{noteId}/reply
     * <p>
     * Body: {"replyText": "Nice!"}
     */
    @POST
    @Path("/{noteId}/reply")
    public Response replyToNote(
            @PathParam("noteId") Long noteId,
            Map<String, String> body) {

        String replyText = body.get("replyText");
        if (replyText == null || replyText.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("BAD_REQUEST", "Reply text is required", java.util.UUID.randomUUID().toString()))
                    .build();
        }

        MessageResponse response = noteService.replyToNote(
                userContext.getCurrentUserId(),
                noteId,
                replyText
        );
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Reply sent successfully"))
                .build();
    }

    /**
     * React to a note with emoji
     * Tạo tin nhắn TEXT với nội dung là emoji
     * POST /v1/notes/{noteId}/react
     * <p>
     * Body: {"emoji": "👍"}
     */
    @POST
    @Path("/{noteId}/react")
    public Response reactToNote(
            @PathParam("noteId") Long noteId,
            Map<String, String> body) {

        String emoji = body.get("emoji");
        if (emoji == null || emoji.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("BAD_REQUEST", "Emoji is required", java.util.UUID.randomUUID().toString()))
                    .build();
        }

        MessageResponse response = noteService.reactToNote(
                userContext.getCurrentUserId(),
                noteId,
                emoji
        );
        return Response.ok(ApiResponse.success(response, "Reaction sent successfully")).build();
    }

    /**
     * Get all messages (replies + reactions) for a note
     * Only note owner can see
     * GET /v1/notes/{noteId}/messages
     */
    @GET
    @Path("/{noteId}/messages")
    public Response getNoteMessages(@PathParam("noteId") Long noteId) {
        var messages = noteService.getNoteMessages(
                userContext.getCurrentUserId(),
                noteId
        );
        return Response.ok(ApiResponse.success(messages, "Messages retrieved successfully")).build();
    }
}

