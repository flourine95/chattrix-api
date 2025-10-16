package com.chattrix.api.filters;

import com.chattrix.api.entities.User;

import java.security.Principal;

public record UserPrincipal(User user, String token) implements Principal {

    @Override
    public String getName() {
        return user.getUsername();
    }

    public Long getUserId() {
        return user.getId();
    }

}
