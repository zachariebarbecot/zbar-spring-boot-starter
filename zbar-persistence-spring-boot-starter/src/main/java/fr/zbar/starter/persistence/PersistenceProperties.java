package fr.zbar.starter.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Persistence starter properties.
 *
 * @param enabled whether the persistence starter is enabled.
 * @param jpa JPA defaults settings.
 * @param liquibase Liquibase defaults settings.
 */
@ConfigurationProperties(prefix = "zbar.persistence")
public record PersistenceProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue Jpa jpa,
    @DefaultValue Liquibase liquibase) {

  /**
   * JPA defaults settings.
   *
   * @param enabled whether the JPA defaults are applied.
   */
  public record Jpa(@DefaultValue("true") boolean enabled) {}

  /**
   * Liquibase defaults settings.
   *
   * @param enabled whether the Liquibase defaults are applied.
   */
  public record Liquibase(@DefaultValue("true") boolean enabled) {}
}
