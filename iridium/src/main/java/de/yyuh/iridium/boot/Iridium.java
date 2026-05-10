package de.yyuh.iridium.boot;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.controller.ControllerRegistry;
import de.yyuh.iridium.boot.middleware.MiddlewareRegistry;
import de.yyuh.iridium.boot.web.IridiumWebServer;

/**
 * Bootstraps and starts an Iridium application.
 *
 * <p>Scans the classpath for {@code @MiddlewareComponent} and
 * {@code @RestController} classes, registers them with the embedded
 * HTTP server, and starts listening.</p>
 *
 * <p>Call {@link #run(Class, String[])} from your {@code main} method.</p>
 */
@NullMarked
public final class Iridium {

  /**
   * Constructs the application, registers all components, and
   * starts the web server.
   *
   * @param host the bind address
   * @param port the listen port
   * @param args command-line arguments (reserved for future use)
   */
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

  /**
   * Entry-point shortcut. Reads {@link IridiumBootstrap} from the
   * given class and starts the application.
   *
   * @param main the class annotated with {@link IridiumBootstrap}
   * @param args command-line arguments
   * @throws IllegalStateException if the class is missing the annotation
   */
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
