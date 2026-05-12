package de.yyuh.iridium.auth;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.iridium.auth.request.CreateUserRequest;
import de.yyuh.iridium.shared.hashing.PasswordHasher;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Repository;

@NullMarked
@Repository("users")
public record User(
    @Identifier UUID id,
    String username,
    String email,
    String passwordHash,
    String role) implements IEntity {

  public static User fromRequest(final CreateUserRequest request) {
    final var passwordHash = PasswordHasher.hashPassword(request.password());

    if (passwordHash.isErr()) {
      throw new IllegalStateException(passwordHash.err().get());
    }

    return new User(
        UUID.randomUUID(),
        request.name(),
        request.email(),
        passwordHash.ok().get(),
        "user");
  }
}
