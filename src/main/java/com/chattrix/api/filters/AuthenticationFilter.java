package com.chattrix.api.filters;

import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.TokenService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
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

    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Inject
    private TokenService tokenService;

    @Inject
    private UserRepository userRepository;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();

        try {
            if (!tokenService.validateToken(token)) {
                throw new UnauthorizedException("Invalid or expired token");
            }

            Long userId = tokenService.getUserIdFromToken(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            final UserPrincipal userPrincipal = new UserPrincipal(user, token);

            requestContext.setSecurityContext(new SecurityContext() {
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
            });

        } catch (UnauthorizedException e) {
            // Re-throw UnauthorizedException to be handled by BusinessExceptionMapper
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Authentication failed");
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }
}
