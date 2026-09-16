package com.bogusz.bicyclerouteplannerbackend.exception;


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


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
        problemDetail.setTitle("Authorization Error");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("User Not Found");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidGeometryException.class)
    public ProblemDetail handleInvalidGeometryException(InvalidGeometryException ex){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Geometry Parsing Error");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ProblemDetail handleRouteNotFoundException(RouteNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Route Not Found");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ProblemDetail handleFolderNotFoundException(FolderNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Folder Not Found");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(AvoidanceZoneNotFoundException.class)
    public ProblemDetail handleAvoidanceZoneNotFoundException(AvoidanceZoneNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Avoidance Zone Not Found");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }


    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("User already exists");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @Override
    protected ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters"
        );
        problemDetail.setTitle("Validation Failure");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());


        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty("validationErrors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    @NonNull
    protected ResponseEntity<@NonNull Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
        problemDetail.setTitle("Unreadable request");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Insufficient permissions for this resource."
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problemDetail.setTitle("Runtime Error");
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problemDetail;
    }
}
