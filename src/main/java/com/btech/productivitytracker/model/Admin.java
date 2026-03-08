package com.btech.productivitytracker.model;

import java.util.Collection;

public class Admin extends User {
    public Admin(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean canManageAllTasks() {
        return true;
    }

    @Override
    public void showRoleMessage() {
        System.out.println("Welcome " + getFullName() + ". Admin privileges enabled.");
    }

    public void printRegisteredUsers(Collection<User> users) {
        System.out.println("\nRegistered Users:");
        users.forEach(System.out::println);
    }
}
