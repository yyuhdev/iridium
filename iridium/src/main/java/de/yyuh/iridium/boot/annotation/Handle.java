package de.yyuh.iridium.boot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a request handler within a controller or middleware.
 *
 * <p>This annotation is also used as a meta-annotation by the HTTP-method
 * annotations (e.g. {@code @GET}, {@code @POST}). The framework scans for
 * methods whose annotation is itself annotated with {@code @Handle}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handle {

}
