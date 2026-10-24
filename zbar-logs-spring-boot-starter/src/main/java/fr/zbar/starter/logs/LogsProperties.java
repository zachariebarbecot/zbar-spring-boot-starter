package fr.zbar.starter.logs;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Logs starter properties.
 *
 * @param enabled whether the logs starter is enabled.
 * @param excludePaths ant patterns of request paths without start/end logs; replaces the default
 *     list when set.
 * @param environment deployed environment name written in JSON logs; defaults to the first active
 *     profile other than {@code json}.
 */
@ConfigurationProperties(prefix = "zbar.logs")
public record LogsProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue({"/actuator/**", "/v3/api-docs"}) List<String> excludePaths,
    String environment) {}
