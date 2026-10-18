package com.laundryhub.backend.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.laundryhub.backend.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem-details body
 * ({@code {"status":400,"title":"Bad Request","detail":"Minimum of 8 kg per order", ...}}).
 * Domain rules throw {@link IllegalArgumentException} (400) and {@link IllegalStateException} (409);
 * services throw {@link ApiException} subclasses with their own status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Extra problem property holding field → message for validation errors. */
    public static final String ERRORS_PROPERTY = "errors";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param e       a service error
     * @param request the request
     * @return the problem with the exception's own status
     */
    // OOP: Polymorphism — getStatus() comes from the ApiException subclass.
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException e, HttpServletRequest request) {
        return problem(e.getStatus(), e.getMessage(), request);
    }

    /**
     * Broken domain rule or bad argument, e.g. "Minimum of 8 kg per order".
     *
     * @param e       the error
     * @param request the request
     * @return 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadArgument(IllegalArgumentException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /**
     * Action not allowed in the current state, e.g. changing a completed order.
     *
     * @param e       the error
     * @param request the request
     * @return 409
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleBadState(IllegalStateException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    /**
     * Two people changed the same row at once (optimistic or pessimistic lock).
     *
     * @param e       the error
     * @param request the request
     * @return 409
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ProblemDetail handleLocking(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Someone else changed this at the same time. Please refresh and try again.",
                request);
    }

    /**
     * A database constraint failed, e.g. a duplicate value.
     *
     * @param e       the error
     * @param request the request
     * @return 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        LOG.info("Constraint violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "This conflicts with existing data.", request);
    }

    /**
     * Invalid query or path parameter values.
     *
     * @param e       the error
     * @param request the request
     * @return 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Some values are not valid.", request);
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        problem.setProperty(ERRORS_PROPERTY, errors);
        return problem;
    }

    /**
     * A parameter of the wrong type, e.g. {@code ?status=WET}.
     *
     * @param e       the error
     * @param request the request
     * @return 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "'" + e.getName() + "' has an invalid value.", request);
    }

    /**
     * Role check failed inside a controller ({@code @PreAuthorize}).
     *
     * @param e       the error
     * @param request the request
     * @return 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "You do not have access to this.", request);
    }

    /**
     * Authentication failed inside a controller.
     *
     * @param e       the error
     * @param request the request
     * @return 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Please sign in.", request);
    }

    /**
     * Anything unexpected. Logged with its stack trace; the client only sees a generic message.
     *
     * @param e       the error
     * @param request the request
     * @return 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
        LOG.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request);
    }

    /** Bean Validation errors on a request body: one message per field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are not valid.");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        e.getBindingResult().getGlobalErrors().forEach(error -> errors.putIfAbsent(error.getObjectName(),
                error.getDefaultMessage()));
        problem.setProperty(ERRORS_PROPERTY, errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }
}
