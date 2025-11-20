package com.chattrix.api.entities;

/**
 * Enum representing the direction of a call from a user's perspective.
 */
public enum CallDirection {
    /**
     * Call received from another user
     */
    INCOMING,
    
    /**
     * Call initiated to another user
     */
    OUTGOING
}
