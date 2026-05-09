package de.yyuh.iridium.example.controller;

import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.response.Response;

@RestController
public final class ExampleController {

  @GET("/test/{id}")
  public Response handle(final RequestContext ctx) {
    final var id = ctx.pathVariable("id").orElse("unknown");

    return Response.ok("test id: " + id);
  }
}
