package de.yyuh.iridium.boot.request.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an HTTP {@code POST} request to a handler method.
 *
 * <p>The {@link #value()} is the route path pattern.</p>
 */
@RequestType
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface POST {

  /**
   * The route path pattern.
   *
   * @return the path, e.g. {@code /users}
   */
  String value() default "";
}
