package fr.zbar.starter.errors;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

/** Reads the trace identifier from the current span of the application {@link Tracer}. */
class TracerTraceIdResolver implements TraceIdResolver {

  private final ObjectProvider<Tracer> tracer;

  TracerTraceIdResolver(ObjectProvider<Tracer> tracer) {
    this.tracer = tracer;
  }

  @Override
  public @Nullable String currentTraceId() {
    Tracer available = tracer.getIfAvailable();
    Span span = available == null ? null : available.currentSpan();
    String traceId = span == null ? null : span.context().traceId();
    return traceId == null || traceId.isBlank() ? null : traceId;
  }
}
