package fr.zbar.starter.errors;

import org.jspecify.annotations.Nullable;

/** One entry of the {@code errors} property of a problem response. */
record Violation(String field, @Nullable String message) {}
