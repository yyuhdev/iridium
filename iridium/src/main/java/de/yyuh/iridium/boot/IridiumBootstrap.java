package de.yyuh.iridium.boot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the main entry-point class of an Iridium application.
 *
 * <p>Place this annotation on the class that calls
 * {@link Iridium#run(Class, String[])}. It provides the default
 * host and port for the embedded web server.</p>
 *
 * <p>Example:
 * <pre>{@code
 * @IridiumBootstrap(port = 3000)
 * public final class Main {
 *     public static void main(String[] args) {
 *         Iridium.run(Main.class, args);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IridiumBootstrap {

  /**
   * The port the embedded HTTP server listens on.
   *
   * @return the port number (default {@code 8080})
   */
  int port() default 8080;

  /**
   * The host address the server binds to.
   *
   * @return the host IP or hostname (default {@code 0.0.0.0})
   */
  String host() default "0.0.0.0";

}
