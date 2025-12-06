package com.chattrix.api.resources.auth;

import com.chattrix.api.filters.RateLimited;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.AuthResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.AuthService;
import com.chattrix.api.services.VerificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @Inject
    private UserContext userContext;

    @Inject
    private VerificationService verificationService;

    @POST
    @Path("/register")
    @RateLimited(maxRequests = 3, windowSeconds = 300)
    public Response register(@Valid RegisterRequest request) {
        authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(null, "Registration successful. Please check your email to verify your account."))
                .build();
    }

    @POST
    @Path("/login")
    @RateLimited(maxRequests = 10)
    public Response login(@Valid LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return Response.ok(ApiResponse.success(authResponse, "Login successful")).build();
    }

    @GET
    @Path("/me")
    @Secured
    public Response getCurrentUser() {
        UserResponse userDto = authService.getCurrentUserById(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(userDto, "User retrieved successfully")).build();
    }

    @POST
    @Path("/logout")
    @Secured
    public Response logout() {
        authService.logout(userContext.getCurrentUserId(), userContext.getToken());
        return Response.ok(ApiResponse.success(null, "Logged out from this device successfully")).build();
    }

    @POST
    @Path("/logout-all")
    @Secured
    public Response logoutAllDevices() {
        authService.logoutAllDevices(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(null, "Logged out from all devices successfully")).build();
    }

    @POST
    @Path("/refresh")
    public Response refreshToken(@Valid RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        return Response.ok(ApiResponse.success(authResponse, "Token refreshed successfully")).build();
    }

    @PUT
    @Path("/change-password")
    @Secured
    public Response changePassword(@Valid ChangePasswordRequest request) {
        authService.changePassword(userContext.getCurrentUserId(), request);
        return Response.ok(ApiResponse.success(null, "Password changed successfully")).build();
    }

    @POST
    @Path("/verify-email")
    @RateLimited(windowSeconds = 300)
    public Response verifyEmail(@Valid VerifyEmailRequest request) {
        verificationService.verifyEmail(request);
        return Response.ok(ApiResponse.success(null, "Email verified successfully. You can now login.")).build();
    }

    @POST
    @Path("/resend-verification")
    @RateLimited(maxRequests = 3, windowSeconds = 600)
    public Response resendVerification(@Valid ResendVerificationRequest request) {
        verificationService.sendVerificationEmail(request);
        return Response.ok(ApiResponse.success(null, "Verification email sent successfully")).build();
    }

    @POST
    @Path("/forgot-password")
    @RateLimited(maxRequests = 3, windowSeconds = 600)
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        verificationService.sendPasswordResetEmail(request);
        return Response.ok(ApiResponse.success(null, "Password reset email sent successfully")).build();
    }

    @POST
    @Path("/reset-password")
    @RateLimited(windowSeconds = 300)
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        verificationService.resetPassword(request);
        return Response.ok(ApiResponse.success(null, "Password reset successfully")).build();
    }
}
