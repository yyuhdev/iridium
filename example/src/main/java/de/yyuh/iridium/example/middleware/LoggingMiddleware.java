package de.yyuh.iridium.example.middleware;

import de.yyuh.iridium.boot.annotation.Order;
import de.yyuh.iridium.boot.middleware.Middleware;
import de.yyuh.iridium.boot.middleware.annotation.MiddlewareComponent;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.shared.log.Log;

/**
 * Example middleware that logs every request with its duration.
 *
 * <p>
 * Runs at {@code @Order(1)} to execute early in the chain.
 * Logs the HTTP method, path, response status, and elapsed time.
 * </p>
 */
@Order(1)
@MiddlewareComponent(path = "/test/*")
public final class LoggingMiddleware implements Middleware {

  private static Log log = Log.of(LoggingMiddleware.class);

  /** {@inheritDoc} */
  @Override
  public Response handle(final RequestContext ctx, final Next next) throws Exception {
    final var start = System.currentTimeMillis();

    log.info("\u2192 %s %s", ctx.method(), ctx.path());

    final var response = next.handle(ctx);

    final var elapsed = System.currentTimeMillis() - start;

    log.info("\u2190 " + ctx.method() + " " + ctx.path() + " \u2192 " + response.status() + " (" + elapsed + "ms)");

    return response;
  }
}
