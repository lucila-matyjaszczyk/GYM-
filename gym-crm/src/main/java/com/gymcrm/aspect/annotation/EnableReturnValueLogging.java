package com.gymcrm.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: put it on a method to make {@code LoggingAspect} log
 * whatever that method returns, right after it finishes successfully.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnableReturnValueLogging {
}
