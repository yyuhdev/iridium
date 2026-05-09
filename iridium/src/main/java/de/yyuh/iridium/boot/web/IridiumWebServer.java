package de.yyuh.iridium.boot.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import org.jspecify.annotations.NullMarked;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.result.Result;

@NullMarked
public final class IridiumWebServer {

  private final HttpServer httpServer;

  public IridiumWebServer(
      final String host,
      final int port) {
    final var result = this.createWebServer(host, port);

    if (result.isErr()) {
      throw new IllegalStateException("Failed to create web server: " + result.err().get());
    }

    this.httpServer = result.ok().get();
  }

  public void start() {
    httpServer.start();
  }

  public Result<HttpContext, String> createPath(
      final String path,
      final String httpMethod,
      final Object controller,
      final Method handler) {
    return Result.of(() -> this.httpServer.createContext(path, ex -> {
      if (!ex.getRequestMethod().equalsIgnoreCase(httpMethod)) {
        return;
      }

      final var ctx = new RequestContext(ex);
      final var result = invoke(controller, handler, ctx);

      if (result != null) {
        apply(ctx, result);
      }
    })).mapErr(Exception::getMessage);
  }

  private static Response invoke(
      final Object controller,
      final Method handler,
      final RequestContext ctx) {
    try {
      final var value = handler.invoke(controller, ctx);

      if (value instanceof final Response response) {
        return response;
      }

      return null;
    } catch (final IllegalAccessException e) {
      throw new RuntimeException("Cannot access handler: " + handler.getName(), e);
    } catch (final InvocationTargetException e) {
      throw new RuntimeException("Handler threw: " + handler.getName(), e.getCause());
    }
  }

  private static void apply(final RequestContext ctx, final Response response) {
    response.headers().forEach((name, values) ->
        values.forEach(value -> ctx.responseHeader(name, value)));

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
