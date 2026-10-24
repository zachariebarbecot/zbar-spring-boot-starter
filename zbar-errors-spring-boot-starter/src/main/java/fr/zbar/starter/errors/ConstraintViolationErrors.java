package fr.zbar.starter.errors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.beans.Introspector;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Isolated so that the handler loads without Jakarta Validation on the classpath. */
final class ConstraintViolationErrors {

  private ConstraintViolationErrors() {}

  static @Nullable List<Violation> of(Exception ex) {
    if (!(ex instanceof ConstraintViolationException violations)) {
      return null;
    }
    return violations.getConstraintViolations().stream()
        .map(violation -> new Violation(parameter(violation), violation.getMessage()))
        .toList();
  }

  private static String parameter(ConstraintViolation<?> violation) {
    String name = null;
    for (Path.Node node : violation.getPropertyPath()) {
      name = node.getName();
    }
    return name != null
        ? name
        : Introspector.decapitalize(violation.getRootBeanClass().getSimpleName());
  }
}
