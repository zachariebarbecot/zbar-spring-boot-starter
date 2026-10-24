package fr.zbar.starter.errors;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class ErrorsExceptionHandlerTest {

  private final ErrorsExceptionHandler handler = new ErrorsExceptionHandler(() -> null);

  @Test
  void should_return_no_response_when_server_error_occurs_on_committed_response() {
    MockHttpServletResponse committed = new MockHttpServletResponse();
    committed.setCommitted(true);
    ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest(), committed);

    Object response =
        handler.handleExceptionInternal(
            new IllegalStateException("boom"),
            ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR),
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request);

    assertThat(response).isNull();
  }

  @Test
  void should_sort_errors_by_field_when_several_fields_are_invalid() throws NoSuchMethodException {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "signup");
    result.addError(new FieldError("signup", "zeta", "must not be blank"));
    result.addError(new FieldError("signup", "alpha", "must not be blank"));
    result.addError(new ObjectError("signup", "inconsistent"));
    MethodParameter parameter =
        new MethodParameter(Object.class.getDeclaredMethod("equals", Object.class), 0);
    ServletWebRequest request =
        new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(parameter, result),
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request);

    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat(problem.getProperties().get("errors"))
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .extracting("field")
        .containsExactly("alpha", "signup", "zeta");
  }

  @Test
  void should_use_object_name_as_field_when_bean_level_constraint_is_violated() {
    ValidationController.Range range = new ValidationController.Range(5, 1);
    Set<ConstraintViolation<ValidationController.Range>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(range);
    ServletWebRequest request =
        new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());

    ResponseEntity<Object> response =
        handler.handleUnexpected(new ConstraintViolationException(violations), request);

    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat(problem.getProperties().get("errors"))
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .extracting("field")
        .containsExactly("range");
  }
}
