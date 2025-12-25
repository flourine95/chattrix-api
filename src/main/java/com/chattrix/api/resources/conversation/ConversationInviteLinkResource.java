package com.chattrix.api.resources.conversation;

import com.chattrix.api.config.AppConfig;
import com.chattrix.api.entities.GroupInviteLink;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateInviteLinkRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.InviteLinkResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.invite.GroupInviteLinkService;
import com.chattrix.api.services.invite.QRCodeService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/v1/conversations/{conversationId}/invite-links")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationInviteLinkResource {

    @Inject
    private GroupInviteLinkService inviteLinkService;

    @Inject
    private QRCodeService qrCodeService;

    @Inject
    private UserContext userContext;

    @Inject
    private AppConfig appConfig;

    /**
     * Create invite link
     */
    @POST
    public Response createInviteLink(
            @PathParam("conversationId") Long conversationId,
            @Valid CreateInviteLinkRequest request) {

        Long userId = userContext.getCurrentUserId();
        InviteLinkResponse response = inviteLinkService.createInviteLink(userId, conversationId, request);

        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Invite link created successfully"))
                .build();
    }

    /**
     * Get all invite links for a conversation with cursor-based pagination
     */
    @GET
    public Response getInviteLinks(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("includeRevoked") @DefaultValue("false") boolean includeRevoked) {

        Long userId = userContext.getCurrentUserId();
        CursorPaginatedResponse<InviteLinkResponse> result = inviteLinkService.getInviteLinks(
                userId, conversationId, cursor, limit, includeRevoked);

        return Response.ok(ApiResponse.success(result, "Invite links retrieved successfully")).build();
    }

    /**
     * Get specific invite link
     */
    @GET
    @Path("/{linkId}")
    public Response getInviteLink(
            @PathParam("conversationId") Long conversationId,
            @PathParam("linkId") Long linkId) {

        Long userId = userContext.getCurrentUserId();
        InviteLinkResponse response = inviteLinkService.getInviteLinkById(userId, conversationId, linkId);

        return Response.ok(ApiResponse.success(response, "Invite link retrieved successfully")).build();
    }

    /**
     * Revoke invite link
     */
    @DELETE
    @Path("/{linkId}")
    public Response revokeInviteLink(
            @PathParam("conversationId") Long conversationId,
            @PathParam("linkId") Long linkId) {

        Long userId = userContext.getCurrentUserId();
        InviteLinkResponse response = inviteLinkService.revokeInviteLink(userId, conversationId, linkId);

        return Response.ok(ApiResponse.success(response, "Invite link revoked successfully")).build();
    }

    /**
     * Get QR code for invite link
     */
    @GET
    @Path("/{linkId}/qr")
    @Produces("image/png")
    public Response getQRCode(
            @PathParam("conversationId") Long conversationId,
            @PathParam("linkId") Long linkId,
            @QueryParam("size") @DefaultValue("300") int size,
            @QueryParam("apiUrl") String apiUrl) {

        try {
            GroupInviteLink inviteLink = inviteLinkService.getInviteLink(linkId);

            if (inviteLink == null || !inviteLink.getConversation().getId().equals(conversationId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("NOT_FOUND", "Invite link not found", null))
                        .build();
            }

            // Determine API URL: query param > env variable > default localhost
            String baseUrl = apiUrl;
            System.out.println("baseUrl:" + appConfig.get("app.base.url"));
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = appConfig.get("app.base.url");
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = "http://localhost:8080";
                }
            }
            System.out.println("baseUrl2:" + baseUrl);


            // Generate QR code
            String inviteUrl = baseUrl + "/join/" + inviteLink.getToken();
            byte[] qrCode = qrCodeService.generateQRCode(inviteUrl, size, size);

            return Response.ok(qrCode)
                    .type("image/png")
                    .header("Content-Disposition", "inline; filename=\"invite-qr.png\"")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("QR_GENERATION_FAILED", "Failed to generate QR code", null))
                    .build();
        }
    }
}

