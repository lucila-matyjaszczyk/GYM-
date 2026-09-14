package com.gymcrm.security;

/**
 * Thrown when a {@link Credentials} value doesn't match any existing
 * trainee or trainer. See {@code AuthenticationAspect} (in {@code
 * com.gymcrm.aspect}), which is what actually throws this.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
