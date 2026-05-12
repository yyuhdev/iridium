package de.yyuh.iridium.auth.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.query.impl.IDQuery;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.iridium.auth.User;
import de.yyuh.iridium.auth.query.EmptyQuery;
import de.yyuh.iridium.auth.request.CreateUserRequest;
import de.yyuh.iridium.auth.request.UpdateUserEmailRequest;
import de.yyuh.iridium.auth.request.UpdateUserPasswordRequest;
import de.yyuh.iridium.auth.request.UpdateUserRequest;
import de.yyuh.iridium.auth.request.UpdateUserRoleRequest;
import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.DELETE;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.request.type.PATCH;
import de.yyuh.iridium.boot.request.type.POST;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.hashing.PasswordHasher;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.libs.core.injection.Inject;
import de.yyuh.libs.core.result.Result;

@NullMarked
@RestController
public final class UserController {

  private static final Log LOG = Log.of(UserController.class);

  @Inject
  private Celery celery;

  public UserController() {
  }

  @GET("/users")
  public Response getUsers(final RequestContext ctx) {
    final var platform = getPlatform();

    @SuppressWarnings("unchecked")
    final var query = (IQuery<IEntity>) (IQuery<?>) EmptyQuery.of(User.class);

    final var users = platform.defaultProvider().find(query).join();

    return Response.json(users);
  }

  @GET("/users/{id}")
  public Response getUserById(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    return Response.json(userResult.unwrap());
  }

  @POST("/users")
  public Response createUser(final RequestContext ctx) {
    final var result = ctx.body(CreateUserRequest.class);

    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    final var request = result.ok().get();

    final var existing = findUserByEmail(request.email());
    if (existing.isOk()) {
      return Response.status(409, "Email already registered");
    }

    final var user = User.fromRequest(request);

    final var platform = getPlatform();
    platform.defaultProvider().save(user).join();

    LOG.info("User created: %s (%s)", user.username(), user.id());
    return Response.json(201, user);
  }

  @PATCH("/users/{id}")
  public Response updateUser(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    final var bodyResult = ctx.body(UpdateUserRequest.class);
    if (bodyResult.isErr()) {
      return Response.badRequest(bodyResult.err().get());
    }

    final var existing = userResult.unwrap();
    final var request = bodyResult.ok().get();

    final var updated = new User(
        existing.id(),
        request.username().orElse(existing.username()),
        request.email().orElse(existing.email()),
        existing.passwordHash(),
        request.role().orElse(existing.role()));

    final var platform = getPlatform();
    platform.defaultProvider().save(updated).join();

    LOG.info("User updated: %s", updated.id());
    return Response.json(updated);
  }

  @PATCH("/users/{id}/password")
  public Response updateUserPassword(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    final var bodyResult = ctx.body(UpdateUserPasswordRequest.class);
    if (bodyResult.isErr()) {
      return Response.badRequest(bodyResult.err().get());
    }

    final var passwordHash = PasswordHasher.hashPassword(bodyResult.ok().get().password());

    if (passwordHash.isErr()) {
      return Response.status(500, "Failed to hash password");
    }

    final var existing = userResult.unwrap();
    final var updated = new User(
        existing.id(),
        existing.username(),
        existing.email(),
        passwordHash.ok().get(),
        existing.role());

    final var platform = getPlatform();
    platform.defaultProvider().save(updated).join();

    LOG.info("Password updated for user: %s", updated.id());
    return Response.ok();
  }

  @PATCH("/users/{id}/email")
  public Response updateUserEmail(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    final var bodyResult = ctx.body(UpdateUserEmailRequest.class);
    if (bodyResult.isErr()) {
      return Response.badRequest(bodyResult.err().get());
    }

    final var existing = userResult.unwrap();
    final var updated = new User(
        existing.id(),
        existing.username(),
        bodyResult.ok().get().email(),
        existing.passwordHash(),
        existing.role());

    final var platform = getPlatform();
    platform.defaultProvider().save(updated).join();

    LOG.info("Email updated for user: %s", updated.id());
    return Response.ok();
  }

  @PATCH("/users/{id}/role")
  public Response updateUserRole(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    final var bodyResult = ctx.body(UpdateUserRoleRequest.class);
    if (bodyResult.isErr()) {
      return Response.badRequest(bodyResult.err().get());
    }

    final var existing = userResult.unwrap();
    final var updated = new User(
        existing.id(),
        existing.username(),
        existing.email(),
        existing.passwordHash(),
        bodyResult.ok().get().role());

    final var platform = getPlatform();
    platform.defaultProvider().save(updated).join();

    LOG.info("Role updated for user %s: %s", updated.id(), updated.role());
    return Response.ok();
  }

  @DELETE("/users/{id}")
  public Response deleteUser(final RequestContext ctx) {
    final var idResult = parseUserId(ctx);
    if (idResult.isErr()) {
      return Response.badRequest(idResult.unwrapErr());
    }

    final var userResult = findUser(idResult.unwrap());
    if (userResult.isErr()) {
      return Response.notFound(userResult.unwrapErr());
    }

    final var platform = getPlatform();

    @SuppressWarnings("unchecked")
    final var query = (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(User.class, idResult.unwrap()).build();

    platform.defaultProvider().delete(query).join();

    LOG.info("User deleted: %s", idResult.unwrap());
    return Response.noContent();
  }

  private CeleryMongoDBPlatform getPlatform() {
    return celery.getPlatform(CeleryMongoDBPlatform.class).orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private Result<User, String> findUser(final UUID id) {
    final var platform = getPlatform();
    final var query = (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(User.class, id).build();

    return platform.defaultProvider().get(query)
        .join()
        .map(e -> (User) e)
        .<Result<User, String>>map(Result::ok)
        .orElseGet(() -> Result.err("User not found"));
  }

  @SuppressWarnings("unchecked")
  private Result<User, String> findUserByEmail(final String email) {
    final var platform = getPlatform();
    final var query = (IQuery<IEntity>) (IQuery<?>) EmptyQuery.of(User.class);

    return platform.defaultProvider().find(query)
        .join().stream()
        .map(e -> (User) e)
        .filter(u -> u.email().equalsIgnoreCase(email))
        .findFirst()
        .<Result<User, String>>map(Result::ok)
        .orElseGet(() -> Result.err("User not found"));
  }

  private static Result<UUID, String> parseUserId(final RequestContext ctx) {
    final var idStr = ctx.pathVariable("id");
    if (idStr.isEmpty()) {
      return Result.err("Missing user ID");
    }

    try {
      return Result.ok(UUID.fromString(idStr.get()));
    } catch (final IllegalArgumentException e) {
      return Result.err("Invalid user ID: " + idStr.get());
    }
  }
}
