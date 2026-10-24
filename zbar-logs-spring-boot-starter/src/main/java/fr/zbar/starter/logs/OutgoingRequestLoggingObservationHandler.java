package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import org.springframework.http.client.observation.ClientRequestObservationContext;

/**
 * Logs the start and the end (status or failure, duration) of RestClient and RestTemplate calls.
 */
class OutgoingRequestLoggingObservationHandler
    extends CallLoggingObservationHandler<ClientRequestObservationContext> {

  OutgoingRequestLoggingObservationHandler(Tracer tracer) {
    super(tracer, "outgoing");
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ClientRequestObservationContext;
  }

  @Override
  String method(ClientRequestObservationContext context) {
    return context.getCarrier().getMethod().name();
  }

  @Override
  String target(ClientRequestObservationContext context) {
    return hostAndPath(context.getCarrier().getURI());
  }

  /** Host, port and path of the call: never the user info, query or fragment. */
  private static String hostAndPath(URI uri) {
    String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
    return uri.getHost() + port + uri.getRawPath();
  }

  @Override
  String outcome(ClientRequestObservationContext context) {
    if (context.getError() != null) {
      return "failed=" + context.getError().getClass().getSimpleName();
    }
    try {
      return "status=" + context.getResponse().getStatusCode().value();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
