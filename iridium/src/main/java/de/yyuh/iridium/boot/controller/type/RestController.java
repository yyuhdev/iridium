package de.yyuh.iridium.boot.controller.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a REST controller.
 *
 * <p>The framework discovers classes with this annotation at startup
 * and registers handler methods annotated with HTTP-method annotations
 * (e.g. {@code @GET}, {@code @POST}) as routes.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestController {

}
