package com.Admin;

public class Admin {
    public boolean isAuthenticate(String username, String password) {
        return "admin".equalsIgnoreCase(username) && "admin".equals(password);
    }
}
