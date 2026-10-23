package fr.zbar.starter.openapi;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.Constants;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class OpenApiAutoConfiguration {

  @Bean
  OpenApiCustomizer zbarOpenApiCustomizer(Environment environment) {
    String applicationName = environment.getProperty("spring.application.name");
    return openApi -> {
      if (applicationName == null) {
        return;
      }
      String title = openApi.getInfo().getTitle();
      if (title == null || title.equals(Constants.DEFAULT_TITLE)) {
        openApi.getInfo().setTitle(applicationName);
      }
    };
  }
}
