# Multi-User Task Scheduler and Productivity Tracker

A menu-driven Core Java console application for managing tasks for multiple users, with role-based access, deadlines, priorities, thread-based task simulation, and file persistence.

## Table of Contents
1. Project Overview
2. Key Features
3. System Architecture
4. Package and Class Design
5. End-to-End Execution Flow
6. Data Storage and File Formats
7. Multithreading Design
8. Error Handling Strategy
9. Java Concepts Demonstrated (Syllabus Mapping)
10. How to Compile and Run (Plain Java)
11. How to Test the Project
12. Current Limitations and Future Enhancements

## 1) Project Overview
This project simulates a productivity tracker where multiple users can:
- Register and log in.
- Create tasks with deadlines and priorities.
- View, filter, update, complete, and delete tasks.
- Track completion history.
- Simulate task execution using multiple threads.

The application is intentionally modular so each Java concept is easy to explain in viva.

## 2) Key Features
- Multi-user support with login authentication.
- Admin and user role separation.
- Task lifecycle management (create, update, complete, delete).
- Priority and deadline based task handling.
- Thread-based task execution simulation.
- Persistent storage using file I/O streams.
- Custom checked and unchecked exceptions.
- Generic repository class for reusable storage patterns.
- Custom annotation for method execution-time logging.

## 3) System Architecture
The application follows a layered, package-oriented architecture.

### 3.1 Architecture Layers
- Presentation Layer:
  - `app/Main.java`
  - Handles all console menus, input, and command routing.
- Service Layer:
  - `service/UserService.java`
  - `service/TaskManager.java`
  - `service/Scheduler.java`
  - Contains business logic for users, tasks, and execution simulation.
- Domain/Model Layer:
  - `model/User.java`
  - `model/Admin.java`
  - `model/Task.java`
  - Represents core business entities.
- Utility/Infrastructure Layer:
  - `util/FileUtil.java`
  - `util/Repository.java`
  - `util/ExecutionTimeLogger.java`
  - `util/ExecutionTimeUtil.java`
  - Handles file persistence, reusable generics, and annotation-driven timing logs.
- Concurrency Layer:
  - `thread/TaskExecutionThread.java`
  - `thread/TaskExecutionRunnable.java`
  - Worker implementations used by scheduler.
- Exception Layer:
  - `exception/TaskNotFoundException.java`
  - `exception/InvalidTaskDataException.java`

### 3.2 High-Level Interaction
1. `Main` initializes services and loads data from files.
2. `UserService` manages registration, login, and admin seed data.
3. `TaskManager` performs all task operations and access checks.
4. `Scheduler` pulls pending tasks from `TaskManager` and executes them via threads.
5. `FileUtil` persists users and tasks on logout/exit.

## 4) Package and Class Design

```text
src/main/java/com/btech/productivitytracker/
|-- app/
|   `-- Main.java
|-- model/
|   |-- User.java
|   |-- Admin.java
|   `-- Task.java
|-- service/
|   |-- TaskOperations.java
|   |-- UserService.java
|   |-- TaskManager.java
|   `-- Scheduler.java
|-- thread/
|   |-- TaskExecutionThread.java
|   `-- TaskExecutionRunnable.java
|-- exception/
|   |-- TaskNotFoundException.java
|   `-- InvalidTaskDataException.java
`-- util/
    |-- FileUtil.java
    |-- Repository.java
    |-- ExecutionTimeLogger.java
    `-- ExecutionTimeUtil.java
