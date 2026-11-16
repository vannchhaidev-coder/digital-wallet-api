package com.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtValidator {

  private final JwtProperties properties;
  private final SecretKey verifyingKey;

  public JwtValidator(JwtProperties properties) {
    this.properties = properties;
    this.verifyingKey = Keys.hmacShaKeyFor(decodeSecret(properties.secret()));
  }

  private static byte[] decodeSecret(String secret) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException(
          "app.jwt.secret must be valid base64 and match auth-service's JWT_SECRET exactly", ex);
    }
    if (decoded.length < 32) {
      throw new IllegalStateException("app.jwt.secret must decode to at least 256 bits");
    }
    return decoded;
  }

  public Claims validate(String token) throws JwtException {
    return Jwts.parser()
        .verifyWith(verifyingKey)
        .requireIssuer(properties.issuer())
        .build()
        .parseEncryptedClaims(token)
        .getPayload();
  }
}
