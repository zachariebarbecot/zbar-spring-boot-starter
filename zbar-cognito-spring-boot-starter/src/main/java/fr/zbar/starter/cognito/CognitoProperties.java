package fr.zbar.starter.cognito;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cognito starter properties.
 *
 * @param enabled whether the Cognito starter is enabled.
 * @param issuerUri issuer of the access tokens; derived from region and user pool when absent.
 * @param region AWS region of the user pool.
 * @param userPoolId identifier of the user pool.
 * @param allowedClientIds app client identifiers accepted in the {@code client_id} claim.
 */
@ConfigurationProperties(prefix = "zbar.cognito")
public record CognitoProperties(
    @DefaultValue("true") boolean enabled,
    String issuerUri,
    String region,
    String userPoolId,
    @DefaultValue List<String> allowedClientIds) {}
