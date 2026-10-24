package fr.zbar.starter.restclient;

import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** Failure of an outgoing call answered with an error status. */
public abstract class RestClientStatusException extends RestClientException {

  private final HttpStatusCode statusCode;
  private final HttpMethod method;
  private final URI url;
  private final ProblemDetail problemDetail;

  RestClientStatusException(
      HttpStatusCode statusCode, HttpMethod method, URI url, ProblemDetail problemDetail) {
    super(statusCode.value() + " on " + method + " " + withoutSensitiveParts(url));
    this.statusCode = statusCode;
    this.method = method;
    this.url = url;
    this.problemDetail = problemDetail;
  }

  public HttpStatusCode statusCode() {
    return statusCode;
  }

  public HttpMethod method() {
    return method;
  }

  /** Full URL: contains the query, do not log it as is. */
  public URI url() {
    return url;
  }

  public ProblemDetail problemDetail() {
    return problemDetail;
  }

  private static String withoutSensitiveParts(URI url) {
    return UriComponentsBuilder.fromUri(url)
        .userInfo(null)
        .replaceQuery(null)
        .fragment(null)
        .build()
        .toUriString();
  }
}
