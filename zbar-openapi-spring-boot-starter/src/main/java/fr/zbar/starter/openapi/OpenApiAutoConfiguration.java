package fr.zbar.starter.openapi;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.Constants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = ProjectInfoAutoConfiguration.class)
public class OpenApiAutoConfiguration {

  @Bean
  OpenApiCustomizer zbarOpenApiCustomizer(
      Environment environment, ObjectProvider<GitProperties> gitProperties) {
    String applicationName = environment.getProperty("spring.application.name");
    GitProperties git = gitProperties.getIfAvailable();
    String commit = git == null ? null : git.get("commit.id.abbrev");
    return openApi -> {
      Info info = openApi.getInfo();
      if (applicationName != null
          && (info.getTitle() == null || Constants.DEFAULT_TITLE.equals(info.getTitle()))) {
        info.setTitle(applicationName);
      }
      if (commit != null
          && (info.getVersion() == null || Constants.DEFAULT_VERSION.equals(info.getVersion()))) {
        info.setVersion(commit);
      }
    };
  }
}
