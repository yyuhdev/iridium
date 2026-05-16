package de.yyuh.iridium.auth.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.Celery;
import de.yyuh.iridium.auth.request.ForgotPasswordRequest;
import de.yyuh.iridium.auth.request.LoginRequest;
import de.yyuh.iridium.auth.request.RefreshRequest;
import de.yyuh.iridium.auth.request.RegisterRequest;
import de.yyuh.iridium.auth.request.ResetPasswordRequest;
import de.yyuh.iridium.auth.request.VerifyEmailRequest;
import de.yyuh.iridium.auth.service.AuthService;
import de.yyuh.iridium.auth.service.JwtService;
import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.DELETE;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.request.type.POST;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.libs.core.injection.Inject;
import de.yyuh.libs.core.result.Result;

@NullMarked
@RestController
public final class AuthController {

  private static final Log LOG = Log.of(AuthController.class);

  @Inject
  private Celery celery;

  private final JwtService jwt;
  private volatile AuthService auth;

  public AuthController() {
    this.jwt = JwtService.fromEnv();
  }

  private AuthService auth() {
    if (auth == null) {
      auth = new AuthService(celery, jwt);
    }
    return auth;
  }

  @POST("/auth/login")
  public Response login(final RequestContext ctx) {
    final var result = ctx.body(LoginRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    final var loginResult = auth().login(result.ok().get());
    if (loginResult.isErr()) {
      return Response.status(401, loginResult.unwrapErr());
    }

    return Response.json(loginResult.unwrap());
  }

  @POST("/auth/register")
  public Response register(final RequestContext ctx) {
    final var result = ctx.body(RegisterRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    final var registerResult = auth().register(result.ok().get());
    if (registerResult.isErr()) {
      return Response.status(409, registerResult.unwrapErr());
    }

    return Response.json(201, registerResult.unwrap());
  }

  @POST("/auth/logout")
  public Response logout(final RequestContext ctx) {
    final var userIdResult = extractUserId(ctx);
    if (userIdResult.isErr()) {
      return Response.status(401, userIdResult.unwrapErr());
    }

    final var body = ctx.body(RefreshRequest.class);
    if (body.isErr()) {
      return Response.badRequest(body.err().get());
    }

    final var logoutResult = auth().logout(userIdResult.unwrap(), body.ok().get().refreshToken());
    if (logoutResult.isErr()) {
      return Response.status(401, logoutResult.unwrapErr());
    }

    return Response.ok();
  }

  @POST("/auth/refresh")
  public Response refresh(final RequestContext ctx) {
    final var result = ctx.body(RefreshRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    final var refreshResult = auth().refresh(result.ok().get().refreshToken());
    if (refreshResult.isErr()) {
      return Response.status(401, refreshResult.unwrapErr());
    }

    return Response.json(refreshResult.unwrap());
  }

  @POST("/auth/forgot-password")
  public Response forgotPassword(final RequestContext ctx) {
    final var result = ctx.body(ForgotPasswordRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    LOG.info("Password reset requested for: %s", result.ok().get().email());
    return Response.ok();
  }

  @POST("/auth/reset-password")
  public Response resetPassword(final RequestContext ctx) {
    final var result = ctx.body(ResetPasswordRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    final var req = result.ok().get();
    final var resetResult = auth().resetPassword(req.token(), req.newPassword());
    if (resetResult.isErr()) {
      return Response.status(400, resetResult.unwrapErr());
    }

    LOG.info("Password reset succeeded for token: %s...", req.token().substring(0, Math.min(20, req.token().length())));
    return Response.ok();
  }

  @POST("/auth/verify-email")
  public Response verifyEmail(final RequestContext ctx) {
    final var result = ctx.body(VerifyEmailRequest.class);
    if (result.isErr()) {
      return Response.badRequest(result.err().get());
    }

    LOG.info("Email verification with token: %s", result.ok().get().token());
    return Response.ok();
  }

  @GET("/auth/me")
  public Response me(final RequestContext ctx) {
    final var userIdResult = extractUserId(ctx);
    if (userIdResult.isErr()) {
      return Response.status(401, userIdResult.unwrapErr());
    }

    final var userResult = auth().me(userIdResult.unwrap());
    if (userResult.isErr()) {
      return Response.status(404, userResult.unwrapErr());
    }

    return Response.json(userResult.unwrap());
  }

  @DELETE("/auth/account")
  public Response deleteAccount(final RequestContext ctx) {
    final var userIdResult = extractUserId(ctx);
    if (userIdResult.isErr()) {
      return Response.status(401, userIdResult.unwrapErr());
    }

    final var deleteResult = auth().deleteAccount(userIdResult.unwrap());
    if (deleteResult.isErr()) {
      return Response.status(500, deleteResult.unwrapErr());
    }

    return Response.noContent();
  }

  private Result<UUID, String> extractUserId(final RequestContext ctx) {
    final var header = ctx.requestHeader("Authorization");
    if (header.isEmpty()) {
      return Result.err("Missing Authorization header");
    }

    final var value = header.get();
    if (!value.startsWith("Bearer ")) {
      return Result.err("Invalid Authorization header");
    }

    return jwt.extractUserId(value.substring(7));
  }
}
