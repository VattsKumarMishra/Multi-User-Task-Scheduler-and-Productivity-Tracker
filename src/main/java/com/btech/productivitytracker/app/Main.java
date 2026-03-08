package com.btech.productivitytracker.app;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// ===========================================================================
//  EXCEPTION 1 — Unchecked Exception
//  extends RuntimeException → compiler does NOT force the caller to handle it
//  Used for: bad user input, permission errors
// ===========================================================================
class InvalidTaskDataException extends RuntimeException {
    public InvalidTaskDataException(String message) {
        super(message);
    }
}

// ===========================================================================
//  EXCEPTION 2 — Checked Exception
//  extends Exception → caller MUST either catch it or declare "throws ..."
//  Used for: task not found (a recoverable condition)
// ===========================================================================
class TaskNotFoundException extends Exception {
    public TaskNotFoundException(String message) {
        super(message);
    }
}

// ===========================================================================
//  CUSTOM ANNOTATION
//  @Retention(RUNTIME) → the annotation survives compilation and is readable
//                        via reflection at runtime
//  @Target(METHOD)     → this annotation can only be placed on methods
// ===========================================================================
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ExecutionTimeLogger {
    String value() default ""; // optional label shown in the timer output
}

// ===========================================================================
//  USER CLASS
//  Demonstrates: Encapsulation (private fields) + Polymorphism base
// ===========================================================================
class User {

    // Encapsulation: all fields are private — accessible only via getters/setters
    private String username;
    private String password;
    private String fullName;

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }

    // Check if the provided password matches this user's password
    public boolean authenticate(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    // These 3 methods are intentionally designed to be overridden in Admin
    // → this is what enables runtime polymorphism
    public String getRole() {
        return "USER";
    }

    public boolean canManageAllTasks() {
        return false;
    }

    public void showRoleMessage() {
        System.out.println("Welcome, " + fullName + "! You are logged in as a standard user.");
    }

    @Override
    public String toString() {
        return "User [ username=" + username + ", name=" + fullName + ", role=" + getRole() + " ]";
    }
}

// ===========================================================================
//  ADMIN CLASS
//  Demonstrates: Inheritance (extends User) + Method Overriding
// ===========================================================================
class Admin extends User {

    // super(...) calls the parent class (User) constructor
    public Admin(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    // @Override = tells the compiler this is an override, not a new method
    // At runtime, if the object is Admin, this version runs instead of User's
    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean canManageAllTasks() {
        return true; // admin can see and modify any user's tasks
    }

    @Override
    public void showRoleMessage() {
        System.out.println("Welcome, " + getFullName() + "! Admin privileges are enabled.");
    }

    // Admin-only feature: print every registered user
    public void printAllUsers(Collection<User> users) {
        System.out.println("\n--- All Registered Users ---");
        for (User user : users) {
            System.out.println(user);
        }
    }
}

// ===========================================================================
//  TASK CLASS
//  Demonstrates: Comparable, Constructor Overloading, File Serialization
// ===========================================================================
class Task implements Comparable<Task> {

    private int           id;
    private String        title;
    private String        description;
    private LocalDate     deadline;
    private int           priority;       // 1 = lowest priority, 10 = highest
    private boolean       completed;
    private final String  ownerUsername;
    private final LocalDateTime createdAt;

    // Constructor overloading — short version, delegates to the full constructor
    public Task(int id, String title, LocalDate deadline, int priority, String ownerUsername) {
        this(id, title, "", deadline, priority, ownerUsername, LocalDateTime.now(), false);
    }

    // Full constructor — validates all data before creating the object (fail-fast)
    public Task(int id, String title, String description, LocalDate deadline,
                int priority, String ownerUsername, LocalDateTime createdAt, boolean completed) {

        if (title == null || title.isBlank()) {
            throw new InvalidTaskDataException("Task title cannot be blank.");
        }
        if (deadline == null) {
            throw new InvalidTaskDataException("Deadline cannot be null.");
        }
        if (priority < 1 || priority > 10) {
            throw new InvalidTaskDataException("Priority must be between 1 and 10.");
        }

        this.id            = id;
        this.title         = title;
        this.description   = (description == null) ? "" : description;
        this.deadline      = deadline;
        this.priority      = priority;
        this.ownerUsername = ownerUsername;
        this.createdAt     = createdAt;
        this.completed     = completed;
    }

