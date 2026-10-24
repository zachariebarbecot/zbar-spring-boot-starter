package fr.zbar.starter.restclient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Copies whitelisted headers of the incoming servlet request onto the outgoing request. */
class IncomingHeadersInterceptor implements ClientHttpRequestInterceptor {

  private static final List<String> DEFAULT_HEADERS = List.of("X-Correlation-Id", "X-Request-Id");
  private static final Set<String> FORBIDDEN_HEADERS =
      Set.of("authorization", "cookie", "proxy-authorization");

  private final List<String> headerNames;

  IncomingHeadersInterceptor(List<String> configuredHeaders) {
    this.headerNames =
        Stream.concat(DEFAULT_HEADERS.stream(), configuredHeaders.stream())
            .filter(name -> !FORBIDDEN_HEADERS.contains(name.toLowerCase(Locale.ROOT)))
            .toList();
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes incoming) {
      for (String name : headerNames) {
        List<String> values = Collections.list(incoming.getRequest().getHeaders(name));
        if (!values.isEmpty() && !request.getHeaders().containsHeader(name)) {
          request.getHeaders().put(name, values);
        }
      }
    }
    return execution.execute(request, body);
  }
}
