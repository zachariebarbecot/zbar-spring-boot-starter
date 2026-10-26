package fr.zbar.starter.errors;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns Spring MVC exceptions into RFC 9457 problem responses. */
@RestControllerAdvice
class ErrorsExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorsExceptionHandler.class);
  private static final String GENERIC_DETAIL = "An unexpected error occurred.";
  private static final ClassLoader CLASS_LOADER = ErrorsExceptionHandler.class.getClassLoader();
  private static final boolean VALIDATION_PRESENT =
      ClassUtils.isPresent("jakarta.validation.ConstraintViolationException", CLASS_LOADER);

  // AuthorizationDeniedException extends AccessDeniedException.
  private static final List<Class<?>> SECURITY_EXCEPTIONS =
      Stream.of(
              "org.springframework.security.access.AccessDeniedException",
              "org.springframework.security.core.AuthenticationException")
          .filter(name -> ClassUtils.isPresent(name, CLASS_LOADER))
          .<Class<?>>map(name -> ClassUtils.resolveClassName(name, CLASS_LOADER))
          .toList();

  private final TraceIdResolver traceIds;

  ErrorsExceptionHandler(TraceIdResolver traceIds) {
    this.traceIds = traceIds;
  }

  @ExceptionHandler(Exception.class)
  @Nullable ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request)
      throws Exception {
    if (AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class) != null
        || isSecurityException(ex)) {
      throw ex;
    }
    List<Violation> violations = VALIDATION_PRESENT ? ConstraintViolationErrors.of(ex) : null;
    if (violations != null) {
      return withErrors(ex, violations, HttpStatus.BAD_REQUEST, new HttpHeaders(), request);
    }
    ProblemDetail body = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    return handleExceptionInternal(
        ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  private static boolean isSecurityException(Exception ex) {
    return SECURITY_EXCEPTIONS.stream().anyMatch(type -> type.isInstance(ex));
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Violation> violations =
        ex.getBindingResult().getAllErrors().stream()
            .map(error -> new Violation(fieldOf(error), error.getDefaultMessage()))
            .toList();
    return withErrors(ex, violations, status, headers, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Violation> violations =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new Violation(
                                    parameterName(result.getMethodParameter()),
                                    error.getDefaultMessage())))
            .toList();
    return withErrors(ex, violations, status, headers, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    List<Violation> violations = List.of(new Violation(propertyName(ex), "Invalid value."));
    return withErrors(ex, violations, status, headers, request);
  }

  private static String parameterName(MethodParameter parameter) {
    String name = parameter.getParameterName();
    return name != null ? name : "arg" + parameter.getParameterIndex();
  }

  private static String propertyName(TypeMismatchException ex) {
    String name = ex.getPropertyName();
    return name != null ? name : "parameter";
  }

  private static String fieldOf(ObjectError error) {
    return error instanceof FieldError field ? field.getField() : error.getObjectName();
  }

  private @Nullable ResponseEntity<Object> withErrors(
      Exception ex,
      List<Violation> violations,
      HttpStatusCode status,
      HttpHeaders headers,
      WebRequest request) {
    ProblemDetail body =
        createProblemDetail(ex, status, "Invalid request content.", null, null, request);
    body.setProperty("errors", violations.stream().sorted(comparing(Violation::field)).toList());
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    @Nullable ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    boolean serverError = statusCode.is5xxServerError();
    if (serverError) {
      LOGGER.error("Server error", ex);
    }
    if (response != null && response.getBody() instanceof ProblemDetail problem) {
      if (serverError) {
        problem.setDetail(GENERIC_DETAIL);
      }
      String traceId = traceIds.currentTraceId();
      if (traceId != null) {
        problem.setProperty("traceId", traceId);
      }
    }
    return response;
  }
}
