package fr.zbar.starter.errors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ConditionalOnProperty(prefix = "zbar.errors", name = "enabled", matchIfMissing = true)
@AutoConfiguration(
    beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@EnableConfigurationProperties(ErrorsProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
public class ErrorsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
  ErrorsExceptionHandler errorsExceptionHandler() {
    return new ErrorsExceptionHandler();
  }
}
