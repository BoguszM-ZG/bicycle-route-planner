package com.bogusz.bicyclerouteplannerbackend.exception;

public class GpxExportException extends RuntimeException {
    public GpxExportException(String message) {
        super(message);
    }

    public GpxExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
