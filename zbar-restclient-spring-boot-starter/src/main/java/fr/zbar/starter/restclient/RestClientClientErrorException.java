package fr.zbar.starter.restclient;

import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Outgoing call answered with a client error status. */
public class RestClientClientErrorException extends RestClientStatusException {

  RestClientClientErrorException(
      HttpStatusCode statusCode, HttpMethod method, URI url, ProblemDetail problemDetail) {
    super(statusCode, method, url, problemDetail);
  }
}
