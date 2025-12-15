package com.chattrix.api.entities;

/**
 * Enum representing the status of a call following the state machine.
 * <p>
 * State transitions:
 * INITIATING -> RINGING -> CONNECTING -> CONNECTED -> DISCONNECTING -> ENDED
 * RINGING can also transition to REJECTED or MISSED
 * Any state can transition to FAILED
 */
public enum CallStatus {
    /**
     * Call is being initiated by the caller
     */
    INITIATING,

    /**
     * Call invitation sent to callee, waiting for response
     */
    RINGING,

    /**
     * Callee accepted, establishing connection
     */
    CONNECTING,

    /**
     * Call is active and connected
     */
    CONNECTED,

    /**
     * Call is being disconnected
     */
    DISCONNECTING,

    /**
     * Call has ended normally
     */
    ENDED,

    /**
     * Call was not answered within timeout period
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
