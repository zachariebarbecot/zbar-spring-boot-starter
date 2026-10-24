package fr.zbar.starter.transaction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Transaction starter properties.
 *
 * @param enabled whether the transaction starter is enabled.
 */
@ConfigurationProperties(prefix = "zbar.transaction")
public record TransactionProperties(@DefaultValue("true") boolean enabled) {}
