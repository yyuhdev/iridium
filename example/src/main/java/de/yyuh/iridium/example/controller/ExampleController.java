package de.yyuh.iridium.example.controller;

import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.response.Response;

/**
 * A demonstration REST controller with a single {@code GET} endpoint.
 */
@RestController
public final class ExampleController {

  /**
   * Handles {@code GET /test/{id}} by echoing the path variable.
   *
   * @param ctx the request context containing the path variable {@code id}
   * @return a {@code 200 OK} response with the extracted id
   */
  @GET("/test/{id}")
  public Response handle(final RequestContext ctx) {
    final var id = ctx.pathVariable("id").orElse("unknown");

    return Response.ok("test id: " + id);
  }
}
