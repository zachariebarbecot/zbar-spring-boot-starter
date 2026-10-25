package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PersistenceReadmeTest {

  private final String readme = readme();

  @Test
  void should_document_diff_workflow() {
    assertThat(readme)
        .contains("mvn -Pliquibase-diff")
        .contains("zbar.liquibase.entity-package")
        .contains("zbar.liquibase.dialect")
        .containsIgnoringCase("relire le YAML")
        .containsIgnoringCase("db.changelog-master.yaml");
  }

  @Test
  void should_document_reproducibility_checklist() {
    assertThat(readme)
        .containsIgnoringCase("deux diffs sur base vierge")
        .containsIgnoringCase("diff post-migration vide")
        .containsIgnoringCase("renommage");
  }

  @Test
  void should_document_known_sequence_default_false_positive() {
    assertThat(readme)
        .containsIgnoringCase("faux positif")
        .contains("addDefaultValue")
        .contains("nextval");
  }

  private static String readme() {
    try {
      return Files.readString(Path.of("README.md"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
