package com.bogusz.bicyclerouteplannerbackend.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
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

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledException() {
        log.warn("Access attempt on disabled user account");
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Account Disabled",
                "User account is disabled.", "account-disabled");
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

    @ExceptionHandler(AvoidanceZoneLimitExceededException.class)
    public ProblemDetail handleAvoidanceZoneLimitExceededException(AvoidanceZoneLimitExceededException ex) {
        log.warn("Avoidance zone limit exceeded: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Avoidance Zone Limit Exceeded",
                ex.getMessage(), "avoidance-zone-limit-exceeded");
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User registration conflict: email already exists");
        return createProblemDetail(HttpStatus.CONFLICT, "User already exists",
                ex.getMessage(), "user-already-exists");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Database integrity violation: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return createProblemDetail(HttpStatus.CONFLICT, "Data Conflict",
                "The requested operation could not be completed due to a data conflict.", "data-conflict");
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
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed for field(s): {}", errors.keySet());

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failure",
                "Invalid request parameters", "validation-failure");
        problemDetail.setProperty(ERRORS_PROPERTY, errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            int lastDotIndex = propertyPath.lastIndexOf('.');
            String field = (lastDotIndex >= 0) ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
            String key = !field.isBlank() ? field : "parameter";
            errors.put(key, violation.getMessage());
        });
        log.warn("Validation failed for constraint violation(s): {}", errors.keySet());

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failure",
                "Invalid request parameters", "validation-failure");
        problemDetail.setProperty(ERRORS_PROPERTY, errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    protected ResponseEntity<@NonNull Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        String name = ex.getPropertyName();
        String paramName = (name != null && !name.isBlank()) ? name : "parameter";
        Map<String, String> errors = Map.of(paramName, "Invalid format or type for parameter '" + paramName + "'");
        log.warn("Type mismatch for parameter: {}", paramName);

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failure",
                "Invalid request parameters", "validation-failure");
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
        log.warn("Malformed HTTP request body received: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Unreadable request",
                "Invalid request body", "unreadable-request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleResourceAccessException(ResourceAccessException ex) {
        log.error("External service communication error: {}", ex.getMessage(), ex);
        if (ex.getCause() instanceof SocketTimeoutException || ex.getCause() instanceof HttpTimeoutException) {
            return createProblemDetail(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout",
                    "External service timed out. Please try again later.", "gateway-timeout");
        }
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "Bad Gateway",
                "External service is temporarily unavailable. Please try again later.", "bad-gateway");
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