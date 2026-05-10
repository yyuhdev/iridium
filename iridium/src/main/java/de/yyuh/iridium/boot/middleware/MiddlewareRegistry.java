package de.yyuh.iridium.boot.middleware;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.IridiumComponentRegistry;
import de.yyuh.iridium.boot.annotation.Handle;
import de.yyuh.iridium.boot.annotation.Order;
import de.yyuh.iridium.boot.middleware.annotation.MiddlewareComponent;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.boot.scanner.ClasspathScanner;
import de.yyuh.iridium.boot.web.IridiumWebServer;

/**
 * Discovers {@link MiddlewareComponent @MiddlewareComponent} classes on
 * the classpath and registers them with the web server in the order
 * defined by {@link de.yyuh.iridium.boot.annotation.Order @Order}.
 */
@NullMarked
public final class MiddlewareRegistry implements IridiumComponentRegistry {

  private final IridiumWebServer server;

  /**
   * Constructs a registry tied to the given server.
   *
   * @param server the web server to register middleware with
   */
  public MiddlewareRegistry(final IridiumWebServer server) {
    this.server = server;
  }

  /** {@inheritDoc} */
  @Override
  public void scan() {
    final var middlewareClasses = ClasspathScanner.withAnnotation(MiddlewareComponent.class);
    final var chain = new ArrayList<Middleware>();

    for (final var clazz : middlewareClasses) {
      final var instance = instantiateMiddleware(clazz);
      chain.add(instance);
    }

    chain.sort(Comparator.comparingInt(MiddlewareRegistry::orderOf));

    server.setMiddlewares(chain);
  }

  private Middleware instantiateMiddleware(final Class<?> clazz) {
    final var instanceResult = this.instantiate(clazz);

    if (instanceResult.isErr()) {
      throw new IllegalStateException(
          "Failed to instantiate middleware " + clazz.getName() + ": " + instanceResult.err().get());
    }

    final var instance = instanceResult.ok().get();

    if (instance instanceof final Middleware middleware) {
      return middleware;
    }

    return buildAnnotationBasedMiddleware(instance);
  }

  private static Middleware buildAnnotationBasedMiddleware(final Object instance) {
    final var handlers = new ArrayList<Method>();

    for (final var method : instance.getClass().getDeclaredMethods()) {
      if (hasHandleMetaAnnotation(method)) {
        method.setAccessible(true);
        handlers.add(method);
      }
    }

    if (handlers.isEmpty()) {
      throw new IllegalStateException(
          "@Middleware class " + instance.getClass().getName()
              + " does not implement Middleware and has no @Handle-annotated methods");
    }

    return (ctx, next) -> {
      for (final var method : handlers) {
        final var result = method.invoke(instance, ctx);

        if (result instanceof final Response response) {
          return response;
        }
      }

      return next.handle(ctx);
    };
  }

  private static boolean hasHandleMetaAnnotation(final Method method) {
    for (final var annotation : method.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(Handle.class)) {
        return true;
      }
    }

    return false;
  }

  private static int orderOf(final Middleware middleware) {
    final var order = middleware.getClass().getAnnotation(Order.class);
    return order != null ? order.value() : 0;
  }
}
