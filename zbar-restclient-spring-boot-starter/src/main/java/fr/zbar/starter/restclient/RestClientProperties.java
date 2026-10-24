package fr.zbar.starter.restclient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Rest client starter properties.
 *
 * @param enabled whether the rest client starter is enabled.
 * @param baseUrl optional base URL applied to relative request URIs.
 */
@ConfigurationProperties(prefix = "zbar.restclient")
public record RestClientProperties(@DefaultValue("true") boolean enabled, String baseUrl) {}