    // Getters
    public int           getId()            { return id; }
    public String        getTitle()         { return title; }
    public String        getDescription()   { return description; }
    public LocalDate     getDeadline()      { return deadline; }
    public int           getPriority()      { return priority; }
    public boolean       isCompleted()      { return completed; }
    public String        getOwnerUsername() { return ownerUsername; }
    public LocalDateTime getCreatedAt()     { return createdAt; }

    // Setters with validation
    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new InvalidTaskDataException("Priority must be between 1 and 10.");
        }
        this.priority = priority;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new InvalidTaskDataException("Deadline cannot be null.");
        }
        this.deadline = deadline;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // Comparable<Task>: defines the natural ordering used by PriorityQueue
    // Rule: higher priority number comes first; earlier deadline breaks ties
    @Override
    public int compareTo(Task other) {
        int priorityDiff = Integer.compare(other.priority, this.priority); // descending
        if (priorityDiff != 0) {
            return priorityDiff;
        }
        return this.deadline.compareTo(other.deadline); // ascending if priorities are equal
    }

    // Serialise task to a single pipe-delimited line for file storage
    // Base64 encodes the title so special characters (| or newlines) don't corrupt the format
    public String toDataLine() {
        String encodedTitle = Base64.getEncoder()
                .encodeToString(title.getBytes(StandardCharsets.UTF_8));
        return id + "|" + ownerUsername + "|" + priority + "|"
                + completed + "|" + deadline + "|" + createdAt + "|" + encodedTitle;
    }

    // Deserialise a line back into a Task object
    // Accepts 7 fields (current format) or 8 fields (legacy format that included description)
    public static Task fromDataLine(String line) {
        String[] parts = line.split("\\|", -1); // -1 keeps trailing empty tokens
        if (parts.length != 7 && parts.length != 8) {
            throw new IllegalArgumentException("Invalid task record: " + line);
        }

        int           id        = Integer.parseInt(parts[0]);
        String        owner     = parts[1];
        int           priority  = Integer.parseInt(parts[2]);
        boolean       completed = Boolean.parseBoolean(parts[3]);
        LocalDate     deadline  = LocalDate.parse(parts[4]);
        LocalDateTime createdAt = LocalDateTime.parse(parts[5]);
        String        title     = new String(Base64.getDecoder().decode(parts[6]),
                                             StandardCharsets.UTF_8);

        return new Task(id, title, "", deadline, priority, owner, createdAt, completed);
    }

    @Override
    public String toString() {
        String status = completed ? "DONE" : "PENDING";
        return "[ID=" + id + "]  " + title
                + "  |  Priority=" + priority
                + "  |  Deadline=" + deadline
                + "  |  Status=" + status
                + "  |  Owner=" + ownerUsername;
    }
}

// ===========================================================================
//  GENERIC REPOSITORY
//  Demonstrates: Generics (<T>), Optional
//  A reusable key-value store — used as Repository<Task> and Repository<User>
// ===========================================================================
class Repository<T> {

    private final Map<String, T> storage = new HashMap<>();

    public void save(String key, T value) {
        storage.put(key, value);
    }

    // Returns Optional<T> instead of null — forces callers to handle the absent case
    public Optional<T> findByKey(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    public void delete(String key) {
        storage.remove(key);
    }

    public Collection<T> findAll() {
        return storage.values();
    }

    public Map<String, T> asMap() {
        return storage;
    }
}

// ===========================================================================
//  INTERFACE — Abstraction
//  Defines the contract (what must exist) without defining the implementation
//  TaskManager implements this interface
// ===========================================================================
interface TaskOperations {
    void       addTask(Task task);
    void       deleteTask(int taskId, User user)   throws TaskNotFoundException;
    void       completeTask(int taskId, User user) throws TaskNotFoundException;
    List<Task> viewTasks(User user);
}

// ===========================================================================
//  TASK MANAGER — Core business logic
//  Demonstrates: Interface, Collections, synchronized, Streams, Lambdas
// ===========================================================================
class TaskManager implements TaskOperations {

    // Each collection is chosen for a specific reason:
    private final ArrayList<Task>     allTasks      = new ArrayList<>();      // ordered, general-purpose list
    private final PriorityQueue<Task> priorityQueue = new PriorityQueue<>();  // auto-sorted by Comparable
    private final Stack<Task>         history       = new Stack<>();          // LIFO: last completed on top
    private final Repository<Task>    taskRepo      = new Repository<>();     // O(1) fast lookup by task ID

