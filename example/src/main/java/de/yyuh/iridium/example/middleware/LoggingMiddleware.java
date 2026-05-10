package de.yyuh.iridium.example.middleware;

import de.yyuh.iridium.boot.annotation.Order;
import de.yyuh.iridium.boot.middleware.Middleware;
import de.yyuh.iridium.boot.middleware.annotation.MiddlewareComponent;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;

@Order(1)
@MiddlewareComponent
public final class LoggingMiddleware implements Middleware {

  @Override
  public Response handle(final RequestContext ctx, final Next next) throws Exception {
    final var start = System.currentTimeMillis();

    System.out.println("→ " + ctx.method() + " " + ctx.path());

    final var response = next.handle(ctx);

    final var elapsed = System.currentTimeMillis() - start;

    System.out.println(
        "← " + ctx.method() + " " + ctx.path() + " → " + response.status() + " (" + elapsed + "ms)");

    return response;
  }
}
