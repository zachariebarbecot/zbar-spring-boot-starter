package fr.zbar.starter.cognito;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Accepts only access tokens issued to an allowed app client; Cognito access tokens carry no {@code
 * aud}.
 */
class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

  private final List<String> allowedClientIds;

  CognitoAccessTokenValidator(List<String> allowedClientIds) {
    this.allowedClientIds = allowedClientIds;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (!"access".equals(jwt.getClaimAsString("token_use"))) {
      return failure("The token_use claim must be access");
    }
    if (!allowedClientIds.contains(jwt.getClaimAsString("client_id"))) {
      return failure("The client_id claim is not allowed");
    }
    return OAuth2TokenValidatorResult.success();
  }

  private static OAuth2TokenValidatorResult failure(String description) {
    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
  }
}
