package de.yyuh.iridium.boot.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.IridiumComponentRegistry;
import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.type.RequestType;
import de.yyuh.iridium.boot.scanner.ClasspathScanner;
import de.yyuh.iridium.boot.web.IridiumWebServer;
import de.yyuh.iridium.shared.log.Log;
import de.yyuh.iridium.shared.result.Result;

/**
 * Discovers {@link RestController @RestController} classes on the
 * classpath and registers their handler methods as HTTP routes
 * with the web server.
 */
@NullMarked
public final class ControllerRegistry implements IridiumComponentRegistry {

  private static final Log log = Log.of(ControllerRegistry.class);

  private final IridiumWebServer server;

  /**
   * Constructs a registry tied to the given server.
   *
   * @param server the web server to register routes with
   */
  public ControllerRegistry(final IridiumWebServer server) {
    this.server = server;
  }

  /** {@inheritDoc} */
  @Override
  public void scan() {
    final var controllers = ClasspathScanner.withAnnotation(
        RestController.class);

    log.info("Found %d @RestController classes", controllers.size());

    for (final var controllerClass : controllers) {
      final var instanceResult = this.instantiate(controllerClass);

      if (instanceResult.isErr()) {
        throw new IllegalStateException(instanceResult.err().get());
      }

      final var instance = instanceResult.ok().get();

      for (final var method : controllerClass.getDeclaredMethods()) {
        final var annotation = findRequestAnnotation(method);

        if (annotation == null) {
          continue;
        }

        final var pathResult = getPath(annotation);
        final var httpMethod = annotation.annotationType().getSimpleName();

        if (pathResult.isErr()) {
          continue;
        }

        final var path = pathResult.ok().get();

        log.debug("  %s %s -> %s.%s()", httpMethod, path,
            controllerClass.getSimpleName(), method.getName());
        server.addRoute(path, httpMethod, instance, method);
      }
    }
  }

  private static Annotation findRequestAnnotation(final Method method) {
    for (final var annotation : method.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(RequestType.class)) {
        return annotation;
      }
    }

    return null;
  }

  private static Result<String, String> getPath(final Annotation annotation) {
    return Result.of(() -> {
      return (String) annotation.annotationType()
          .getMethod("value")
          .invoke(annotation);
    }).mapErr(Exception::getMessage);
  }

}
