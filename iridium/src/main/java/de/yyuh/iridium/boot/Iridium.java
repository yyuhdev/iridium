package de.yyuh.iridium.boot;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.credentials.provider.EnvCredentialProvider;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.iridium.boot.controller.ControllerRegistry;
import de.yyuh.iridium.boot.middleware.MiddlewareRegistry;
import de.yyuh.iridium.boot.scanner.ClasspathScanner;
import de.yyuh.iridium.boot.scanner.ClasspathScanner.ClasspathScanException;
import de.yyuh.iridium.boot.web.IridiumWebServer;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.libs.core.injection.Injector;
import de.yyuh.libs.core.timer.Timer;

/**
 * Bootstraps and starts an Iridium application.
 *
 * <p>
 * Scans the classpath for {@code @MiddlewareComponent} and
 * {@code @RestController} classes, registers them with the embedded
 * HTTP server, and starts listening.
 * </p>
 *
 * <p>
 * Call {@link #run(Class, String[])} from your {@code main} method.
 * </p>
 */
@NullMarked
public final class Iridium {

  private static final Log log = Log.of(Iridium.class);

  private final Injector injector = new Injector();
  private final Celery celery;

  public Iridium(
      final String host,
      final int port,
      final String[] args) {
    final var celery = Celery.builder()
        .registerCredentialProvider(new EnvCredentialProvider())
        .registerPlatform(CeleryMongoDBPlatform.class)
        .withId(host)
        .build();

    this(host, port, args, celery);
  }

  public Iridium(
      final String host,
      final int port,
      final String[] args,
      final Celery celery) {
    this.celery = celery;

    final var startupTimer = Timer.start();
    log.info("Starting Iridium on %s:%d ...", host, port);

    final var server = new IridiumWebServer(host, port);

    this.injector.bind(Injector.class, this.injector);
    this.injector.bind(Celery.class, celery);

    final var middlewareRegistry = new MiddlewareRegistry(server);
    this.injector.inject(middlewareRegistry);

    middlewareRegistry.scan();

    final var controllerRegistry = new ControllerRegistry(server);
    this.injector.inject(controllerRegistry);

    controllerRegistry.scan();

    server.start();

    log.info("Iridium is ready on http://%s:%d/ (startup took %dms)",
        host,
        port,
        startupTimer.toTimeUnit(TimeUnit.MILLISECONDS));

    log.info("");
    log.info("%s ██ ▄▄▄▄  ▄▄ ▄▄▄▄  ▄▄ ▄▄ ▄▄ ▄▄   ▄▄", Log.BLUE);
    log.info("%s ██ ██▄█▄ ██ ██▀██ ██ ██ ██ ██▀▄▀██", Log.BLUE);
    log.info("%s ██ ██ ██ ██ ████▀ ██ ▀███▀ ██   ██", Log.BLUE);
    log.info("");
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

  public static void run(
      final Class<?> main,
      final String[] args,
      final Celery celery) {
    if (!main.isAnnotationPresent(IridiumBootstrap.class)) {
      throw new IllegalStateException("Missing @IridiumBootstrap");
    }

    final var annotation = main.getAnnotation(IridiumBootstrap.class);

    new Iridium(annotation.host(), annotation.port(), args, celery);
  }
}
