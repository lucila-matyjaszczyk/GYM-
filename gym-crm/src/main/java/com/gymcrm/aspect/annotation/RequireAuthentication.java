package com.gymcrm.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: put it on a Service method whose FIRST parameter is a
 * {@code com.gymcrm.security.Credentials}, and {@code AuthenticationAspect}
 * will check those credentials against the database before letting the
 * method run -- per note 2 of the Hibernate task, every operation except
 * creating a Trainee/Trainer profile needs this.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireAuthentication {
}
