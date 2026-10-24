package fr.zbar.starter.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TransactionMetadataTest {

  @Test
  void should_document_enabled_with_default_true_and_description() throws Exception {
    try (InputStream metadata =
        new ClassPathResource("META-INF/spring-configuration-metadata.json").getInputStream()) {
      List<Map<String, Object>> found =
          JsonPath.read(metadata, "$.properties[?(@.name == 'zbar.transaction.enabled')]");

      assertThat(found).hasSize(1);
      assertThat(found.get(0).get("defaultValue")).isEqualTo(true);
      assertThat((String) found.get(0).get("description")).isNotBlank();
    }
  }
}
