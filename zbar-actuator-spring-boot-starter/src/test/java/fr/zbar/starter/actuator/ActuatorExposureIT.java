package fr.zbar.starter.actuator;

import static fr.zbar.starter.actuator.ActuatorTestApplication.get;
import static fr.zbar.starter.actuator.ActuatorTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class ActuatorExposureIT {

  @Test
  void should_expose_health_info_and_prometheus_by_default() throws Exception {
    try (ConfigurableApplicationContext context = run()) {
      HttpResponse<String> health = get(context, "/actuator/health");
      assertThat(health.statusCode()).isEqualTo(200);
      assertThat((String) JsonPath.read(health.body(), "$.status")).isEqualTo("UP");
      assertThat(get(context, "/actuator/info").statusCode()).isEqualTo(200);
      HttpResponse<String> prometheus = get(context, "/actuator/prometheus");
      assertThat(prometheus.statusCode()).isEqualTo(200);
      assertThat(prometheus.body()).contains("# HELP");
    }
  }

  @Test
  void should_not_expose_env_beans_and_metrics_by_default() throws Exception {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(get(context, "/actuator/env").statusCode()).isEqualTo(404);
      assertThat(get(context, "/actuator/beans").statusCode()).isEqualTo(404);
      assertThat(get(context, "/actuator/metrics").statusCode()).isEqualTo(404);
    }
  }

  @Test
  void should_let_application_exposure_win() throws Exception {
    try (ConfigurableApplicationContext context =
        run("--management.endpoints.web.exposure.include=health,info,prometheus,metrics")) {
      assertThat(get(context, "/actuator/metrics").statusCode()).isEqualTo(200);
    }
  }

  @Test
  void should_not_expose_info_and_prometheus_nor_set_defaults_when_disabled() throws Exception {
    try (ConfigurableApplicationContext context = run("--zbar.actuator.enabled=false")) {
      assertThat(get(context, "/actuator/prometheus").statusCode()).isEqualTo(404);
      assertThat(get(context, "/actuator/info").statusCode()).isEqualTo(404);
      assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
          .isNull();
    }
  }
}
