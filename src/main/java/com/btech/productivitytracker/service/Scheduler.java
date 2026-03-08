package com.btech.productivitytracker.service;

import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;
import com.btech.productivitytracker.thread.TaskExecutionRunnable;
import com.btech.productivitytracker.thread.TaskExecutionThread;
import com.btech.productivitytracker.util.ExecutionTimeLogger;

import java.util.ArrayList;
import java.util.List;

public class Scheduler {
    private final TaskManager taskManager;

    public Scheduler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @ExecutionTimeLogger("TaskExecutionSimulation")
    public void simulateTaskExecution(User user) {
        List<Task> pendingTasks = taskManager.getPendingTasksForExecution(user);
        if (pendingTasks.isEmpty()) {
            System.out.println("No pending tasks found for execution.");
            return;
        }

        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < pendingTasks.size(); i++) {
            Task task = pendingTasks.get(i);
            Thread worker;

            if (i % 2 == 0) {
                worker = new TaskExecutionThread(taskManager, task, user);
            } else {
                worker = new Thread(new TaskExecutionRunnable(taskManager, task, user), "RunnableWorker-Task-" + task.getId());
            }

            worker.setPriority(mapTaskPriorityToThreadPriority(task.getPriority()));
            workers.add(worker);
            worker.start();
        }

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Execution join interrupted.");
            }
        }

        System.out.println("Task simulation finished. All eligible tasks are marked completed.");
    }

    private int mapTaskPriorityToThreadPriority(int taskPriority) {
        if (taskPriority >= 8) {
            return Thread.MAX_PRIORITY;
        }
        if (taskPriority >= 5) {
            return Thread.NORM_PRIORITY;
        }
        return 3;
    }
}
