package fr.zbar.starter.logs;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs the start and the end (outcome, duration) of an observed HTTP call. */
abstract class CallLoggingObservationHandler<C extends Observation.Context>
    implements ObservationHandler<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CallLoggingObservationHandler.class);
  private static final String START_NANOS = "zbar.logs.startNanos";

  private final Tracer tracer;
  private final String label;

  CallLoggingObservationHandler(Tracer tracer, String label) {
    this.tracer = tracer;
    this.label = label;
  }

  abstract String method(C context);

  abstract String target(C context);

  /** The end of the call: {@code status=200} or {@code failed=<ExceptionType>}. */
  abstract String outcome(C context);

  @Override
  public void onStart(C context) {
    context.put(START_NANOS, System.nanoTime());
    String method = method(context);
    String target = target(context);
    inTrace(context, () -> LOGGER.info("{} start {} {}", label, method, target));
  }

  @Override
  public void onStop(C context) {
    long durationMs = (System.nanoTime() - context.<Long>get(START_NANOS)) / 1_000_000;
    String method = method(context);
    String target = target(context);
    String outcome = outcome(context);
    inTrace(
        context,
        () ->
            LOGGER.info(
                "{} end {} {} {} duration={}ms", label, method, target, outcome, durationMs));
  }

  private void inTrace(Observation.Context context, Runnable log) {
    try (Tracer.SpanInScope ignored =
        tracer.withSpan(context.<TracingContext>get(TracingContext.class).getSpan())) {
      log.run();
    }
  }
}
