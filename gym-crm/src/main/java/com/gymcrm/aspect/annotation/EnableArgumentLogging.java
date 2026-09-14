package com.gymcrm.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: put it on a method to make {@code LoggingAspect} log
 * that method's argument values right before it runs. Carries no data of
 * its own -- its mere presence on a method is the whole signal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnableArgumentLogging {
}
