package de.yyuh.iridium.boot.web;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.sun.net.httpserver.HttpServer;

import de.yyuh.iridium.boot.middleware.Middleware;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.iridium.shared.result.Result;

/**
 * An embedded HTTP server backed by {@code com.sun.net.httpserver.HttpServer}.
 *
 * <p>
 * Manages the registered routes and the middleware chain. All requests
 * hit the root context, where the middleware chain runs before matching
 * a registered route.
 * </p>
 */
@NullMarked
public final class IridiumWebServer {

  private static final Log log = Log.of(IridiumWebServer.class);

  private final HttpServer httpServer;

  private List<Middleware> middlewares = List.of();
  private final List<Route> routes = new ArrayList<>();

  /**
   * Constructs and configures the server, including the root
   * request handler.
   *
   * @param host the bind address
   * @param port the listen port
   * @throws IllegalStateException if the server cannot be created
   */
  public IridiumWebServer(
      final String host,
      final int port) {
    final var result = this.createWebServer(host, port);

    if (result.isErr()) {
      throw new IllegalStateException("Failed to create web server: " + result.err().get());
    }

    this.httpServer = result.ok().get();

    log.info("Web server bound to %s:%d", host, port);

    this.httpServer.createContext("/", exchange -> {
      final var ctx = new RequestContext(exchange);

      final var routeHandler = buildRouteHandler();
      final var chain = buildMiddlewareChain(routeHandler);

      Result.of(() -> {
        final var response = chain.handle(ctx);
        apply(ctx, response);
        return null;
      }).ifErr(e -> {
        ctx.send(500, "Internal Server Error");

        log.error("Internal Server Error", e);
        e.printStackTrace();
      });
    });
  }

  /**
   * Sets the ordered list of middleware components.
   *
   * @param middlewares the middleware chain
   */
  public void setMiddlewares(final List<Middleware> middlewares) {
    this.middlewares = Collections.unmodifiableList(new ArrayList<>(middlewares));
  }

  /**
   * Starts the server, beginning to accept incoming connections.
   */
  public void start() {
    httpServer.start();
    log.info("HTTP server started");
  }

  /**
   * Registers a new route.
   *
   * @param path       the route path pattern
   * @param httpMethod the HTTP method (e.g. {@code GET})
   * @param controller the controller instance
   * @param handler    the handler method to invoke
   */
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
      Method handler) {
  }

  private Middleware.Next buildMiddlewareChain(final Middleware.Next finalHandler) {
    Middleware.Next chain = finalHandler;

    for (int i = middlewares.size() - 1; i >= 0; i--) {
      final var middleware = middlewares.get(i);
      final var next = chain;
      chain = ctx -> middleware.handle(ctx, next);
    }

    return chain;
  }

  private Middleware.Next buildRouteHandler() {
    return ctx -> {
      for (final var route : routes) {
        if (!route.httpMethod().equalsIgnoreCase(ctx.method())) {
          continue;
        }

        final var match = route.pattern().match(ctx.path());

        if (match.isPresent()) {
          ctx.setPathVariables(match.get());

          final var invokeResult = invoke(route.controller(), route.handler(), ctx);

          if (!invokeResult.isErr()) {
            return invokeResult.ok().get();
          }
        }
      }

      return Response.notFound("Not Found");
    };
  }

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
    }).mapErr(e -> e.getMessage() != null ? e.getMessage() : e.getClass().getName());
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
