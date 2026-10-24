package fr.zbar.starter.test;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Deferred so the configuration is processed after test-declared beans
 * {@code @ConditionalOnMissingBean} must see, yet before the auto-configurations:
 * {@code @ServiceConnection} only sees container beans registered before them.
 */
class ZbarPostgresImportSelector implements DeferredImportSelector, Ordered {

  @Override
  public String[] selectImports(AnnotationMetadata importingClassMetadata) {
    return new String[] {ZbarPostgresConfiguration.class.getName()};
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
