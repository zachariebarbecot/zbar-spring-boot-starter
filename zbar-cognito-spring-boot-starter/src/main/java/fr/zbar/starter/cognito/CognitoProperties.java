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
    @DefaultValue List<String> allowedClientIds) {

  public CognitoProperties {
    if (allowedClientIds.isEmpty()) {
      throw new IllegalArgumentException("zbar.cognito.allowed-client-ids must not be empty");
    }
    if (isBlank(issuerUri) && (isBlank(region) || isBlank(userPoolId))) {
      throw new IllegalArgumentException(
          "zbar.cognito.issuer-uri, or both zbar.cognito.region and zbar.cognito.user-pool-id, "
              + "must be set");
    }
  }

  String issuer() {
    return isBlank(issuerUri)
        ? "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId
        : issuerUri;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
