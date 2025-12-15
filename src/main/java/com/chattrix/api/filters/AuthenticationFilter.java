package com.chattrix.api.filters;

import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.auth.TokenService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    public static final String SECURITY_CONTEXT_ATTRIBUTE = "CUSTOM_SECURITY_CONTEXT";
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    @Inject
    private TokenService tokenService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            throw BusinessException.unauthorized("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();

        try {
            if (!tokenService.validateToken(token)) {
                throw BusinessException.unauthorized("Invalid or expired token");
            }

            Long userId = tokenService.getUserIdFromToken(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> BusinessException.unauthorized("User not found"));

            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            final UserPrincipal userPrincipal = new UserPrincipal(user, token);

            SecurityContext newSecurityContext = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return userPrincipal;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return true;
                }

                @Override
                public boolean isSecure() {
                    return currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return AUTHENTICATION_SCHEME;
                }
            };

            requestContext.setSecurityContext(newSecurityContext);

            // Store in request attribute for CDI beans to access
            request.setAttribute(SECURITY_CONTEXT_ATTRIBUTE, newSecurityContext);

        } catch (BusinessException e) {
            // Re-throw UnauthorizedException to be handled by BusinessExceptionMapper
            throw e;
        } catch (Exception e) {
            throw BusinessException.unauthorized("Authentication failed");
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }
}





