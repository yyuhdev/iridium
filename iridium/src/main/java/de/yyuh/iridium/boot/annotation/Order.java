package de.yyuh.iridium.boot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the execution order for middleware components.
 *
 * <p>Lower values execute first. Components without this annotation
 * are treated as having {@code value = 0} (highest priority).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

  /** Execution priority — lower values run earlier in the chain. */
  int value() default 0;
}
