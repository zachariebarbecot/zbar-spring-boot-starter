package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import java.io.IOException;
import java.io.UncheckedIOException;
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
    return OutgoingTargets.hostAndPath(context.getCarrier().getURI());
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
