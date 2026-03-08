# Multi-User Task Scheduler and Productivity Tracker

A menu-driven Core Java console application for managing tasks across multiple users, with role-based access, deadlines, priorities, thread-based task simulation, and file persistence - all written in a **single Java file** for clarity.

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Key Features](#2-key-features)
3. [Project Structure](#3-project-structure)
4. [All Java Concepts Demonstrated](#4-all-java-concepts-demonstrated)
5. [Class Summary (all in Main.java)](#5-class-summary-all-in-mainjava)
6. [Data Storage](#6-data-storage)
7. [Multithreading Design](#7-multithreading-design)
8. [Error Handling](#8-error-handling)
9. [How to Compile and Run](#9-how-to-compile-and-run)
10. [Default Login](#10-default-login)

---

## 1) Project Overview

This project simulates a productivity tracker where multiple users can:
- Register and log in (with role-based access: User / Admin).
- Create tasks with a title, deadline, and priority (1-10).
- View, filter, update, complete, and delete tasks.
- Track a completion history (Stack-based LIFO).
- Simulate concurrent task execution using multiple threads.
- Persist all data to plain files on disk.

> **All 16 classes live inside a single file: `Main.java`**
> This keeps the code easy to read, navigate, and explain in a viva.

---

## 2) Key Features

- Multi-user support with login authentication.
- Role-based access control (User and Admin).
- Task creation with title, deadline, and priority.
- View, filter, sort, update, complete, and delete tasks.
- Completion history using a Stack (LIFO).
- Concurrent task execution simulation using Threads.
- File persistence (users and tasks saved to disk automatically).
- Default admin account seeded on first run.

---

## 3) Project Structure

```
src/main/java/com/btech/productivitytracker/app/
+-- Main.java          <- entire project lives here (16 classes, ~600 lines)

data/
+-- users.dat          <- auto-created, stores registered users (byte stream)
+-- tasks.txt          <- auto-created, stores tasks (character stream)
```

### All 16 classes inside Main.java (in order)

| # | Class / Type | Role |
|---|---|---|
| 1 | `InvalidTaskDataException` | Unchecked exception (`extends RuntimeException`) |
| 2 | `TaskNotFoundException` | Checked exception (`extends Exception`) |
| 3 | `@ExecutionTimeLogger` | Custom annotation (`@Retention`, `@Target`) |
| 4 | `User` | Base class - encapsulation + polymorphism base |
| 5 | `Admin extends User` | Subclass - inheritance + method overriding |
| 6 | `Task implements Comparable<Task>` | Domain model - constructor overloading, serialization |
| 7 | `Repository<T>` | Generic key-value store - generics + Optional |
| 8 | `TaskOperations` | Interface - abstraction/contract |
| 9 | `TaskManager implements TaskOperations` | Core logic - synchronized, streams, lambdas |
| 10 | `UserService` | User registration and login |
| 11 | `TaskExecutionThread extends Thread` | Threading way 1 |
| 12 | `TaskExecutionRunnable implements Runnable` | Threading way 2 |
| 13 | `Scheduler` | Thread orchestration (start + join) |
| 14 | `ExecutionTimeUtil` | Java Reflection utility |
| 15 | `FileUtil` | File I/O - byte streams + character streams |
| 16 | `public class Main` | Entry point - menus, input, exception handling |

> Java rule: a `.java` file can contain multiple classes - only ONE may be `public`,
> and the filename must match that class. All others here are package-private.

---

## 4) All Java Concepts Demonstrated

| Concept | Where |
|---|---|
| Encapsulation | `User`, `Task` - private fields with getters/setters |
| Inheritance | `Admin extends User` |
| Method Overriding + `@Override` | `Admin` overrides `getRole()`, `canManageAllTasks()`, `showRoleMessage()` |
| Runtime Polymorphism | `loggedInUser.showRoleMessage()` - Admin or User version chosen at runtime |
| Constructor Overloading | `Task` has a short 5-arg and a full 8-arg constructor |
| Method Overloading | `TaskManager.addTask(Task)` and `addTask(String, LocalDate, int, User)` |
| Interface | `TaskOperations` implemented by `TaskManager` |
| Checked Exception | `TaskNotFoundException extends Exception` |
| Unchecked Exception | `InvalidTaskDataException extends RuntimeException` |
| Custom Annotation | `@ExecutionTimeLogger` with `@Retention(RUNTIME)` and `@Target(METHOD)` |
| Java Reflection | `ExecutionTimeUtil` finds and invokes method by name, reads annotation |
| Generics | `Repository<T>` - used as both `Repository<Task>` and `Repository<User>` |
| `Optional<T>` | `Repository.findByKey()` - avoids null, forces explicit absent-value handling |
| `ArrayList` | `TaskManager.allTasks` - ordered, general-purpose task list |
| `PriorityQueue` | `TaskManager.priorityQueue` - auto-sorted by `Task.compareTo()` |
| `Stack` | `TaskManager.history` - LIFO completion log |
| `HashMap` | `UserService.users` - O(1) username lookup |
| `Comparable<T>` | `Task.compareTo()` - higher priority first, earlier deadline breaks ties |
| Lambda expressions | `.filter(task -> ...)`, `.sorted((t1, t2) -> ...)` |
| Streams API | `.stream().filter().sorted().collect()` in `TaskManager` |
| `synchronized` | All `TaskManager` methods - prevents race conditions |
| `extends Thread` | `TaskExecutionThread` - threading way 1 |
| `implements Runnable` | `TaskExecutionRunnable` - threading way 2 (preferred) |
| `thread.start()` | Launches a new OS thread (does NOT call `run()` directly) |
| `thread.join()` | Main thread waits until worker thread finishes |
| Thread priority | `worker.setPriority(...)` mapped from task priority |
| Byte streams | `FileInputStream` / `FileOutputStream` for `users.dat` |
| Character streams | `BufferedReader` / `FileWriter` for `tasks.txt` |
| `try-with-resources` | All streams auto-closed on block exit |
| `Base64` encoding | Task titles encoded to safely handle special characters in files |
| `instanceof` pattern match | `if (currentUser instanceof Admin admin)` (Java 16+) |
| `LocalDate` / `LocalDateTime` | Task deadline and creation timestamp |

---

## 5) Class Summary (all in Main.java)

### `User` - Base Class
- Private fields: `username`, `password`, `fullName`
- `authenticate(String)` - checks password
- `getRole()`, `canManageAllTasks()`, `showRoleMessage()` - designed to be overridden

### `Admin extends User` - Subclass
- Overrides all three polymorphic methods
- Extra: `printAllUsers(Collection<User>)` - admin-only feature

### `Task implements Comparable<Task>`
- Fields: `id`, `title`, `deadline`, `priority`, `completed`, `ownerUsername`, `createdAt`
- `compareTo()`: higher priority first, earlier deadline breaks ties - used by `PriorityQueue`
- `toDataLine()` / `fromDataLine()`: serialise/deserialise for file storage (Base64-encodes title)

### `Repository<T>` - Generic Store
- Wraps `HashMap<String, T>` for O(1) lookup
- Returns `Optional<T>` from `findByKey()` - no null returns

### `TaskOperations` - Interface
- Contract: `addTask`, `deleteTask`, `completeTask`, `viewTasks`
- `deleteTask` and `completeTask` declare `throws TaskNotFoundException`

### `TaskManager implements TaskOperations`
- All public methods are `synchronized` - thread-safe
- Uses `ArrayList`, `PriorityQueue`, `Stack`, and `Repository` simultaneously
- `getTasksSortedByPriority()` and `filterTasksByDeadline()` use Stream pipelines with lambdas

### `UserService`
- `registerUser()` overloaded (2-arg and 3-arg versions)
- `seedDefaultAdmin()` - ensures `admin/admin123` always exists

### `TaskExecutionThread extends Thread`
- Names thread via `super("Worker-Thread-<id>")` in constructor
- Sleep: `max(500, 1600 - priority x 100)` ms - higher priority finishes faster

### `TaskExecutionRunnable implements Runnable`
- Preferred approach - does not consume the class's single inheritance slot
- Wrapped in `new Thread(runnable, name)` by `Scheduler`

### `Scheduler`
- Alternates between `TaskExecutionThread` (even index) and `TaskExecutionRunnable` (odd index)
- Calls `worker.start()` for every thread, then `worker.join()` on all of them

### `ExecutionTimeUtil` - Reflection
- `invokeWithTiming(target, "methodName", paramTypes, args)`
- Uses `Method.invoke()` to call method dynamically at runtime
- Reads `@ExecutionTimeLogger` annotation value to label the timer output

### `FileUtil` - File I/O
- **Byte streams** (`FileInputStream`/`FileOutputStream`) for `users.dat`
- **Character streams** (`BufferedReader`/`FileWriter`) for `tasks.txt`
- `ensureFileExists()` creates missing directories and files automatically

---

## 6) Data Storage

### `data/users.dat` - pipe-delimited, written as bytes
```
username|password|fullName|role
admin|admin123|System Admin|ADMIN
alice|pass123|Alice Doe|USER
```

### `data/tasks.txt` - pipe-delimited, written as characters
```
id|ownerUsername|priority|completed|deadline|createdAt|base64(title)
1|alice|8|false|2026-12-31|2026-03-08T10:00:00|VGFzayBPbmU=
```

Base64 encoding on the title prevents pipe characters or newlines inside task names from breaking the parser.

---

## 7) Multithreading Design

```
Scheduler.simulateTaskExecution(user)
   |
   +-- even index --> new TaskExecutionThread(...)       [extends Thread]
   +-- odd index  --> new Thread(TaskExecutionRunnable)  [implements Runnable]
   |
   +-- worker.setPriority(...)   <- mapped from task priority (1-10)
   +-- worker.start()            <- launches new OS thread asynchronously
   +-- worker.join()             <- main thread waits until all workers finish
```

All `TaskManager` methods are `synchronized` so concurrent `completeTask()` calls from multiple threads are safe.

---

## 8) Error Handling

| Type | Class | Used for |
|---|---|---|
| Checked | `TaskNotFoundException extends Exception` | Task ID not found |
| Unchecked | `InvalidTaskDataException extends RuntimeException` | Bad input, blank fields, access denied |

`Main` catches both at the menu level and prints a readable message instead of crashing.

---

## 9) How to Compile and Run

### Prerequisites
- JDK 17 or higher

### Compile
```powershell
cd "d:\JAVA PROJECT\Multi-User-Task-Scheduler-and-Productivity-Tracker"
javac -d target/classes src/main/java/com/btech/productivitytracker/app/Main.java
```

### Run
```powershell
java -cp target/classes com.btech.productivitytracker.app.Main
```

---

## 10) Default Login

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |

A new user account can be created from option 1 on the main menu.

---

*All 16 Java classes are in one file (`Main.java`) so every concept is in one place - easy to find, read, and explain.*