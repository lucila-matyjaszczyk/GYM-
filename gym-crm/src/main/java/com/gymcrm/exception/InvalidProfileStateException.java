package com.gymcrm.exception;

/**
 * Thrown when an activate/de-activate call would not actually change
 * anything -- e.g. trying to activate a Trainee/Trainer that is already
 * active. Per the task notes, activate/de-activate are deliberately NOT
 * idempotent: calling "activate" twice in a row is treated as a mistake
 * (maybe two tabs open, a double click, a retried request) and rejected
 * loudly, instead of silently succeeding both times.
 */
public class InvalidProfileStateException extends RuntimeException {

    public InvalidProfileStateException(String message) {
        super(message);
    }
}
