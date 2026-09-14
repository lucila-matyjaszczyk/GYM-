package com.gymcrm.exception;

/**
 * Thrown when an operation refers to an entity id that does not exist in
 * storage — e.g. trying to update a Trainee/Trainer that was never created,
 * or creating a Training that points at a trainee/trainer id that doesn't
 * exist. Prevents these situations from silently creating "phantom" records
 * or dangling references instead of failing loudly.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
