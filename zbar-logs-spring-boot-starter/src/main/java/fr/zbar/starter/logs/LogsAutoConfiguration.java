package fr.zbar.starter.logs;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConditionalOnProperty(prefix = "zbar.logs", name = "enabled", matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(LogsProperties.class)
public class LogsAutoConfiguration {}
