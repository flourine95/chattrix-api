package com.chattrix.api.filters;

import com.chattrix.api.config.JacksonConfig;
import com.chattrix.api.responses.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Provider
@RateLimited
@Priority(Priorities.USER - 100)
public class RateLimitedFilter implements ContainerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new JacksonConfig().getContext(null);
    private final Map<String, Map<Long, AtomicInteger>> rateLimitCache = new ConcurrentHashMap<>();
    @Context
    private ResourceInfo resourceInfo;
    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return;
        }

        RateLimited rateLimited = method.getAnnotation(RateLimited.class);
        if (rateLimited == null) {
            return;
        }

        String key = generateKey(requestContext);

        int maxRequests = rateLimited.maxRequests();
        int windowSeconds = rateLimited.windowSeconds();

        if (!isAllowed(key, maxRequests, windowSeconds)) {
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Too many requests. Please try again in " + windowSeconds + " seconds.",
                    "RATE_LIMIT_EXCEEDED"
            );

            requestContext.abortWith(
                    Response.status(Response.Status.TOO_MANY_REQUESTS)
                            .entity(OBJECT_MAPPER.writeValueAsString(errorResponse))
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }
    }

    private String generateKey(ContainerRequestContext requestContext) {
        String endpoint = requestContext.getUriInfo().getPath();
        String clientIp = getClientIp();
        return endpoint + ":" + clientIp;
    }

    private boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        long currentWindow = System.currentTimeMillis() / (windowSeconds * 1000L);

        Map<Long, AtomicInteger> windowCounts = rateLimitCache.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        windowCounts.entrySet().removeIf(entry -> entry.getKey() < currentWindow - 1);

        AtomicInteger count = windowCounts.computeIfAbsent(currentWindow, k -> new AtomicInteger(0));

        return count.incrementAndGet() <= maxRequests;
    }

    private String getClientIp() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
