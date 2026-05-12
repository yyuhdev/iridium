package de.yyuh.iridium.auth.entity;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Repository;
import de.yyuh.celery.api.entity.IEntity;

@NullMarked
@Repository("refresh_tokens")
public record RefreshToken(
    @Identifier UUID id,
    UUID userId,
    String tokenHash,
    long expiresAt,
    long createdAt) implements IEntity {

  public boolean isExpired() {
    return System.currentTimeMillis() > expiresAt;
  }
}
