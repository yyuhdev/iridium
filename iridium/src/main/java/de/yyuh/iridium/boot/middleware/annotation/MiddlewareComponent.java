package de.yyuh.iridium.boot.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a middleware component to be discovered at startup.
 *
 * <p>A middleware can either implement {@link de.yyuh.iridium.boot.middleware.Middleware}
 * or declare handler methods annotated with HTTP-method annotations
 * (which are meta-annotated with {@code @Handle}).</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareComponent {

  /**
   * Path patterns this middleware applies to.
   * An empty array means all paths.
   *
   * @return the path patterns
   */
  String[] path() default {};

  /**
   * Path patterns excluded from this middleware.
   *
   * @return the exclusion patterns
   */
  String[] exclude() default {};
}
