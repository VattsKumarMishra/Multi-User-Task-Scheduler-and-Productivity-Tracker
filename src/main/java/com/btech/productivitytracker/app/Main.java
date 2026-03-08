package com.btech.productivitytracker.app;

import com.btech.productivitytracker.exception.InvalidTaskDataException;
import com.btech.productivitytracker.exception.TaskNotFoundException;
import com.btech.productivitytracker.model.Admin;
import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;
import com.btech.productivitytracker.service.Scheduler;
import com.btech.productivitytracker.service.TaskManager;
import com.btech.productivitytracker.service.UserService;
import com.btech.productivitytracker.util.ExecutionTimeUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String USERS_FILE = "data/users.dat";
    private static final String TASKS_FILE = "data/tasks.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserService userService = new UserService();
        TaskManager taskManager = new TaskManager();
        Scheduler scheduler = new Scheduler(taskManager);

        initializeData(userService, taskManager);

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt(scanner, "Enter your choice: ");

            try {
                switch (choice) {
                    case 1 -> handleRegistration(scanner, userService);
                    case 2 -> handleLogin(scanner, userService, taskManager, scheduler);
                    case 3 -> {
                        saveAllData(userService, taskManager);
                        running = false;
                        System.out.println("Goodbye. Data saved successfully.");
                    }
                    default -> throw new InvalidTaskDataException("Invalid main menu choice.");
                }
            } catch (InvalidTaskDataException e) {
                // Unchecked exception handling example.
                System.out.println("Input error (unchecked): " + e.getMessage());
            } catch (IOException e) {
                // Checked exception handling example.
                System.out.println("I/O error while saving data: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void initializeData(UserService userService, TaskManager taskManager) {
        try {
            userService.loadUsers(USERS_FILE);
            userService.seedDefaultAdmin();
            userService.saveUsers(USERS_FILE);
            taskManager.loadTasks(TASKS_FILE);
        } catch (IOException e) {
            System.out.println("Startup file loading issue: " + e.getMessage());
            System.out.println("Continuing with fresh in-memory data.");
        }
    }

    private static void printMainMenu() {
        System.out.println("\n==== Multi-User Task Scheduler and Productivity Tracker ====");
        System.out.println("1. Register User");
        System.out.println("2. Login");
        System.out.println("3. Exit");
    }

    private static void handleRegistration(Scanner scanner, UserService userService) {
        System.out.println("\n--- User Registration ---");
        String username = readNonBlank(scanner, "Enter username: ");
        String password = readNonBlank(scanner, "Enter password: ");
        String fullName = readNonBlank(scanner, "Enter full name: ");

        boolean registered = userService.registerUser(username, password, fullName);
        if (registered) {
            System.out.println("Registration successful.");
        } else {
            System.out.println("Username already exists.");
        }
    }

    private static void handleLogin(Scanner scanner,
                                    UserService userService,
                                    TaskManager taskManager,
                                    Scheduler scheduler) {
        System.out.println("\n--- Login ---");
        String username = readNonBlank(scanner, "Username: ");
        String password = readNonBlank(scanner, "Password: ");

        User loggedInUser = userService.login(username, password);
        if (loggedInUser == null) {
            System.out.println("Invalid credentials.");
            return;
        }

        // Runtime polymorphism: Admin overrides showRoleMessage().
        loggedInUser.showRoleMessage();
        userSessionMenu(scanner, loggedInUser, userService, taskManager, scheduler);
    }

    private static void userSessionMenu(Scanner scanner,
                                        User currentUser,
                                        UserService userService,
                                        TaskManager taskManager,
                                        Scheduler scheduler) {
        boolean loggedIn = true;

        while (loggedIn) {
            printUserMenu(currentUser);
            int choice = readInt(scanner, "Choose an option: ");

            try {
                switch (choice) {
                    case 1 -> addTaskFlow(scanner, taskManager, currentUser);
                    case 2 -> printTasks(taskManager.viewTasks(currentUser));
                    case 3 -> printTasks(taskManager.getTasksSortedByPriority(currentUser));
                    case 4 -> filterTaskFlow(scanner, taskManager, currentUser);
                    case 5 -> updateTaskFlow(scanner, taskManager, currentUser);
                    case 6 -> completeTaskFlow(scanner, taskManager, currentUser);
                    case 7 -> deleteTaskFlow(scanner, taskManager, currentUser);
                    case 8 -> printTasks(taskManager.getCompletionHistory(currentUser));
                    case 9 -> ExecutionTimeUtil.invokeWithTiming(
                            scheduler,
                            "simulateTaskExecution",
                            new Class<?>[]{User.class},
                            currentUser
                    );
                    case 10 -> {
                        saveAllData(userService, taskManager);
                        loggedIn = false;
                        System.out.println("Logged out and data saved.");
                    }
                    case 11 -> {
                        if (currentUser instanceof Admin admin) {
                            admin.printRegisteredUsers(userService.getAllUsers());
                        } else {
                            throw new InvalidTaskDataException("Only admin can view all users.");
                        }
                    }
                    case 12 -> {
                        if (currentUser.canManageAllTasks()) {
                            printTasks(taskManager.viewTasks(currentUser));
                        } else {
                            throw new InvalidTaskDataException("Only admin can view all tasks.");
                        }
                    }
                    default -> throw new InvalidTaskDataException("Invalid user menu choice.");
                }
            } catch (TaskNotFoundException e) {
                // Checked exception: declared in method signatures and handled here.
                System.out.println("Task operation error (checked): " + e.getMessage());
            } catch (InvalidTaskDataException e) {
                // Unchecked exception: used for invalid inputs and access errors.
                System.out.println("Validation error (unchecked): " + e.getMessage());
            } catch (IOException e) {
                System.out.println("File operation failed: " + e.getMessage());
            }
        }
    }

    private static void printUserMenu(User currentUser) {
        System.out.println("\n--- User Menu (" + currentUser.getRole() + ") ---");
        System.out.println("1. Add a task");
        System.out.println("2. View all my tasks");
        System.out.println("3. View my tasks sorted by priority");
        System.out.println("4. Filter my tasks by deadline");
        System.out.println("5. Set/update task priority and deadline");
        System.out.println("6. Mark task as completed");
        System.out.println("7. Delete a task");
        System.out.println("8. View completed task history");
        System.out.println("9. Simulate task execution using threads");
        System.out.println("10. Save and logout");

        if (currentUser.canManageAllTasks()) {
            System.out.println("11. View all registered users (Admin)");
            System.out.println("12. View all tasks (Admin)");
        }
    }

    private static void addTaskFlow(Scanner scanner, TaskManager taskManager, User user) {
        System.out.println("\n--- Add Task ---");
        String title = readNonBlank(scanner, "Title: ");
        String description = readNonBlank(scanner, "Description: ");
        LocalDate deadline = readDate(scanner, "Deadline (yyyy-MM-dd): ");
        int priority = readInt(scanner, "Priority (1-10): ");

        Task task = taskManager.addTask(title, description, deadline, priority, user);
        System.out.println("Task added successfully with ID: " + task.getId());
    }

    private static void filterTaskFlow(Scanner scanner, TaskManager taskManager, User user) {
        LocalDate targetDate = readDate(scanner, "Show tasks with deadline on/before (yyyy-MM-dd): ");
        List<Task> filteredTasks = taskManager.filterTasksByDeadline(user, targetDate);
        printTasks(filteredTasks);
    }

    private static void updateTaskFlow(Scanner scanner, TaskManager taskManager, User user) throws TaskNotFoundException {
        int taskId = readInt(scanner, "Task ID to update: ");
        int newPriority = readInt(scanner, "New priority (1-10): ");
        LocalDate newDeadline = readDate(scanner, "New deadline (yyyy-MM-dd): ");
        taskManager.updateTaskPriorityAndDeadline(taskId, newPriority, newDeadline, user);
        System.out.println("Task updated successfully.");
    }

    private static void completeTaskFlow(Scanner scanner, TaskManager taskManager, User user) throws TaskNotFoundException {
        int taskId = readInt(scanner, "Task ID to mark completed: ");
        taskManager.completeTask(taskId, user);
        System.out.println("Task marked as completed.");
    }

    private static void deleteTaskFlow(Scanner scanner, TaskManager taskManager, User user) throws TaskNotFoundException {
        int taskId = readInt(scanner, "Task ID to delete: ");
        taskManager.deleteTask(taskId, user);
        System.out.println("Task deleted successfully.");
    }

    private static void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        System.out.println("\nTasks:");
        tasks.forEach(task -> {
            System.out.println(task);
            if (!task.getDescription().isBlank()) {
                System.out.println("   Description: " + task.getDescription());
            }
        });
    }

    private static int readInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new InvalidTaskDataException("Expected numeric input but got: " + input);
        }
    }

    private static String readNonBlank(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        if (value.isBlank()) {
            throw new InvalidTaskDataException("Input cannot be blank.");
        }
        return value;
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidTaskDataException("Invalid date format. Use yyyy-MM-dd.");
        }
    }

    private static void saveAllData(UserService userService, TaskManager taskManager) throws IOException {
        userService.saveUsers(USERS_FILE);
        taskManager.saveTasks(TASKS_FILE);
    }
}
