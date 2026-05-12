package de.yyuh.iridium.auth.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.NullMarked;

import de.yyuh.libs.core.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@NullMarked
public final class JwtService {

  private static final long ACCESS_TOKEN_MS = 15 * 60 * 1000;
  private static final long REFRESH_TOKEN_MS = 7 * 24 * 60 * 60 * 1000;
  private static final String ISSUER = "iridium-auth";

  private final SecretKey secretKey;

  public JwtService(final String secret) {
    this.secretKey = deriveKey(secret);
  }

  public static JwtService fromEnv() {
    final var secret = System.getenv().getOrDefault(
        "JWT_SECRET",
        "iridium-default-jwt-secret-change-in-production");
    return new JwtService(secret);
  }

  public String generateAccessToken(final UUID userId) {
    final var now = System.currentTimeMillis();

    return Jwts.builder()
        .issuer(ISSUER)
        .subject(userId.toString())
        .issuedAt(new Date(now))
        .expiration(new Date(now + ACCESS_TOKEN_MS))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken(final UUID userId) {
    final var now = System.currentTimeMillis();

    return Jwts.builder()
        .issuer(ISSUER)
        .subject(userId.toString())
        .issuedAt(new Date(now))
        .expiration(new Date(now + REFRESH_TOKEN_MS))
        .claim("type", "refresh")
        .signWith(secretKey)
        .compact();
  }

  public Result<Claims, String> validateToken(final String token) {
    return Result.of(() -> Jwts.parser()
        .verifyWith(secretKey)
        .requireIssuer(ISSUER)
        .build()
        .parseSignedClaims(token)
        .getPayload())
        .mapErr(Throwable::getMessage);
  }

  public Result<UUID, String> extractUserId(final String token) {
    return validateToken(token)
        .map(Claims::getSubject)
        .map(UUID::fromString);
  }

  public String hashToken(final String token) {
    return Result.of(() -> {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var hash = digest.digest(token.getBytes());
      return Base64.getEncoder().encodeToString(hash);
    }).unwrapOr(token);
  }

  private static SecretKey deriveKey(final String secret) {
    return Result.of(() -> {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var keyBytes = digest.digest(secret.getBytes());
      return new SecretKeySpec(keyBytes, "HmacSHA256");
    }).unwrap();
  }
}
