package fr.zbar.starter.openapi;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class OpenApiAutoConfiguration {

  private static final String SPRINGDOC_DEFAULT_TITLE = "OpenAPI definition";

  @Bean
  OpenApiCustomizer zbarOpenApiCustomizer(Environment environment) {
    String applicationName = environment.getProperty("spring.application.name");
    return openApi -> {
      if (applicationName == null) {
        return;
      }
      if (openApi.getInfo() == null) {
        openApi.setInfo(new Info());
      }
      String title = openApi.getInfo().getTitle();
      if (title == null || title.equals(SPRINGDOC_DEFAULT_TITLE)) {
        openApi.getInfo().setTitle(applicationName);
      }
    };
  }
}
