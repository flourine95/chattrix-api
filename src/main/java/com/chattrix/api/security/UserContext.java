package com.chattrix.api.security;

import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.filters.UserPrincipal;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@RequestScoped
public class UserContext {

    @Context
    private SecurityContext securityContext;

    private UserPrincipal getPrincipal() {
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