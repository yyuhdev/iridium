package de.yyuh.iridium.boot.request.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as an HTTP request type.
 *
 * <p>Used by the framework to identify HTTP-method annotations
 * ({@code @GET}, {@code @POST}, etc.) on handler methods.</p>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {}