    private int nextTaskId = 1;

    // synchronized: only one thread can execute this method at a time
    // This prevents two threads from corrupting shared data simultaneously
    public synchronized void loadTasks(String filePath) throws IOException {
        List<Task> loaded = FileUtil.loadTasks(filePath);

        allTasks.clear();
        priorityQueue.clear();
        history.clear();
        taskRepo.asMap().clear();

        for (Task task : loaded) {
            allTasks.add(task);
            taskRepo.save(String.valueOf(task.getId()), task);
            if (task.isCompleted()) {
                history.push(task);
            } else {
                priorityQueue.offer(task); // offer() inserts into the priority queue
            }
        }

        // Continue assigning IDs from where we left off
        nextTaskId = loaded.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    public synchronized void saveTasks(String filePath) throws IOException {
        FileUtil.saveTasks(allTasks, filePath);
    }

    // @ExecutionTimeLogger marks this method to be timed when called via reflection
    @ExecutionTimeLogger("AddTask")
    @Override
    public synchronized void addTask(Task task) {
        allTasks.add(task);
        taskRepo.save(String.valueOf(task.getId()), task);
        if (!task.isCompleted()) {
            priorityQueue.offer(task);
        }
    }

    // Method overloading: same method name, different parameters
    // This version builds the Task object for you from the raw fields
    public synchronized Task addTask(String title, LocalDate deadline, int priority, User owner) {
        Task newTask = new Task(nextTaskId, title, deadline, priority, owner.getUsername());
        nextTaskId++;
        addTask(newTask); // calls the overloaded addTask(Task) above
        return newTask;
    }

    @Override
    public synchronized void deleteTask(int taskId, User user) throws TaskNotFoundException {
        Task task = findById(taskId);
        checkAccess(task, user);

        allTasks.remove(task);
        priorityQueue.remove(task);
        history.remove(task);
        taskRepo.delete(String.valueOf(taskId));
    }

    @Override
    public synchronized void completeTask(int taskId, User user) throws TaskNotFoundException {
        Task task = findById(taskId);
        checkAccess(task, user);

        if (!task.isCompleted()) {
            task.setCompleted(true);
            priorityQueue.remove(task);
            history.push(task); // push onto the completion Stack (LIFO)
        }
    }

    @Override
    public synchronized List<Task> viewTasks(User user) {
        if (user.canManageAllTasks()) {
            return new ArrayList<>(allTasks); // admin sees every task
        }
        // Lambda expression inside stream: filter tasks that belong to this user
        return allTasks.stream()
                .filter(task -> task.getOwnerUsername().equalsIgnoreCase(user.getUsername()))
                .collect(Collectors.toList());
    }

    public synchronized void updateTask(int taskId, int newPriority,
                                        LocalDate newDeadline, User user)
            throws TaskNotFoundException {
        Task task = findById(taskId);
        checkAccess(task, user);

        // Must remove and re-add so PriorityQueue can re-sort with updated values
        priorityQueue.remove(task);
        task.setPriority(newPriority);
        task.setDeadline(newDeadline);
        if (!task.isCompleted()) {
            priorityQueue.offer(task);
        }
    }

    // Stream pipeline: filter → sort by lambda comparator → collect into list
    public synchronized List<Task> getTasksSortedByPriority(User user) {
        return viewTasks(user).stream()
                .filter(task -> !task.isCompleted())
                .sorted((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> filterTasksByDeadline(User user, LocalDate onOrBefore) {
        return viewTasks(user).stream()
                .filter(task -> !task.getDeadline().isAfter(onOrBefore))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> getCompletionHistory(User user) {
        if (user.canManageAllTasks()) {
            return new ArrayList<>(history); // admin sees all completed tasks
        }
        return history.stream()
                .filter(task -> task.getOwnerUsername().equalsIgnoreCase(user.getUsername()))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> getPendingTasks(User user) {
        return getTasksSortedByPriority(user);
    }

    // Uses Optional to safely look up a task — no null checks needed
    // orElseThrow: if Optional is empty, throw the checked exception
    private Task findById(int taskId) throws TaskNotFoundException {
        return taskRepo.findByKey(String.valueOf(taskId))
                .orElseThrow(() -> new TaskNotFoundException("No task found with ID: " + taskId));
    }

    // Throws unchecked exception if the user doesn't own the task and isn't an admin
    private void checkAccess(Task task, User user) {
        boolean isOwner = task.getOwnerUsername().equalsIgnoreCase(user.getUsername());
        if (!isOwner && !user.canManageAllTasks()) {
            throw new InvalidTaskDataException(
                    "You do not have permission to modify task ID: " + task.getId());
        }
    }
}

// ===========================================================================
//  USER SERVICE — manages user registration and login
// ===========================================================================
class UserService {

    private final HashMap<String, User> users    = new HashMap<>();
    private final Repository<User>      userRepo = new Repository<>();

    public void loadUsers(String filePath) throws IOException {
        Map<String, User> loaded = FileUtil.loadUsers(filePath);
        users.clear();
        userRepo.asMap().clear();

        for (Map.Entry<String, User> entry : loaded.entrySet()) {
            users.put(entry.getKey(), entry.getValue());
            userRepo.save(entry.getKey(), entry.getValue());
        }
    }

    public void saveUsers(String filePath) throws IOException {
        FileUtil.saveUsers(users, filePath);
    }

    // Make sure a default admin account always exists on first run
    public void seedDefaultAdmin() {
        if (!users.containsKey("admin")) {
            Admin admin = new Admin("admin", "admin123", "System Admin");
            users.put(admin.getUsername(), admin);
            userRepo.save(admin.getUsername(), admin);
        }
    }

    // Method overloading: 2-argument version calls the 3-argument version
    public boolean registerUser(String username, String password) {
        return registerUser(username, password, username);
    }

    public boolean registerUser(String username, String password, String fullName) {
        if (username.isBlank() || password.isBlank()) {
            throw new InvalidTaskDataException("Username and password cannot be blank.");
        }
        if (users.containsKey(username)) {
            return false; // username already exists
        }
        User newUser = new User(username, password, fullName);
        users.put(username, newUser);
        userRepo.save(username, newUser);
        return true;
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(password)) {
            return user;
        }
        return null; // login failed
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }
}

// ===========================================================================
//  THREAD — Way 1: extend Thread and override run()
//  The run() method body is the code that runs inside the new OS thread
// ===========================================================================
class TaskExecutionThread extends Thread {

    private final TaskManager taskManager;
    private final Task        task;
    private final User        user;

    public TaskExecutionThread(TaskManager taskManager, Task task, User user) {
        super("Worker-Thread-" + task.getId()); // give the thread a readable name
        this.taskManager = taskManager;
        this.task        = task;
        this.user        = user;
    }

    @Override
    public void run() {
        System.out.println("[Thread] Starting task ID=" + task.getId() + "  on thread: " + getName());
        try {
            // Higher priority = shorter sleep = finishes faster (simulates urgency)
            long sleepMillis = Math.max(500L, 1600L - task.getPriority() * 100L);
            Thread.sleep(sleepMillis);

            taskManager.completeTask(task.getId(), user); // synchronized — thread-safe
            System.out.println("[Thread] Finished task ID=" + task.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status, then stop
        } catch (TaskNotFoundException e) {
            System.out.println("[Thread] Error: " + e.getMessage());
        }
    }
}

// ===========================================================================
//  THREAD — Way 2: implement Runnable, pass it to new Thread(...)
//  PREFERRED over extending Thread because:
//  Java only allows single inheritance — implementing Runnable leaves extends free
// ===========================================================================
class TaskExecutionRunnable implements Runnable {

    private final TaskManager taskManager;
    private final Task        task;
    private final User        user;

    public TaskExecutionRunnable(TaskManager taskManager, Task task, User user) {
        this.taskManager = taskManager;
        this.task        = task;
        this.user        = user;
    }

    @Override
    public void run() {
        System.out.println("[Runnable] Starting task ID=" + task.getId()
                + "  on thread: " + Thread.currentThread().getName());
        try {
            long sleepMillis = Math.max(500L, 1600L - task.getPriority() * 100L);
            Thread.sleep(sleepMillis);

            taskManager.completeTask(task.getId(), user);
            System.out.println("[Runnable] Finished task ID=" + task.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TaskNotFoundException e) {
            System.out.println("[Runnable] Error: " + e.getMessage());
        }
    }
}

// ===========================================================================
//  SCHEDULER — creates and manages worker threads
// ===========================================================================
class Scheduler {

    private final TaskManager taskManager;

    public Scheduler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @ExecutionTimeLogger("TaskExecutionSimulation")
    public void simulateTaskExecution(User user) {
        List<Task> pendingTasks = taskManager.getPendingTasks(user);
        if (pendingTasks.isEmpty()) {
            System.out.println("No pending tasks to simulate.");
            return;
        }

        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < pendingTasks.size(); i++) {
            Task   task   = pendingTasks.get(i);
            Thread worker;

            // Alternate between the two threading approaches to demonstrate both
            if (i % 2 == 0) {
                worker = new TaskExecutionThread(taskManager, task, user);          // Way 1
            } else {
                worker = new Thread(                                                 // Way 2
                        new TaskExecutionRunnable(taskManager, task, user),
                        "Worker-Runnable-" + task.getId());
            }

            // Map task priority (1-10) to Java thread priority (1-10)
            if (task.getPriority() >= 8) {
                worker.setPriority(Thread.MAX_PRIORITY);   // 10
            } else if (task.getPriority() >= 5) {
                worker.setPriority(Thread.NORM_PRIORITY);  // 5
            } else {
                worker.setPriority(3);
            }

            workers.add(worker);
            worker.start(); // start() launches a NEW thread — does NOT call run() directly
        }

        // join(): forces the main thread to WAIT here until each worker thread finishes
        // Without join(), the main thread would print "done" before tasks are actually complete
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("All threads finished. Eligible tasks have been marked complete.");
    }
}

// ===========================================================================
//  REFLECTION + ANNOTATION UTILITY
//  Finds a method by name at runtime (not at compile time) and times its execution
// ===========================================================================
final class ExecutionTimeUtil {

    private ExecutionTimeUtil() {
        // Utility class — not meant to be instantiated, so constructor is private
    }

    public static void invokeWithTiming(Object target, String methodName,
                                        Class<?>[] paramTypes, Object... args) {
        try {
            // Reflection: find the method object by its name and parameter types
            Method method = target.getClass().getMethod(methodName, paramTypes);

            long startTime = System.nanoTime();
            method.invoke(target, args); // dynamically call the method
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            // Read our custom annotation to get the display label for the log
            if (method.isAnnotationPresent(ExecutionTimeLogger.class)) {
                ExecutionTimeLogger annotation = method.getAnnotation(ExecutionTimeLogger.class);
                String label = annotation.value().isBlank() ? methodName : annotation.value();
                System.out.println("[Timer] '" + label + "' completed in " + durationMs + " ms");
            }

        } catch (InvocationTargetException e) {
            // InvocationTargetException wraps the real exception — unwrap it
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Method invocation failed: " + methodName, cause);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Cannot find or access method: " + methodName, e);
        }
    }
}

// ===========================================================================
//  FILE I/O UTILITY
//  Demonstrates: Byte Streams (users) + Character Streams (tasks)
//                try-with-resources for automatic stream closing
// ===========================================================================
final class FileUtil {

    private FileUtil() {} // utility class — no instantiation

    // Ensure the file and its parent folders exist before reading or writing
    static void ensureFileExists(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (path.getParent() != null && Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
    }

    // ── BYTE STREAMS (FileInputStream / FileOutputStream) ───────────────────
    // Used for user data — shows raw byte-level file reading/writing
    static Map<String, User> loadUsers(String filePath) throws IOException {
        ensureFileExists(filePath);
        Map<String, User> users = new HashMap<>();

        // try-with-resources: stream is automatically closed when block exits
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            byte[] bytes = inputStream.readAllBytes(); // read the whole file as bytes
            if (bytes.length == 0) {
                return users;
            }

            String fileContent = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = fileContent.split("\\R"); // \\R matches any line ending

            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) continue;

                String username = parts[0];
                String password = parts[1];
                String fullName = parts[2];
                String role     = parts[3];

                User user = "ADMIN".equalsIgnoreCase(role)
                        ? new Admin(username, password, fullName)
                        : new User(username, password, fullName);
                users.put(username, user);
            }
        }
        return users;
    }

    static void saveUsers(Map<String, User> users, String filePath) throws IOException {
        ensureFileExists(filePath);

        StringBuilder sb = new StringBuilder();
        for (User user : users.values()) {
            sb.append(user.getUsername()).append("|")
              .append(user.getPassword()).append("|")
              .append(user.getFullName()).append("|")
              .append(user.getRole()).append("\n");
        }

        // false = overwrite mode (don't append to existing content)
        try (FileOutputStream outputStream = new FileOutputStream(filePath, false)) {
            outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8)); // write bytes
        }
    }

    // ── CHARACTER STREAMS (BufferedReader / FileWriter) ──────────────────────
    // Used for task data — shows text/character-level file reading/writing
    static List<Task> loadTasks(String filePath) throws IOException {
        ensureFileExists(filePath);
        List<Task> tasks = new ArrayList<>();

        // BufferedReader wraps FileReader for efficient line-by-line reading
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) { // readLine = character-level read
                if (!line.isBlank()) {
                    tasks.add(Task.fromDataLine(line));
                }
            }
        }
        return tasks;
    }

