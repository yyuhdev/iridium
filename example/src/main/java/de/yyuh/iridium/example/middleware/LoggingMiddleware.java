package de.yyuh.iridium.example.middleware;

import de.yyuh.iridium.boot.annotation.Order;
import de.yyuh.iridium.boot.middleware.Middleware;
import de.yyuh.iridium.boot.middleware.annotation.MiddlewareComponent;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;

/**
 * Example middleware that logs every request with its duration.
 *
 * <p>Runs at {@code @Order(1)} to execute early in the chain.
 * Logs the HTTP method, path, response status, and elapsed time.</p>
 */
@Order(1)
@MiddlewareComponent
public final class LoggingMiddleware implements Middleware {

  /** {@inheritDoc} */
  @Override
  public Response handle(final RequestContext ctx, final Next next) throws Exception {
    final var start = System.currentTimeMillis();

    System.out.println("\u2192 " + ctx.method() + " " + ctx.path());

    final var response = next.handle(ctx);

    final var elapsed = System.currentTimeMillis() - start;

    System.out.println(
        "\u2190 " + ctx.method() + " " + ctx.path() + " \u2192 " + response.status() + " (" + elapsed + "ms)");

    return response;
  }
}
