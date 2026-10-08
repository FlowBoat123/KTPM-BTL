package com.dental.service;

import com.dental.dto.*;
import com.dental.entity.*;
import com.dental.exception.ApiException;
import com.dental.repository.*;
import com.dental.security.*;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final AccountRepository accounts;
  private final LoginSessionRepository sessions;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final Clock clock;
  private final long ttl;
  private final String dummyHash;

  public AuthService(
      AccountRepository accounts,
      LoginSessionRepository sessions,
      PasswordEncoder passwords,
      JwtService jwt,
      Clock clock,
      @Value("${app.jwt-ttl-minutes}") long ttl) {
    this.accounts = accounts;
    this.sessions = sessions;
    this.passwords = passwords;
    this.jwt = jwt;
    this.clock = clock;
    this.ttl = ttl;
    dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public Responses.Login login(Requests.Login request) {
    if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.invalid("Password must not exceed 72 UTF-8 bytes");
    Account account = accounts.findByUsername(request.username()).orElse(null);
    boolean matched =
        passwords.matches(
            request.password(), account == null ? dummyHash : account.getPasswordHash());
    if (account == null || !matched || !account.getActive())
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    Instant now = clock.instant();
    Instant expiry = now.plusSeconds(ttl * 60);
    String id = UUID.randomUUID().toString();
    sessions.save(new LoginSession(id, account, expiry));
    return new Responses.Login(
        jwt.issue(account, id, now, expiry), "Bearer", expiry, ViewMapper.user(account));
  }

  @Transactional(readOnly = true)
  public CurrentUser authenticate(String token) {
    var claims = jwt.decode(token);
    LoginSession session =
        sessions.findById(claims.getId()).orElseThrow(() -> new JwtException("Revoked token"));
    Account account = session.getAccount();
    if (!session.getExpiresAt().isAfter(clock.instant())
        || !account.getActive()
        || !account.getId().toString().equals(claims.getSubject()))
      throw new JwtException("Inactive session");
    return new CurrentUser(
        account.getId(), account.getUsername(), account.getRole(), session.getId());
  }

  @Transactional
  public void logout(CurrentUser user) {
    sessions.deleteById(user.sessionId());
  }

  public Responses.User me(CurrentUser user) {
    return new Responses.User(user.accountId(), user.username(), user.role());
  }
}
