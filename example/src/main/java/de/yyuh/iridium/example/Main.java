package de.yyuh.iridium.example;

import de.yyuh.iridium.boot.Iridium;
import de.yyuh.iridium.boot.IridiumBootstrap;

/**
 * Entry point for the example Iridium application.
 *
 * <p>
 * Starts an HTTP server on {@code 0.0.0.0:8080} (default),
 * discovers the {@link de.yyuh.iridium.example.controller.ExampleController}
 * and {@link de.yyuh.iridium.example.middleware.LoggingMiddleware},
 * and begins accepting requests.
 * </p>
 */
@IridiumBootstrap
public final class Main {

  /**
   * Bootstraps and starts the application.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(final String[] args) {
    Iridium.run(Main.class, args);
  }
}
