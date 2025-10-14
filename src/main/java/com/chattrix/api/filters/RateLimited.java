package com.chattrix.api.filters;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NameBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /**
     * Maximum number of requests allowed
     */
    int maxRequests() default 5;

    /**
     * Time window in seconds
     */
    int windowSeconds() default 60;

    /**
     * Key type for rate limiting
     */
    KeyType keyType() default KeyType.IP;

    enum KeyType {
        IP,           // Rate limit by IP address
        EMAIL,        // Rate limit by email (for email-based endpoints)
        IP_AND_EMAIL  // Rate limit by combination of IP and email
    }
}

