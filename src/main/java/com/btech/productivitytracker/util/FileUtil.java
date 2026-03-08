package com.btech.productivitytracker.util;

import com.btech.productivitytracker.model.Admin;
import com.btech.productivitytracker.model.Task;
import com.btech.productivitytracker.model.User;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileUtil {
    private FileUtil() {
    }

    public static void ensureFileExists(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (path.getParent() != null && Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
    }

    // Byte streams are used here as requested (FileInputStream/FileOutputStream).
    public static Map<String, User> loadUsersWithByteStream(String usersFile) throws IOException {
        ensureFileExists(usersFile);
        Map<String, User> users = new HashMap<>();

        try (FileInputStream inputStream = new FileInputStream(usersFile)) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                return users;
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\\R");

            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) {
                    continue;
                }

                String username = parts[0];
                String password = parts[1];
                String fullName = parts[2];
                String role = parts[3];

                User user = "ADMIN".equalsIgnoreCase(role)
                        ? new Admin(username, password, fullName)
                        : new User(username, password, fullName);
                users.put(username, user);
            }
        }

        return users;
    }

    public static void saveUsersWithByteStream(Map<String, User> users, String usersFile) throws IOException {
        ensureFileExists(usersFile);

        StringBuilder builder = new StringBuilder();
        for (User user : users.values()) {
            builder.append(user.getUsername()).append('|')
                    .append(user.getPassword()).append('|')
                    .append(user.getFullName()).append('|')
                    .append(user.getRole())
                    .append(System.lineSeparator());
        }

        try (FileOutputStream outputStream = new FileOutputStream(usersFile, false)) {
            outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    // Character streams are used here as requested (BufferedReader/FileWriter).
    public static List<Task> loadTasksWithCharStream(String tasksFile) throws IOException {
        ensureFileExists(tasksFile);
        List<Task> tasks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(tasksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                tasks.add(Task.fromDataLine(line));
            }
        }

        return tasks;
    }

    public static void saveTasksWithCharStream(List<Task> tasks, String tasksFile) throws IOException {
        ensureFileExists(tasksFile);

        try (FileWriter writer = new FileWriter(tasksFile, false)) {
            for (Task task : tasks) {
                writer.write(task.toDataLine());
                writer.write(System.lineSeparator());
            }
        }
    }
}
