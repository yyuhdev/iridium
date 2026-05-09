package de.yyuh.iridium.boot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IridiumBootstrap {

  int port() default 8080;

  String host() default "0.0.0.0";

}
