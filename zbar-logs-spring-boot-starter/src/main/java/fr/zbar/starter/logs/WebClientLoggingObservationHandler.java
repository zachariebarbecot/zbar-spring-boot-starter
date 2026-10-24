package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;

/** Logs the start and the end (status or failure, duration) of WebClient calls. */
class WebClientLoggingObservationHandler
    extends CallLoggingObservationHandler<ClientRequestObservationContext> {

  WebClientLoggingObservationHandler(Tracer tracer) {
    super(tracer, "outgoing");
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ClientRequestObservationContext;
  }

  @Override
  String method(ClientRequestObservationContext context) {
    return context.getRequest().method().name();
  }

  @Override
  String target(ClientRequestObservationContext context) {
    return OutgoingTargets.hostAndPath(context.getRequest().url());
  }

  @Override
  String outcome(ClientRequestObservationContext context) {
    if (context.getError() != null) {
      return "failed=" + context.getError().getClass().getSimpleName();
    }
    return "status=" + context.getResponse().statusCode().value();
  }
}
