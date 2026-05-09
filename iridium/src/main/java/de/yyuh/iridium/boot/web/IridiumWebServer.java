package de.yyuh.iridium.boot.web;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.sun.net.httpserver.HttpServer;

import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.result.Result;

@NullMarked
public final class IridiumWebServer {

  private final HttpServer httpServer;
  private final List<Route> routes = new ArrayList<>();

  public IridiumWebServer(
      final String host,
      final int port) {
    final var result = this.createWebServer(host, port);

    if (result.isErr()) {
      throw new IllegalStateException("Failed to create web server: " + result.err().get());
    }

    this.httpServer = result.ok().get();

    this.httpServer.createContext("/", exchange -> {
      final var path = exchange.getRequestURI().getPath();
      final var method = exchange.getRequestMethod();

      for (final var route : routes) {
        if (!route.httpMethod().equalsIgnoreCase(method)) {
          continue;
        }

        final var match = route.pattern().match(path);

        if (match.isPresent()) {
          final var ctx = new RequestContext(exchange);
          ctx.setPathVariables(match.get());

          final var invokeResult = invoke(route.controller(), route.handler(), ctx);

          if (!invokeResult.isErr()) {
            apply(ctx, invokeResult.ok().get());
          }

          return;
        }
      }

      // No matching route found
      final var ctx = new RequestContext(exchange);
      ctx.send(404, "Not Found");
    });
  }

  public void start() {
    httpServer.start();
  }

  public void addRoute(
      final String path,
      final String httpMethod,
      final Object controller,
      final Method handler) {
    routes.add(new Route(httpMethod, PathPattern.parse(path), controller, handler));
  }

  private record Route(
      String httpMethod,
      PathPattern pattern,
      Object controller,
      Method handler) {}

  private static Result<Response, String> invoke(
      final Object controller,
      final Method handler,
      final RequestContext ctx) {
    return Result.of(() -> {
      final var value = handler.invoke(controller, ctx);

      if (!(value instanceof final Response response)) {
        return null;
      }

      return response;
    }).mapErr(Exception::getMessage);
  }

  private static void apply(final RequestContext ctx, final Response response) {
    response.headers().forEach((name, values) -> values.forEach(value -> ctx.responseHeader(name, value)));

    final var result = ctx.send(response.status(), response.body());

    if (result.isErr()) {
      throw new RuntimeException("Failed to send response", result.unwrapErr());
    }
  }

  private Result<HttpServer, String> createWebServer(final String host, final int port) {
    return Result.of(() -> HttpServer.create(new InetSocketAddress(host, port), 0))
        .mapErr(Exception::getMessage);
  }
}
