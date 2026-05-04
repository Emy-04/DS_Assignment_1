package com.example.service;

import com.example.model.User;
import com.example.util.FileLoader;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthService implements IAuthService {

    private final Map<String, User> users;
    private final String userFile;

    public AuthService(String userFile) {
        this.userFile = userFile;
        Map<String, User> loaded;
        try { loaded = FileLoader.loadUsers(userFile); }
        catch (IOException e) { loaded = new LinkedHashMap<>(); }
        this.users = loaded;
    }

    @Override
    public synchronized User login(String username, String password) throws Exception {
        if (!users.containsKey(username)) throw new Exception("404 User Not Found");
        User u = users.get(username);
        if (!u.getPassword().equals(password)) throw new Exception("401 Unauthorized");
        return u;
    }

    @Override
    public synchronized User register(String name, String username, String password) throws Exception {
        if (users.containsKey(username)) throw new Exception("Username Already Exists");
        User u = new User(name, username, password);
        users.put(username, u);
        try { FileLoader.saveUsers(userFile, users.values()); }
        catch (IOException e) { e.printStackTrace(); }
        return u;
    }
}