package de.yyuh.iridium.boot;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.controller.ControllerRegistry;
import de.yyuh.iridium.boot.middleware.MiddlewareRegistry;
import de.yyuh.iridium.boot.web.IridiumWebServer;

@NullMarked
public final class Iridium {

  public Iridium(
      final String host,
      final int port,
      final String[] args) {
    final var server = new IridiumWebServer(host, port);

    final var middlewareRegistry = new MiddlewareRegistry(server);
    middlewareRegistry.scan();

    final var controllerRegistry = new ControllerRegistry(server);
    controllerRegistry.scan();

    server.start();
  }

  public static void run(
      final Class<?> main,
      final String[] args) {
    if (!main.isAnnotationPresent(IridiumBootstrap.class)) {
      throw new IllegalStateException("Missing @IridiumBootstrap");
    }

    final var annotation = main.getAnnotation(IridiumBootstrap.class);

    new Iridium(annotation.host(), annotation.port(), args);
  }
}
