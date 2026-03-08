package com.btech.productivitytracker.exception;

// Checked exception: caller must handle or declare it.
public class TaskNotFoundException extends Exception {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
