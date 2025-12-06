package com.chattrix.api.resources.call;

import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CallHistoryResponse;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.security.UserContext; // Import UserContext
import com.chattrix.api.services.call.CallHistoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/calls/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallHistoryResource {

    @Inject
    private CallHistoryService callHistoryService;

    @Inject
    private UserContext userContext;

    @GET
    public Response getCallHistory(@QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("size") @DefaultValue("20") int size,
                                   @QueryParam("callType") String callTypeStr,
                                   @QueryParam("status") String statusStr) {

        // Lấy ID cực gọn
        String userId = String.valueOf(userContext.getCurrentUserId());

        // Parse params (Nên tách ra helper method hoặc để Service lo nếu muốn Resource sạch hơn)
        CallType callType = parseEnum(CallType.class, callTypeStr, "callType");
        CallHistoryStatus status = parseEnum(CallHistoryStatus.class, statusStr, "status");

        PaginatedResponse<CallHistoryResponse> response = callHistoryService.getCallHistory(
                userId, page, size, callType, status
        );

        return Response.ok(ApiResponse.success(response, "Call history retrieved successfully")).build();
    }

    @DELETE
    @Path("/{callId}")
    public Response deleteCallHistory(@PathParam("callId") String callId) {
        String userId = String.valueOf(userContext.getCurrentUserId());

        callHistoryService.deleteCallHistory(userId, callId);

        return Response.ok(ApiResponse.success(null, "Call history deleted successfully")).build();
    }

    // Helper method để parse Enum tránh lặp code try-catch
    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String paramName) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + paramName + ": " + value);
        }
    }
}