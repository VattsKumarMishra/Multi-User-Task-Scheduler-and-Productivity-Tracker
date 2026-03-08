package com.btech.productivitytracker.service;

import com.btech.productivitytracker.exception.InvalidTaskDataException;
import com.btech.productivitytracker.model.Admin;
import com.btech.productivitytracker.model.User;
import com.btech.productivitytracker.util.FileUtil;
import com.btech.productivitytracker.util.Repository;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final HashMap<String, User> users = new HashMap<>();
    private final Repository<User> userRepository = new Repository<>();

    public void loadUsers(String usersFile) throws IOException {
        Map<String, User> loadedUsers = FileUtil.loadUsersWithByteStream(usersFile);
        users.clear();
        userRepository.asMap().clear();

        loadedUsers.forEach((username, user) -> {
            users.put(username, user);
            userRepository.save(username, user);
        });
    }

    public void saveUsers(String usersFile) throws IOException {
        FileUtil.saveUsersWithByteStream(users, usersFile);
    }

    public boolean registerUser(String username, String password) {
        return registerUser(username, password, username);
    }

    public boolean registerUser(String username, String password, String fullName) {
        validateUserInput(username, password, fullName);
        if (users.containsKey(username)) {
            return false;
        }

        User user = new User(username, password, fullName);
        users.put(username, user);
        userRepository.save(username, user);
        return true;
    }

    public void seedDefaultAdmin() {
        if (!users.containsKey("admin")) {
            Admin admin = new Admin("admin", "admin123", "System Admin");
            users.put(admin.getUsername(), admin);
            userRepository.save(admin.getUsername(), admin);
        }
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(password)) {
            return user;
        }
        return null;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public HashMap<String, User> getUsersMap() {
        return users;
    }

    private void validateUserInput(String username, String password, String fullName) {
        if (username == null || username.isBlank()) {
            throw new InvalidTaskDataException("Username cannot be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidTaskDataException("Password cannot be blank.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidTaskDataException("Full name cannot be blank.");
        }
    }
}
