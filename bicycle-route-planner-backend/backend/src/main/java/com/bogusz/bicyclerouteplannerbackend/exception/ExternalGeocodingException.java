package com.bogusz.bicyclerouteplannerbackend.exception;

public class ExternalGeocodingException extends RuntimeException {
    public ExternalGeocodingException(String message) {
        super(message);
    }
}
