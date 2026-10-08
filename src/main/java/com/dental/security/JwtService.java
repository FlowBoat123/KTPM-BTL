package com.dental.security;

import com.dental.entity.Account;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;

  public JwtService(@Value("${app.jwt-secret:}") String secret, Clock clock) {
    byte[] bytes;
    if (secret.isBlank()) {
      bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
    } else {
      bytes = secret.getBytes(StandardCharsets.UTF_8);
      if (bytes.length < 32)
        throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }
    var key = new SecretKeySpec(bytes, "HmacSHA256");
    encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    var nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    var timestamps = new JwtTimestampValidator(Duration.ZERO);
    timestamps.setClock(clock);
    nimbus.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator("dental-clinic")));
    decoder = nimbus;
  }

  public String issue(Account account, String sessionId, Instant now, Instant expiry) {
    var claims =
        JwtClaimsSet.builder()
            .issuer("dental-clinic")
            .subject(account.getId().toString())
            .id(sessionId)
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("role", account.getRole().name())
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public Jwt decode(String token) {
    return decoder.decode(token);
  }
}
