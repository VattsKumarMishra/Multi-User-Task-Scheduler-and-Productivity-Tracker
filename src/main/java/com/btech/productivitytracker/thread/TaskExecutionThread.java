package com.btech.productivitytracker.thread;

import com.btech.productivitytracker.exception.TaskNotFoundException;
import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;
import com.btech.productivitytracker.service.TaskManager;

public class TaskExecutionThread extends Thread {
    private final TaskManager taskManager;
    private final Task task;
    private final User actingUser;

    public TaskExecutionThread(TaskManager taskManager, Task task, User actingUser) {
        super("ThreadWorker-Task-" + task.getId());
        this.taskManager = taskManager;
        this.task = task;
        this.actingUser = actingUser;
    }

    @Override
    public void run() {
        System.out.println("[Thread] Starting task " + task.getId() + " on " + getName());
        try {
            Thread.sleep(Math.max(500L, 1600L - (task.getPriority() * 100L)));
            taskManager.completeTask(task.getId(), actingUser);
            System.out.println("[Thread] Completed task " + task.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Thread] Task interrupted for ID " + task.getId());
        } catch (TaskNotFoundException e) {
            System.out.println("[Thread] " + e.getMessage());
        }
    }
}
