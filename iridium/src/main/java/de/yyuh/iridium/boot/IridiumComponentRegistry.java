package de.yyuh.iridium.boot;

import de.yyuh.iridium.shared.result.Result;

/**
 * Common contract for component registries that scan the classpath
 * and instantiate annotated classes.
 *
 * <p>Implementations — e.g. {@link de.yyuh.iridium.boot.controller.ControllerRegistry}
 * and {@link de.yyuh.iridium.boot.middleware.MiddlewareRegistry} — override
 * {@link #scan()} to discover and register their respective component types.</p>
 */
public interface IridiumComponentRegistry {

  /**
   * Scans the classpath for annotated components and registers them.
   */
  public void scan();

  /**
   * Instantiates a class via its no-arg constructor.
   *
   * @param clazz the class to instantiate
   * @return an {@link Result.Ok} with the instance, or an {@link Result.Err}
   *         with the error message
   */
  public default Result<Object, String> instantiate(final Class<?> clazz) {
    return Result.of(() -> {
      return (Object) clazz.getDeclaredConstructor().newInstance();
    }).mapErr(Exception::getMessage);
  }

}
