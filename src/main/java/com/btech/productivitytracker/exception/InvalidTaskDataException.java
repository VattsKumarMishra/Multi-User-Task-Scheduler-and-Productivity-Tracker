package com.btech.productivitytracker.exception;

// Unchecked exception: used for invalid runtime input or permissions.
public class InvalidTaskDataException extends RuntimeException {
    public InvalidTaskDataException(String message) {
        super(message);
    }
}