```

### 4.1 Core Responsibilities
- `Main`:
  - Entry point.
  - Main menu and user session menu.
  - Input parsing and `try-catch` orchestration.
- `UserService`:
  - User registration and login.
  - User load/save operations.
  - Default admin seeding.
- `TaskManager`:
  - Implements `TaskOperations`.
  - Add/view/update/complete/delete tasks.
  - Access control checks.
  - Completion history and priority queue management.
- `Scheduler`:
  - Thread simulation of pending task execution.
  - Mixes `Thread` and `Runnable` creation strategies.
- `Task`:
  - Task entity with validation and comparison logic.
  - Serialization and deserialization helpers for file storage.
- `User` and `Admin`:
  - Role model with inheritance and overridden behavior.

## 5) End-to-End Execution Flow

### 5.1 Startup
1. `Main.main()` creates `UserService`, `TaskManager`, and `Scheduler`.
2. `initializeData()`:
   - Loads users from file.
   - Ensures default admin exists.
   - Loads tasks from file.

### 5.2 Main Menu
User can:
- Register
- Login
- Exit

### 5.3 After Login
Session menu allows:
- Add task
- View tasks
- Sort tasks by priority
- Filter tasks by deadline
- Update task priority/deadline
- Complete task
- Delete task
- View completion history
- Simulate threaded execution
- Save and logout

Admin additionally can:
- View all registered users
- View all tasks across users

### 5.4 Save and Exit
- On logout/exit, users and tasks are written back to files.

## 6) Data Storage and File Formats
Runtime files are created automatically under `data/`.

### 6.1 User Storage
- File: `data/users.dat`
- Stream style: Byte streams (`FileInputStream`, `FileOutputStream`)
- Record format:

```text
username|password|fullName|role
```

### 6.2 Task Storage
- File: `data/tasks.txt`
- Stream style: Character streams (`BufferedReader`, `FileWriter`)
- Record format:

```text
id|ownerUsername|priority|completed|deadline|createdAt|base64(title)|base64(description)
```

Base64 is used for title/description to safely handle separators and special characters.

## 7) Multithreading Design
- Thread execution is initiated from `Scheduler.simulateTaskExecution(User user)`.
- One worker thread is created per pending task.
- Alternate strategy is used intentionally:
  - Even index: `TaskExecutionThread extends Thread`
  - Odd index: `new Thread(new TaskExecutionRunnable(...))`
- Thread priority is mapped from task priority.
- `join()` is used to wait until all worker threads finish.
- Shared task operations are protected with `synchronized` methods in `TaskManager`.

## 8) Error Handling Strategy
- Checked exception:
  - `TaskNotFoundException`
  - Used when task ID is not found.
- Unchecked exception:
  - `InvalidTaskDataException`
  - Used for invalid inputs and access violations.
- `Main` catches and prints user-friendly error messages for both categories.

## 9) Java Concepts Demonstrated (Syllabus Mapping)

1. Object-Oriented Programming
- Classes: `Task`, `User`, `Admin`, `TaskManager`, `Scheduler`
- Constructors and encapsulation in model classes.
- Method overloading in `TaskManager` (`addTask`, `viewTasks`).
- Inheritance: `Admin extends User`.
- Runtime polymorphism: overridden `showRoleMessage()`.

2. Interfaces and Functional Interfaces
- Interface: `TaskOperations`.
- Lambdas for sorting/filtering in `TaskManager` streams.

3. Exception Handling
- `try-catch` in menu flow.
- Custom checked and unchecked exceptions.

4. Multithreading
- `Thread` and `Runnable` both demonstrated.
- Thread priority and synchronization included.

5. File Handling
- Byte streams and character streams both used.

6. Packages
- Structured by concern: model, service, exception, thread, util.

7. Collections Framework
- `ArrayList`, `HashMap`, `PriorityQueue`, `Stack`.

8. Generics
- Generic class: `Repository<T>`.

9. Annotation
- Custom `@ExecutionTimeLogger` annotation and runtime reflection usage.

## 10) How to Compile and Run (Plain Java)

### 10.1 Prerequisite
- JDK 17+ installed.

### 10.2 Compile
From project root:

```powershell
Set-Location "d:\JAVA PROJECT\Multi-User-Task-Scheduler-and-Productivity-Tracker"
if (Test-Path "out") { Remove-Item "out" -Recurse -Force }
$javaFiles = Get-ChildItem -Recurse -Path "src\main\java" -Filter "*.java" | ForEach-Object { $_.FullName }
javac -d out $javaFiles
```

### 10.3 Run

```powershell
java -cp out com.btech.productivitytracker.app.Main
```

Default admin credentials:
- Username: `admin`
- Password: `admin123`

## 11) How to Test the Project

### 11.1 Manual Functional Test Checklist
1. Register a new user.
2. Login as that user.
3. Add multiple tasks with different priorities and deadlines.
4. View and sort tasks.
5. Update one task's priority/deadline.
6. Complete a task manually.
7. Run thread simulation and verify pending tasks become completed.
8. View completed task history.
9. Login as admin and verify all-users/all-tasks visibility.
10. Test invalid inputs (wrong date format, non-numeric menu input, invalid task ID).

### 11.2 Fast Smoke-Test Input Example
You can pipe scripted input to quickly verify basic flow.

```powershell
$input = @"
1
alice
pass123
Alice Doe
2
alice
pass123
1
Task One
Prepare notes
2026-12-31
8
2
9
8
10
3
"@
$input | java -cp out com.btech.productivitytracker.app.Main
```

## 12) Current Limitations and Future Enhancements

### Current Limitations
- Passwords are stored in plain text (not secure for production).
- File-based persistence only (no database).
- No unit/integration test framework yet.
- Console UI only.

### Future Enhancements
- Add password hashing and stronger authentication.
- Replace file persistence with a relational database.
- Add JUnit test suite.
- Add search, pagination, and reminder notifications.
- Build REST API or GUI layer on top of current services.

---
This README is designed for both project understanding and viva presentation. It maps implementation details directly to syllabus concepts so each section can be explained confidently.
