package fr.zbar.starter.errors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns Spring MVC exceptions into RFC 9457 problem responses. */
@RestControllerAdvice
class ErrorsExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorsExceptionHandler.class);
  private static final String GENERIC_DETAIL = "An unexpected error occurred.";

  private final TraceIdResolver traceIds;

  ErrorsExceptionHandler(TraceIdResolver traceIds) {
    this.traceIds = traceIds;
  }

  @ExceptionHandler(Exception.class)
  @Nullable ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
    ProblemDetail body = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    return handleExceptionInternal(
        ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
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
