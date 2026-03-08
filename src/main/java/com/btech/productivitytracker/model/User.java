package com.btech.productivitytracker.model;

public class User {
    private String username;
    private String password;
    private String fullName;

    public User(String username, String password) {
        this(username, password, username);
    }

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean authenticate(String inputPassword) {
        return password.equals(inputPassword);
    }

    public String getRole() {
        return "USER";
    }

    public boolean canManageAllTasks() {
        return false;
    }

    // Overridden in Admin to demonstrate runtime polymorphism.
    public void showRoleMessage() {
        System.out.println("Welcome " + fullName + ". You are logged in as a standard user.");
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + getRole() + '\'' +
                '}';
    }
}
