package fr.zbar.starter.errors;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns Spring MVC exceptions into RFC 9457 problem responses. */
@RestControllerAdvice
class ErrorsExceptionHandler extends ResponseEntityExceptionHandler {}
