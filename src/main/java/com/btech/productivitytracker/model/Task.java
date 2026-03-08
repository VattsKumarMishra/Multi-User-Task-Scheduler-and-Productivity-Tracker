package com.btech.productivitytracker.model;

import com.btech.productivitytracker.exception.InvalidTaskDataException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

public class Task implements Comparable<Task> {
    private int id;
    private String title;
    private String description;
    private LocalDate deadline;
    private int priority;
    private boolean completed;
    private final String ownerUsername;
    private final LocalDateTime createdAt;

    public Task(int id, String title, LocalDate deadline, int priority, String ownerUsername) {
        this(id, title, "", deadline, priority, ownerUsername);
    }

    public Task(int id, String title, String description, LocalDate deadline, int priority, String ownerUsername) {
        this(id, title, description, deadline, priority, ownerUsername, LocalDateTime.now(), false);
    }

    public Task(int id,
                String title,
                String description,
                LocalDate deadline,
                int priority,
                String ownerUsername,
                LocalDateTime createdAt,
                boolean completed) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskDataException("Task title cannot be blank.");
        }
        if (deadline == null) {
            throw new InvalidTaskDataException("Deadline cannot be null.");
        }
        if (priority < 1 || priority > 10) {
            throw new InvalidTaskDataException("Priority must be between 1 and 10.");
        }

        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.deadline = deadline;
        this.priority = priority;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskDataException("Task title cannot be blank.");
        }
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new InvalidTaskDataException("Deadline cannot be null.");
        }
        this.deadline = deadline;
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new InvalidTaskDataException("Priority must be between 1 and 10.");
        }
        this.priority = priority;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public int compareTo(Task other) {
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        return this.deadline.compareTo(other.deadline);
    }

    public String toDataLine() {
        return id + "|"
                + ownerUsername + "|"
                + priority + "|"
                + completed + "|"
                + deadline + "|"
                + createdAt + "|"
                + encode(title) + "|"
                + encode(description);
    }

    public static Task fromDataLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException("Invalid task record: " + line);
        }

        int id = Integer.parseInt(parts[0]);
        String owner = parts[1];
        int priority = Integer.parseInt(parts[2]);
        boolean completed = Boolean.parseBoolean(parts[3]);
        LocalDate deadline = LocalDate.parse(parts[4]);
        LocalDateTime createdAt = LocalDateTime.parse(parts[5]);
        String title = decode(parts[6]);
        String description = decode(parts[7]);

        return new Task(id, title, description, deadline, priority, owner, createdAt, completed);
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        String status = completed ? "COMPLETED" : "PENDING";
        return "[TaskId=" + id + "] " + title
                + " | Priority=" + priority
                + " | Deadline=" + deadline
                + " | Status=" + status
                + " | Owner=" + ownerUsername;
    }
}
