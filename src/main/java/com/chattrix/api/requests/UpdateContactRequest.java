package com.chattrix.api.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateContactRequest {
    public String nickname;
    @JsonProperty("isFavorite")
    public Boolean isFavorite;
}

