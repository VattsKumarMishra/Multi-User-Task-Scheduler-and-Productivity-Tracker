package com.btech.productivitytracker.service;

import com.btech.productivitytracker.exception.InvalidTaskDataException;
import com.btech.productivitytracker.exception.TaskNotFoundException;
import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;
import com.btech.productivitytracker.util.ExecutionTimeLogger;
import com.btech.productivitytracker.util.FileUtil;
import com.btech.productivitytracker.util.Repository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.stream.Collectors;

public class TaskManager implements TaskOperations {
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final PriorityQueue<Task> priorityQueue = new PriorityQueue<>(Comparator.naturalOrder());
    private final Stack<Task> completionHistory = new Stack<>();
    private final Repository<Task> taskRepository = new Repository<>();
    private int nextTaskId = 1;

    public synchronized void loadTasks(String tasksFile) throws IOException {
        List<Task> loadedTasks = FileUtil.loadTasksWithCharStream(tasksFile);

        tasks.clear();
        priorityQueue.clear();
        completionHistory.clear();
        taskRepository.asMap().clear();

        for (Task task : loadedTasks) {
            tasks.add(task);
            taskRepository.save(String.valueOf(task.getId()), task);
            if (task.isCompleted()) {
                completionHistory.push(task);
            } else {
                priorityQueue.offer(task);
            }
        }

        nextTaskId = loadedTasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    public synchronized void saveTasks(String tasksFile) throws IOException {
        FileUtil.saveTasksWithCharStream(tasks, tasksFile);
    }

    @ExecutionTimeLogger("AddTaskOperation")
    @Override
    public synchronized void addTask(Task task) {
        tasks.add(task);
        taskRepository.save(String.valueOf(task.getId()), task);
        if (!task.isCompleted()) {
            priorityQueue.offer(task);
        }
    }

    // Method overloading: another addTask form with task fields.
    public synchronized Task addTask(String title,
                                     String description,
                                     LocalDate deadline,
                                     int priority,
                                     User owner) {
        Task task = new Task(nextTaskId++, title, description, deadline, priority, owner.getUsername());
        addTask(task);
        return task;
    }

    @Override
    public synchronized void deleteTask(int taskId, User actingUser) throws TaskNotFoundException {
        Task task = findById(taskId);
        validateTaskAccess(task, actingUser);

        tasks.remove(task);
        priorityQueue.remove(task);
        completionHistory.remove(task);
        taskRepository.delete(String.valueOf(taskId));
    }

    @Override
    public synchronized void completeTask(int taskId, User actingUser) throws TaskNotFoundException {
        Task task = findById(taskId);
        validateTaskAccess(task, actingUser);

        if (!task.isCompleted()) {
            task.setCompleted(true);
            priorityQueue.remove(task);
            completionHistory.push(task);
        }
    }

    @Override
    public synchronized List<Task> viewTasks(User actingUser) {
        if (actingUser.canManageAllTasks()) {
            return new ArrayList<>(tasks);
        }

        return tasks.stream()
                .filter(task -> task.getOwnerUsername().equalsIgnoreCase(actingUser.getUsername()))
                .collect(Collectors.toList());
    }

    // Method overloading: view tasks with optional pending filter.
    public synchronized List<Task> viewTasks(User actingUser, boolean onlyPending) {
        return viewTasks(actingUser).stream()
                .filter(task -> !onlyPending || !task.isCompleted())
                .collect(Collectors.toList());
    }

    public synchronized void updateTaskPriorityAndDeadline(int taskId,
                                                           int newPriority,
                                                           LocalDate newDeadline,
                                                           User actingUser) throws TaskNotFoundException {
        Task task = findById(taskId);
        validateTaskAccess(task, actingUser);

        priorityQueue.remove(task);
        task.setPriority(newPriority);
        task.setDeadline(newDeadline);

        if (!task.isCompleted()) {
            priorityQueue.offer(task);
        }
    }

    public synchronized List<Task> getTasksSortedByPriority(User actingUser) {
        // Lambda expression for custom sorting.
        return viewTasks(actingUser).stream()
                .filter(task -> !task.isCompleted())
                .sorted((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> filterTasksByDeadline(User actingUser, LocalDate beforeOrOnDate) {
        // Lambda expression for filtering tasks.
        return viewTasks(actingUser).stream()
                .filter(task -> !task.getDeadline().isAfter(beforeOrOnDate))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> getPendingTasksForExecution(User actingUser) {
        return getTasksSortedByPriority(actingUser);
    }

    public synchronized List<Task> getCompletionHistory(User actingUser) {
        if (actingUser.canManageAllTasks()) {
            return new ArrayList<>(completionHistory);
        }

        return completionHistory.stream()
                .filter(task -> task.getOwnerUsername().equalsIgnoreCase(actingUser.getUsername()))
                .collect(Collectors.toList());
    }

    public synchronized Task peekHighestPriorityTask() {
        return priorityQueue.peek();
    }

    private Task findById(int taskId) throws TaskNotFoundException {
        return taskRepository.findByKey(String.valueOf(taskId))
                .orElseThrow(() -> new TaskNotFoundException("Task with ID " + taskId + " was not found."));
    }

    private void validateTaskAccess(Task task, User actingUser) {
        boolean ownerMatch = task.getOwnerUsername().equalsIgnoreCase(actingUser.getUsername());
        if (!ownerMatch && !actingUser.canManageAllTasks()) {
            throw new InvalidTaskDataException("Access denied for task ID " + task.getId());
        }
    }
}
