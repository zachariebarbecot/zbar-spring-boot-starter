package fr.zbar.starter.errors;

import org.jspecify.annotations.Nullable;

/** Finds the trace identifier of the current request, if any. */
@FunctionalInterface
interface TraceIdResolver {

  @Nullable String currentTraceId();
}
