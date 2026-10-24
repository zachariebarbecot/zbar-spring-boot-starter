package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;

/** Logs the start and the end (status, duration) of every observed incoming request. */
class RequestLoggingObservationHandler
    extends CallLoggingObservationHandler<ServerRequestObservationContext> {

  private final List<String> excludePaths;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  RequestLoggingObservationHandler(Tracer tracer, List<String> excludePaths) {
    super(tracer, "request");
    this.excludePaths = excludePaths;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ServerRequestObservationContext server
        && excludePaths.stream()
            .noneMatch(pattern -> pathMatcher.match(pattern, server.getCarrier().getRequestURI()));
  }

  @Override
  String method(ServerRequestObservationContext context) {
    return context.getCarrier().getMethod();
  }

  @Override
  String target(ServerRequestObservationContext context) {
    return context.getCarrier().getRequestURI();
  }

  @Override
  String outcome(ServerRequestObservationContext context) {
    return "status=" + context.getResponse().getStatus();
  }
}
