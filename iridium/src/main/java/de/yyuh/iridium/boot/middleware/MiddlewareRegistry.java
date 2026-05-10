package de.yyuh.iridium.boot.middleware;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.IridiumComponentRegistry;
import de.yyuh.iridium.boot.annotation.Handle;
import de.yyuh.iridium.boot.annotation.Order;
import de.yyuh.iridium.boot.middleware.annotation.MiddlewareComponent;
import de.yyuh.iridium.boot.response.Response;
import de.yyuh.iridium.boot.scanner.ClasspathScanner;
import de.yyuh.iridium.boot.web.GlobPattern;
import de.yyuh.iridium.boot.web.IridiumWebServer;
import de.yyuh.iridium.shared.log.Log;

/**
 * Discovers {@link MiddlewareComponent @MiddlewareComponent} classes on
 * the classpath and registers them with the web server in the order
 * defined by {@link de.yyuh.iridium.boot.annotation.Order @Order}.
 */
@NullMarked
public final class MiddlewareRegistry implements IridiumComponentRegistry {

  private static final Log log = Log.of(MiddlewareRegistry.class);

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

    middlewareClasses.stream()
        .sorted(Comparator.comparingInt(MiddlewareRegistry::orderOf))
        .forEach(clazz -> {
          final var instance = instantiateMiddleware(clazz);
          final var annotation = clazz.getAnnotation(MiddlewareComponent.class);
          final var wrapped = wrapWithPathFilter(instance, annotation);

          chain.add(wrapped);
        });

    log.info("Found %d Middlewares", chain.size());

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

  private static Middleware wrapWithPathFilter(
      final Middleware middleware,
      final MiddlewareComponent annotation) {
    final var includePatterns = GlobPattern.parseAll(annotation.path());
    final var excludePatterns = GlobPattern.parseAll(annotation.exclude());

    if (includePatterns.isEmpty() && excludePatterns.isEmpty()) {
      return middleware;
    }

    return (ctx, next) -> {
      final var path = ctx.path();

      if (!includePatterns.isEmpty() && !GlobPattern.anyMatches(includePatterns, path)) {
        return next.handle(ctx);
      }

      if (GlobPattern.anyMatches(excludePatterns, path)) {
        return next.handle(ctx);
      }

      return middleware.handle(ctx, next);
    };
  }

  private static int orderOf(final Class<?> clazz) {
    final var order = clazz.getAnnotation(Order.class);
    return order != null ? order.value() : 0;
  }
}
