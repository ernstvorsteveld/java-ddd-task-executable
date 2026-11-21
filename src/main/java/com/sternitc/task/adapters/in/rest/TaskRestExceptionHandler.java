package com.sternitc.task.adapters.in.rest;

import com.sternitc.task.api.model.ErrorItem;
import com.sternitc.task.api.model.ErrorResponsePayload;
import com.sternitc.task.domain.model.InvalidLinkedEntityException;
import com.sternitc.task.domain.model.InvalidPageRequestException;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidProcessNameException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskStatusException;
import com.sternitc.task.domain.model.InvalidTaskTypeException;
import com.sternitc.task.domain.model.InvalidTaskVersionException;
import com.sternitc.task.domain.model.TaskNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public final class TaskRestExceptionHandler {

    private final Clock clock;

    public TaskRestExceptionHandler() {
        this(Clock.systemUTC());
    }

    public TaskRestExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponsePayload> handleNotFound(TaskNotFoundException ex) {
        return singleError(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskTypeException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidTaskType(InvalidTaskTypeException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_TASK_TYPE", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskVersionException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidTaskVersion(InvalidTaskVersionException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_TASK_VERSION", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskStatusException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidTaskStatus(InvalidTaskStatusException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_TASK_STATUS", ex.getMessage());
    }

    @ExceptionHandler(InvalidProcessNameException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidProcessName(InvalidProcessNameException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_PROCESS_NAME", ex.getMessage());
    }

    @ExceptionHandler(InvalidPlanningWindowException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidPlanningWindow(InvalidPlanningWindowException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_PLANNING_WINDOW", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskExecutorException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidTaskExecutor(InvalidTaskExecutorException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_TASK_EXECUTOR", ex.getMessage());
    }

    @ExceptionHandler(InvalidLinkedEntityException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidLinkedEntity(InvalidLinkedEntityException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_LINKED_ENTITY", ex.getMessage());
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ResponseEntity<ErrorResponsePayload> handleInvalidPageRequest(InvalidPageRequestException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponsePayload> handleValidationErrors(MethodArgumentNotValidException ex) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ErrorItem> items = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorItem(
                        "VALIDATION_ERROR",
                        now,
                        error.getField() + ": " + Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value")
                ))
                .toList();
        if (items.isEmpty()) {
            return singleError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponsePayload(items));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponsePayload> handleConstraintViolations(ConstraintViolationException ex) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ErrorItem> items = ex.getConstraintViolations().stream()
                .map(violation -> new ErrorItem(
                        "VALIDATION_ERROR",
                        now,
                        violation.getPropertyPath() + ": " + violation.getMessage()
                ))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponsePayload(items));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponsePayload> handleMalformedRequest(RuntimeException ex) {
        return singleError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponsePayload> handleUnexpectedError(Exception ex) {
        return singleError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex.getMessage());
    }

    private ResponseEntity<ErrorResponsePayload> singleError(HttpStatus status, String errorCode, String description) {
        var item = new ErrorItem(
                errorCode,
                OffsetDateTime.now(clock),
                Objects.requireNonNullElse(description, errorCode)
        );
        return ResponseEntity.status(status).body(new ErrorResponsePayload(List.of(item)));
    }
}
