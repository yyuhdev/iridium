package de.yyuh.iridium.boot.web;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import org.jspecify.annotations.NullMarked;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.shared.result.Result;

@NullMarked
public final class IridiumWebServer {

  private final HttpServer httpServer;

  public IridiumWebServer(
      final String host,
      final int port) {
    final var result = this.createWebServer(host, port);

    if (result.isErr()) {
      throw new IllegalStateException("Failed to start web server" + result.err().get());
    }

    this.httpServer = result.ok().get();
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

      Result.of(() -> handler.invoke(controller, ctx)).ifErr(exception -> {
        throw new IllegalStateException(exception.getMessage());
      });
    })).mapErr(Exception::getMessage);
  }

  private Result<HttpServer, String> createWebServer(final String host, final int port) {
    return Result.of(() -> HttpServer.create(new InetSocketAddress(host, port), 0))
        .mapErr(Exception::getMessage);
  }
}
