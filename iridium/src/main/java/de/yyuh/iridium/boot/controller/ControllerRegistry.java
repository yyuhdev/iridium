package de.yyuh.iridium.boot.controller;

import java.lang.annotation.Annotation;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.type.RequestType;
import de.yyuh.iridium.boot.scanner.ClasspathScanner;
import de.yyuh.iridium.boot.web.IridiumWebServer;
import de.yyuh.iridium.shared.result.Result;

@NullMarked
public final class ControllerRegistry {

  private final IridiumWebServer server;

  public ControllerRegistry(final IridiumWebServer server) {
    this.server = server;
  }

  public void scan() {
    final var controllers = ClasspathScanner.withAnnotation(
        RestController.class);

    for (final var controllerClass : controllers) {
      final var instance = instantiate(controllerClass);

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

        server.createPath(path, httpMethod, instance, method);
      }
    }
  }

  private static Annotation findRequestAnnotation(final java.lang.reflect.Method method) {
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

  private static Object instantiate(final Class<?> clazz) {
    return Result.of(() -> {
      return clazz.getDeclaredConstructor().newInstance();
    }).mapErr(Exception::getMessage);
  }
}
