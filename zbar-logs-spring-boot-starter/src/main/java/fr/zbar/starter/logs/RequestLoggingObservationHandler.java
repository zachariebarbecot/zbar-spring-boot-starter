package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;

/** Logs the start and the end (status, duration) of every observed incoming request. */
class RequestLoggingObservationHandler
    implements ObservationHandler<ServerRequestObservationContext> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RequestLoggingObservationHandler.class);
  private static final String START_NANOS = "zbar.logs.startNanos";

  private final Tracer tracer;
  private final List<String> excludePaths;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  RequestLoggingObservationHandler(Tracer tracer, List<String> excludePaths) {
    this.tracer = tracer;
    this.excludePaths = excludePaths;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ServerRequestObservationContext server
        && excludePaths.stream()
            .noneMatch(pattern -> pathMatcher.match(pattern, server.getCarrier().getRequestURI()));
  }

  @Override
  public void onStart(ServerRequestObservationContext context) {
    context.put(START_NANOS, System.nanoTime());
    String method = context.getCarrier().getMethod();
    String path = context.getCarrier().getRequestURI();
    inTrace(context, () -> LOGGER.info("request start {} {}", method, path));
  }

  @Override
  public void onStop(ServerRequestObservationContext context) {
    long durationMs = (System.nanoTime() - context.<Long>get(START_NANOS)) / 1_000_000;
    String method = context.getCarrier().getMethod();
    String path = context.getCarrier().getRequestURI();
    int status = context.getResponse().getStatus();
    inTrace(
        context,
        () ->
            LOGGER.info(
                "request end {} {} status={} duration={}ms", method, path, status, durationMs));
  }

  private void inTrace(Observation.Context context, Runnable log) {
    try (Tracer.SpanInScope ignored =
        tracer.withSpan(context.<TracingContext>get(TracingContext.class).getSpan())) {
      log.run();
    }
  }
}
