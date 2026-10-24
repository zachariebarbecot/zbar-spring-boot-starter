package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LogsMetadataTest {

  @Test
  void should_document_enabled_with_default_true_and_description() throws Exception {
    Map<String, Object> property = property("zbar.logs.enabled");

    assertThat(property.get("defaultValue")).isEqualTo(true);
    assertThat((String) property.get("description")).isNotBlank();
  }

  @Test
  void should_document_exclude_paths_with_description() throws Exception {
    Map<String, Object> property = property("zbar.logs.exclude-paths");

    assertThat((String) property.get("description")).isNotBlank();
  }

  @Test
  void should_document_environment_with_description() throws Exception {
    Map<String, Object> property = property("zbar.logs.environment");

    assertThat((String) property.get("description")).isNotBlank();
  }

  private Map<String, Object> property(String name) throws Exception {
    try (InputStream metadata =
        new ClassPathResource("META-INF/spring-configuration-metadata.json").getInputStream()) {
      List<Map<String, Object>> found =
          JsonPath.read(metadata, "$.properties[?(@.name == '" + name + "')]");
      assertThat(found).hasSize(1);
      return found.get(0);
    }
  }
}
