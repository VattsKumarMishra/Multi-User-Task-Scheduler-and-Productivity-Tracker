package com.btech.productivitytracker.service;

import com.btech.productivitytracker.exception.TaskNotFoundException;
import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;

import java.util.List;

public interface TaskOperations {
    void addTask(Task task);

    void deleteTask(int taskId, User actingUser) throws TaskNotFoundException;

    void completeTask(int taskId, User actingUser) throws TaskNotFoundException;

    List<Task> viewTasks(User actingUser);
}
