package com.bogusz.bicyclerouteplannerbackend.exception;

public class InvalidGeometryException extends RuntimeException {
    public InvalidGeometryException(String message, Throwable cause) {
        super(message, cause);
    }
}
