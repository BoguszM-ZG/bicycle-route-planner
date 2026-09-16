package com.bogusz.bicyclerouteplannerbackend.exception;

public class AvoidanceZoneNotFoundException extends RuntimeException {
    public AvoidanceZoneNotFoundException(String message) {
        super(message);
    }
}
