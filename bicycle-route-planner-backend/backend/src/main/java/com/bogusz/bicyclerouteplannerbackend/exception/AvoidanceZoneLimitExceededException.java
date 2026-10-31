package com.bogusz.bicyclerouteplannerbackend.exception;

public class AvoidanceZoneLimitExceededException extends RuntimeException {
    public AvoidanceZoneLimitExceededException(String message) {
        super(message);
    }
}
