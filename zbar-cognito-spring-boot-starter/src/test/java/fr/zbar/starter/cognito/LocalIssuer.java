package fr.zbar.starter.cognito;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.UnaryOperator;

/** Fake Cognito user pool: signs access tokens and serves the matching JWKS locally. */
final class LocalIssuer implements AutoCloseable {

  static final String CLIENT_ID = "client-1";

  private final RSAKey key;
  private final HttpServer server;
  private final String issuer;

  LocalIssuer() throws IOException, JOSEException {
    key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    byte[] jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/pool/.well-known/jwks.json",
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, jwks.length);
          exchange.getResponseBody().write(jwks);
          exchange.close();
        });
    server.start();
    issuer = "http://localhost:" + server.getAddress().getPort() + "/pool";
  }

  String issuer() {
    return issuer;
  }

  /** A valid access token. */
  String accessToken() {
    return token(claims -> claims);
  }

  /** A valid access token, then adjusted by {@code customizer}. */
  String token(UnaryOperator<JWTClaimsSet.Builder> customizer) {
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("user-1")
            .claim("token_use", "access")
            .claim("client_id", CLIENT_ID)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
              customizer.apply(claims).build());
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
