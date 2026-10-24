package fr.zbar.starter.errors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Errors starter properties.
 *
 * @param enabled whether the errors starter is enabled.
 */
@ConfigurationProperties(prefix = "zbar.errors")
public record ErrorsProperties(@DefaultValue("true") boolean enabled) {}
