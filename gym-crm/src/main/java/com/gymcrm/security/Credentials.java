package com.gymcrm.security;

/**
 * Bundles a username and a password together as one value, so a Service
 * method that needs authentication only has to accept one extra parameter
 * ({@code Credentials credentials}) instead of two loose {@code String}s
 * that would be easy to mix up or accidentally pass in the wrong order.
 * <p>
 * This is a Java {@code record} (available since Java 16): a compact,
 * built-in way to declare a small, immutable class that just holds a
 * couple of values together. From this one line, Java automatically
 * generates: a constructor, {@code username()}/{@code password()} accessors
 * (no "get" prefix -- that's the record naming convention), and working
 * {@code equals()}/{@code hashCode()}/{@code toString()} -- all the
 * boilerplate we've been writing by hand in {@code Trainee}, {@code
 * Trainer}, etc.
 */
public record Credentials(String username, String password) {
}
