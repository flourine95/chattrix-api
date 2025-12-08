package com.chattrix.api.security;

import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.filters.AuthenticationFilter;
import com.chattrix.api.filters.UserPrincipal;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;

@RequestScoped
public class UserContext {

    @Inject
    private HttpServletRequest request;

    private UserPrincipal getPrincipal() {
        SecurityContext securityContext = (SecurityContext) request.getAttribute(AuthenticationFilter.SECURITY_CONTEXT_ATTRIBUTE);

        if (securityContext != null && securityContext.getUserPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("You must be logged in to perform this action");
    }

    public Long getCurrentUserId() {
        return getPrincipal().getUserId();
    }

    public String getToken() {
        return getPrincipal().token();
    }

    public User getCurrentUser() {
        return getPrincipal().user();
    }
}