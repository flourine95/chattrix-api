package com.chattrix.api.entities;

/**
 * Enum representing the final status of a call in history.
 * This is a simplified status compared to CallStatus, focusing on the outcome.
 */
public enum CallHistoryStatus {
    /**
     * Call was successfully completed
     */
    COMPLETED,
    
    /**
     * Call was not answered (timed out)
     */
    MISSED,
    
    /**
     * Call was rejected by the callee
     */
    REJECTED,
    
    /**
     * Call failed due to an error
     */
    FAILED
}
