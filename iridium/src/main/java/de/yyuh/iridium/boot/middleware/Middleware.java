package de.yyuh.iridium.boot.middleware;

import org.jspecify.annotations.Nullable;

import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.response.Response;

@FunctionalInterface
public interface Middleware {

  @Nullable
  Response handle(
      final RequestContext ctx,
      final Next next) throws Exception;

  @FunctionalInterface
  interface Next {

    Response handle(final RequestContext ctx) throws Exception;
  }
}
