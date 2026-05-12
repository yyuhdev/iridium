package de.yyuh.iridium.auth.service;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.query.impl.IDQuery;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.iridium.auth.User;
import de.yyuh.iridium.auth.entity.RefreshToken;
import de.yyuh.iridium.auth.query.EmptyQuery;
import de.yyuh.iridium.auth.request.CreateUserRequest;
import de.yyuh.iridium.auth.request.LoginRequest;
import de.yyuh.iridium.auth.request.RegisterRequest;
import de.yyuh.iridium.auth.response.TokenPair;
import de.yyuh.iridium.shared.hashing.PasswordHasher;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.libs.core.result.Result;

@NullMarked
public final class AuthService {

  private static final Log LOG = Log.of(AuthService.class);

  private final Celery celery;
  private final JwtService jwt;

  public AuthService(final Celery celery, final JwtService jwt) {
    this.celery = celery;
    this.jwt = jwt;
  }

  public Result<TokenPair, String> login(final LoginRequest request) {
    final var userResult = findUserByEmail(request.email());
    if (userResult.isErr()) {
      return Result.err(userResult.unwrapErr());
    }

    final var user = userResult.unwrap();

    final var passwordOk = PasswordHasher.verifyPassword(request.password(), user.passwordHash());
    if (passwordOk.isErr() || !passwordOk.unwrap()) {
      return Result.err("Invalid email or password");
    }

    final var tokens = issueTokens(user.id());

    LOG.info("User %s logged in", user.username());
    return Result.ok(tokens);
  }

  public Result<TokenPair, String> register(final RegisterRequest request) {
    final var platform = getPlatform();

    final var existing = findUserByEmail(request.email());
    if (existing.isOk()) {
      return Result.err("Email already registered");
    }

    final var createRequest = new CreateUserRequest(request.name(), request.email(), request.password());
    final var user = User.fromRequest(createRequest);

    platform.defaultProvider().save(user).join();

    final var tokens = issueTokens(user.id());

    LOG.info("User %s registered", user.username());
    return Result.ok(tokens);
  }

  public Result<TokenPair, String> refresh(final String rawRefreshToken) {
    final var platform = getPlatform();

    final var claimsResult = jwt.validateToken(rawRefreshToken);
    if (claimsResult.isErr()) {
      return Result.err("Invalid refresh token");
    }

    final var claims = claimsResult.unwrap();
    final var type = claims.get("type", String.class);

    if (!"refresh".equals(type)) {
      return Result.err("Token is not a refresh token");
    }

    final var userId = UUID.fromString(claims.getSubject());
    final var tokenHash = jwt.hashToken(rawRefreshToken);

    final var storedToken = findRefreshToken(userId, tokenHash);
    if (storedToken == null || storedToken.isExpired()) {
      return Result.err("Refresh token expired or revoked");
    }

    platform.defaultProvider().delete(tokenQuery(storedToken.id())).join();

    final var tokens = issueTokens(userId);

    LOG.info("Tokens refreshed for user %s", userId);
    return Result.ok(tokens);
  }

  public Result<Void, String> logout(final UUID userId, final String rawRefreshToken) {
    final var platform = getPlatform();
    final var tokenHash = jwt.hashToken(rawRefreshToken);

    final var storedToken = findRefreshToken(userId, tokenHash);
    if (storedToken != null) {
      platform.defaultProvider().delete(tokenQuery(storedToken.id())).join();
    }

    LOG.info("User %s logged out", userId);
    return Result.ok(null);
  }

  public Result<User, String> me(final UUID userId) {
    return findUserById(userId);
  }

  public Result<Void, String> deleteAccount(final UUID userId) {
    final var platform = getPlatform();
    platform.defaultProvider().delete(userQuery(userId)).join();

    LOG.info("Account deleted for user %s", userId);
    return Result.ok(null);
  }

  private TokenPair issueTokens(final UUID userId) {
    final var accessToken = jwt.generateAccessToken(userId);
    final var refreshToken = jwt.generateRefreshToken(userId);

    final var now = System.currentTimeMillis();
    final var tokenEntity = new RefreshToken(
        UUID.randomUUID(),
        userId,
        jwt.hashToken(refreshToken),
        now + 7 * 24 * 60 * 60 * 1000L,
        now);

    final var platform = getPlatform();
    platform.defaultProvider().save(tokenEntity).join();

    return new TokenPair(accessToken, refreshToken);
  }

  private CeleryMongoDBPlatform getPlatform() {
    return celery.getPlatform(CeleryMongoDBPlatform.class).orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private Result<User, String> findUserByEmail(final String email) {
    final var platform = getPlatform();
    final var query = (IQuery<IEntity>) (IQuery<?>) EmptyQuery.of(User.class);

    final var users = platform.defaultProvider().find(query).join();
    final var match = users.stream()
        .map(e -> (User) e)
        .filter(u -> u.email().equalsIgnoreCase(email))
        .findFirst();

    return match.<Result<User, String>>map(Result::ok)
        .orElseGet(() -> Result.err("User not found"));
  }

  @SuppressWarnings("unchecked")
  private Result<User, String> findUserById(final UUID userId) {
    final var platform = getPlatform();
    final var query = (IQuery<IEntity>) (IQuery<?>) userQuery(userId);

    return platform.defaultProvider().get(query)
        .join()
        .map(e -> (User) e)
        .<Result<User, String>>map(Result::ok)
        .orElseGet(() -> Result.err("User not found"));
  }

  @SuppressWarnings("unchecked")
  private RefreshToken findRefreshToken(final UUID userId, final String tokenHash) {
    final var platform = getPlatform();
    final var query = (IQuery<IEntity>) (IQuery<?>) EmptyQuery.of(RefreshToken.class);

    return platform.defaultProvider().find(query).join().stream()
        .map(e -> (RefreshToken) e)
        .filter(rt -> rt.userId().equals(userId) && rt.tokenHash().equals(tokenHash))
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private static IQuery<IEntity> userQuery(final UUID id) {
    return (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(User.class, id).build();
  }

  @SuppressWarnings("unchecked")
  private static IQuery<IEntity> tokenQuery(final UUID id) {
    return (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(RefreshToken.class, id).build();
  }
}
