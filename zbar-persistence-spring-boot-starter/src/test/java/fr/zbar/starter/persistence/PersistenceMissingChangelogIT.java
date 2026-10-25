package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PersistenceMissingChangelogIT {

  @Test
  void should_fail_startup_with_explicit_message_when_default_changelog_is_missing() {
    assertThatThrownBy(PersistenceTestApplication::runWithoutDefaultChangelog)
        .hasRootCauseMessage("classpath:db/changelog/db.changelog-master.yaml does not exist");
  }
}