    static void saveTasks(List<Task> tasks, String filePath) throws IOException {
        ensureFileExists(filePath);

        // FileWriter writes characters (text) — contrast with FileOutputStream (bytes)
        try (FileWriter writer = new FileWriter(filePath, false)) {
            for (Task task : tasks) {
                writer.write(task.toDataLine());
                writer.write("\n");
            }
        }
    }
}

// ===========================================================================
//  MAIN CLASS — Entry point
//  Note: Only ONE class in a .java file can be "public"
//        The filename must match that public class name → Main.java
// ===========================================================================
public class Main {

    private static final String USERS_FILE = "data/users.dat";
    private static final String TASKS_FILE = "data/tasks.txt";

    public static void main(String[] args) {
        Scanner     scanner     = new Scanner(System.in);
        UserService userService = new UserService();
        TaskManager taskManager = new TaskManager();
        Scheduler   scheduler   = new Scheduler(taskManager); // pass TaskManager via constructor

        // Load persisted data at startup
        // IOException is CHECKED — the compiler forces us to handle it here
        try {
            userService.loadUsers(USERS_FILE);
            userService.seedDefaultAdmin();
            userService.saveUsers(USERS_FILE);
            taskManager.loadTasks(TASKS_FILE);
        } catch (IOException e) {
            System.out.println("Could not load saved data: " + e.getMessage());
            System.out.println("Starting fresh with empty data.");
        }

        boolean running = true;
        while (running) {
            System.out.println("\n========== Task Scheduler ==========");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            int choice = readInt(scanner, "Enter choice: ");

            try {
                if (choice == 1) {
                    String username = readStr(scanner, "Username: ");
                    String password = readStr(scanner, "Password: ");
                    String fullName = readStr(scanner, "Full name: ");
                    boolean success = userService.registerUser(username, password, fullName);
                    System.out.println(success ? "Registered successfully." : "Username already taken.");

                } else if (choice == 2) {
                    String username = readStr(scanner, "Username: ");
                    String password = readStr(scanner, "Password: ");
                    User loggedInUser = userService.login(username, password);

                    if (loggedInUser == null) {
                        System.out.println("Invalid username or password. Try again.");
                    } else {
                        // RUNTIME POLYMORPHISM:
                        // loggedInUser is declared as type User, but the actual object
                        // at runtime could be Admin. Java calls the correct version:
                        //   - Admin object → Admin.showRoleMessage()
                        //   - User object  → User.showRoleMessage()
                        loggedInUser.showRoleMessage();
                        showSessionMenu(scanner, loggedInUser, userService, taskManager, scheduler);
                    }

                } else if (choice == 3) {
                    saveAll(userService, taskManager);
                    running = false;
                    System.out.println("Goodbye! Data saved.");

                } else {
                    throw new InvalidTaskDataException("Please choose 1, 2, or 3.");
                }

            } catch (InvalidTaskDataException e) {
                System.out.println("Input error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    // ── Session menu after login ─────────────────────────────────────────────
    static void showSessionMenu(Scanner scanner, User currentUser,
                                UserService userService, TaskManager taskManager,
                                Scheduler scheduler) {

        boolean loggedIn = true;
        while (loggedIn) {

            System.out.println("\n--- Menu (" + currentUser.getRole()
                    + ": " + currentUser.getUsername() + ") ---");
            System.out.println("1.  Add task");
            System.out.println("2.  View my tasks");
            System.out.println("3.  View tasks sorted by priority");
            System.out.println("4.  Filter tasks by deadline");
            System.out.println("5.  Update task priority / deadline");
            System.out.println("6.  Mark task as complete");
            System.out.println("7.  Delete task");
            System.out.println("8.  View completion history");
            System.out.println("9.  Simulate execution (threads demo)");
            System.out.println("10. Save and logout");
            if (currentUser.canManageAllTasks()) {
                System.out.println("11. View all registered users  [Admin only]");
                System.out.println("12. View all tasks             [Admin only]");
            }

            int choice = readInt(scanner, "Choice: ");

            try {
                if (choice == 1) {
                    String    title    = readStr(scanner, "Task title: ");
                    LocalDate deadline = readDate(scanner, "Deadline (yyyy-MM-dd): ");
                    int       priority = readInt(scanner, "Priority (1-10): ");
                    Task task = taskManager.addTask(title, deadline, priority, currentUser);
                    System.out.println("Task added with ID=" + task.getId());

                } else if (choice == 2) {
                    printTasks(taskManager.viewTasks(currentUser));

                } else if (choice == 3) {
                    printTasks(taskManager.getTasksSortedByPriority(currentUser));

                } else if (choice == 4) {
                    LocalDate before = readDate(scanner, "Show tasks due on/before (yyyy-MM-dd): ");
                    printTasks(taskManager.filterTasksByDeadline(currentUser, before));

                } else if (choice == 5) {
                    int       taskId      = readInt(scanner, "Task ID to update: ");
                    int       newPriority = readInt(scanner, "New priority (1-10): ");
                    LocalDate newDeadline = readDate(scanner, "New deadline (yyyy-MM-dd): ");
                    taskManager.updateTask(taskId, newPriority, newDeadline, currentUser);
                    System.out.println("Task updated successfully.");

                } else if (choice == 6) {
                    int taskId = readInt(scanner, "Task ID to mark complete: ");
                    taskManager.completeTask(taskId, currentUser);
                    System.out.println("Task marked as complete.");

                } else if (choice == 7) {
                    int taskId = readInt(scanner, "Task ID to delete: ");
                    taskManager.deleteTask(taskId, currentUser);
                    System.out.println("Task deleted.");

                } else if (choice == 8) {
                    printTasks(taskManager.getCompletionHistory(currentUser));

                } else if (choice == 9) {
                    // Java Reflection: call simulateTaskExecution() by its name as a String
                    // ExecutionTimeUtil finds the method, calls it, and logs how long it took
                    ExecutionTimeUtil.invokeWithTiming(
                            scheduler,
                            "simulateTaskExecution",
                            new Class<?>[]{User.class},
                            currentUser
                    );

                } else if (choice == 10) {
                    saveAll(userService, taskManager);
                    loggedIn = false;
                    System.out.println("Logged out. Data saved.");

                } else if (choice == 11) {
                    // instanceof pattern matching (Java 16+):
                    // checks the type AND casts in a single expression
                    if (currentUser instanceof Admin admin) {
                        admin.printAllUsers(userService.getAllUsers());
                    } else {
                        throw new InvalidTaskDataException("Only admin can view all users.");
                    }

                } else if (choice == 12) {
                    if (currentUser.canManageAllTasks()) {
                        printTasks(taskManager.viewTasks(currentUser));
                    } else {
                        throw new InvalidTaskDataException("Only admin can view all tasks.");
                    }

                } else {
                    throw new InvalidTaskDataException("Invalid choice. Please try again.");
                }

            } catch (TaskNotFoundException e) {
                // Checked exception — catching it here because callers declared throws
                System.out.println("Task not found: " + e.getMessage());
            } catch (InvalidTaskDataException e) {
                // Unchecked exception — bad input or permission denied
                System.out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }
    }

    static void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks to display.");
            return;
        }
        System.out.println();
        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    // Converts non-numeric input into our domain exception (exception translation pattern)
    static int readInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new InvalidTaskDataException("Expected a number but got: \"" + input + "\"");
        }
    }

    static String readStr(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isBlank()) {
            throw new InvalidTaskDataException("This field cannot be empty.");
        }
        return input;
    }

    static LocalDate readDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            throw new InvalidTaskDataException("Date must be in yyyy-MM-dd format. Got: " + input);
        }
    }

    static void saveAll(UserService userService, TaskManager taskManager) throws IOException {
        userService.saveUsers(USERS_FILE);
        taskManager.saveTasks(TASKS_FILE);
    }
}
