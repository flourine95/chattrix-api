package com.chattrix.api.entities;

/**
 * Enum representing the network quality rating for a call.
 * Based on Agora's network quality indicators.
 */
public enum NetworkQuality {
    /**
     * Excellent network quality - no issues
     */
    EXCELLENT,
    
    /**
     * Good network quality - minor issues
     */
    GOOD,
    
    /**
     * Poor network quality - noticeable issues
     */
    POOR,
    
    /**
     * Bad network quality - significant issues
     */
    BAD,
    
    /**
     * Very bad network quality - severe issues
     */
    VERY_BAD,
    
    /**
     * Network quality unknown or not measured
     */
    UNKNOWN
}
