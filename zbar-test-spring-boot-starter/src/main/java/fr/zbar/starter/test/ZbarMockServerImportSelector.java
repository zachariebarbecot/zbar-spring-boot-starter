package fr.zbar.starter.test;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Deferred so the configuration is processed after test-declared beans
 * {@code @ConditionalOnMissingBean} must see.
 */
class ZbarMockServerImportSelector implements DeferredImportSelector {

  @Override
  public String[] selectImports(AnnotationMetadata importingClassMetadata) {
    return new String[] {ZbarMockServerConfiguration.class.getName()};
  }
}
