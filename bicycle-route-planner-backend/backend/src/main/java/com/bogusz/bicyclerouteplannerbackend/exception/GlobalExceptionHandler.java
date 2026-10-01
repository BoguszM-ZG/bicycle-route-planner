package com.bogusz.bicyclerouteplannerbackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String ERRORS_PROPERTY = "validationErrors";
    private static final URI DEFAULT_TYPE = URI.create("about:blank");


    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException() {
        log.warn("Invalid credentials provided");
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authorization Error",
                "Invalid email or password", "bad-credentials");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), "user-not-found");
    }

    @ExceptionHandler(InvalidGeometryException.class)
    public ProblemDetail handleInvalidGeometryException(InvalidGeometryException ex) {
        log.warn("Invalid geometry provided: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Geometry Parsing Error",
                ex.getMessage(), "invalid-geometry");
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ProblemDetail handleRouteNotFoundException(RouteNotFoundException ex) {
        log.warn("Route not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Route Not Found", ex.getMessage(), "route-not-found");
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ProblemDetail handleFolderNotFoundException(FolderNotFoundException ex) {
        log.warn("Folder not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Folder Not Found", ex.getMessage(), "folder-not-found");
    }

    @ExceptionHandler(AvoidanceZoneNotFoundException.class)
    public ProblemDetail handleAvoidanceZoneNotFoundException(AvoidanceZoneNotFoundException ex) {
        log.warn("Avoidance zone not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Avoidance Zone Not Found",
                ex.getMessage(), "avoidance-zone-not-found");
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "User already exists",
                ex.getMessage(), "user-already-exists");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "Access Denied",
                "Insufficient permissions for this resource.", "access-denied");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), "invalid-argument");
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ProblemDetail handleTokenRefreshException(RefreshTokenException ex) {
        log.warn("Token refresh failed: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Token Refresh Error", ex.getMessage(), "invalid-refresh-token");
    }

    @Override
    protected ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failure",
                "Invalid request parameters", "validation-failure");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty(ERRORS_PROPERTY, errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    protected ResponseEntity<@NonNull Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        log.warn("Unreadable request: {}", ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Unreadable request",
                "Invalid request body", "unreadable-request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }


    @ExceptionHandler(ExternalRoutingException.class)
    public ProblemDetail handleExternalRoutingException(ExternalRoutingException ex) {
        log.error("External routing API error: {}", ex.getMessage(), ex);
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "External Routing API Error",
                "An error occurred while communicating with the routing service.", "external-routing-error");
    }

    @ExceptionHandler(ExternalGeocodingException.class)
    public ProblemDetail handleExternalGeocodingException(ExternalGeocodingException ex) {
        log.error("External geocoding API error: {}", ex.getMessage(), ex);
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "Geocoding API Error",
                "An error occurred while communicating with the geocoding service.", "external-geocoding-error");
    }

    @ExceptionHandler(GpxExportException.class)
    public ProblemDetail handleGpxExportException(GpxExportException ex) {
        log.error("Error occurred while exporting GPX file: {}", ex.getMessage(), ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "GPX Export Error",
                "Failed to export GPX file.", "gpx-export-error");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllUncaughtExceptions(Exception ex) {
        log.error("An unexpected internal server error occurred: ", ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal server error occurred.", "internal-server-error");
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, String errorType) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        if (errorType != null && !errorType.isBlank()) {
            problemDetail.setType(URI.create("urn:problem-type:" + errorType));
        } else {
            problemDetail.setType(DEFAULT_TYPE);
        }

        return problemDetail;
    }
}