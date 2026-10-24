package fr.zbar.starter.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

class ErrorsExceptionHandlerTest {

  private final ErrorsExceptionHandler handler = new ErrorsExceptionHandler();

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
}
