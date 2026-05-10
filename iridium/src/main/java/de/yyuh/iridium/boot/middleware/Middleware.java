package de.yyuh.iridium.boot.middleware;

import org.jspecify.annotations.Nullable;

import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;

/**
 * A functional interface for HTTP middleware.
 *
 * <p>Implementations may intercept a request, modify the context,
 * delegate to the next handler in the chain (or skip it), and
 * optionally mutate the response.</p>
 *
 * <p>Returning {@code null} passes control to the next handler.
 * Returning a {@link Response} short-circuits the chain.</p>
 */
@FunctionalInterface
public interface Middleware {

  /**
   * Handles a request within the middleware chain.
   *
   * @param ctx  the request context
   * @param next the next handler in the chain
   * @return a response to short-circuit, or {@code null} to continue
   * @throws Exception if an error occurs during handling
   */
  @Nullable
  Response handle(
      final RequestContext ctx,
      final Next next) throws Exception;

  /**
   * Represents the next stage in the middleware chain.
   */
  @FunctionalInterface
  interface Next {

    /**
     * Delegates handling to the next middleware or route handler.
     *
     * @param ctx the request context
     * @return the response from downstream handlers
     * @throws Exception if an error occurs
     */
    Response handle(final RequestContext ctx) throws Exception;
  }
}
