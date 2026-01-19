package com.chattrix.api.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Response health() {
        HealthResponse response = new HealthResponse(
            "ok",
            "Chattrix API is running",
            Instant.now().toString(),
            "1.0.0"
        );
        
        return Response.ok(response).build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String message;
        private String timestamp;
        private String version;
    }
}
