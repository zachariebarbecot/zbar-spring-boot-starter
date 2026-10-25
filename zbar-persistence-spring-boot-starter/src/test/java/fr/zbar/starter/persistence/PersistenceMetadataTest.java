package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

class PersistenceMetadataTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "zbar.persistence.enabled",
        "zbar.persistence.jpa.enabled",
        "zbar.persistence.liquibase.enabled"
      })
  void should_document_property_with_default_true_and_description(String name) throws Exception {
    try (InputStream metadata =
        new ClassPathResource("META-INF/spring-configuration-metadata.json").getInputStream()) {
      List<Map<String, Object>> found =
          JsonPath.read(metadata, "$.properties[?(@.name == '" + name + "')]");

      assertThat(found).hasSize(1);
      assertThat(found.get(0).get("defaultValue")).isEqualTo(true);
      assertThat((String) found.get(0).get("description")).isNotBlank();
    }
  }
}
