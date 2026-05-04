package com.example.util;

import com.example.model.Config;
import com.example.model.Question;
import com.example.model.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileLoader {



    public static List<Question> loadQuestions(String filename) throws IOException {
        String path = PathResolver.resolve(filename);
        System.out.println("Loading questions from: " + path);
        List<Question> questions = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|");
            if (parts.length != 8) continue;
            String category   = parts[0].trim();
            String text       = parts[1].trim();
            List<String> choices = Arrays.asList(
                    parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim());
            char correct      = parts[6].trim().charAt(0);
            String difficulty = parts[7].trim();
            questions.add(new Question(category, text, choices, correct, difficulty));
        }
        return questions;
    }

    // Format: name:username:password

    public static Map<String, User> loadUsers(String filename) throws IOException {
        String path = PathResolver.resolve(filename);
        Map<String, User> users = new LinkedHashMap<>();
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("users.txt not found — will be created on first registration.");
            return users;
        }
        System.out.println("Loading users from: " + path);
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(":", 3);
            if (parts.length < 3) continue;
            User u = new User(parts[0].trim(), parts[1].trim(), parts[2].trim());
            users.put(u.getUsername(), u);
        }
        return users;
    }

    public static void saveUsers(String filename, Collection<User> users) throws IOException {
        String path = PathResolver.resolve(filename);
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (User u : users) {
                bw.write(u.getName() + ":" + u.getUsername() + ":" + u.getPassword());
                bw.newLine();
            }
        }
    }


    // Format: username:score1:score2:...  (last 10 kept)

    public static Map<String, List<Integer>> loadScores(String filename) throws IOException {
        String path = PathResolver.resolve(filename);
        Map<String, List<Integer>> scores = new LinkedHashMap<>();
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("scores file not found — will be created after first game.");
            return scores;
        }
        System.out.println("Loading scores from: " + path);
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(":");
            if (parts.length < 1) continue;
            String username = parts[0].trim();
            List<Integer> userScores = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                try { userScores.add(Integer.parseInt(parts[i].trim())); }
                catch (NumberFormatException ignored) {}
            }
            scores.put(username, userScores);
        }
        return scores;
    }

    public static void saveScores(String filename, Map<String, List<Integer>> scores) throws IOException {
        String path = PathResolver.resolve(filename);
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (Map.Entry<String, List<Integer>> entry : scores.entrySet()) {
                StringBuilder sb = new StringBuilder(entry.getKey());
                for (int s : entry.getValue()) sb.append(":").append(s);
                bw.write(sb.toString());
                bw.newLine();
            }
        }
    }


    public static void addScore(String username, int score, Map<String, List<Integer>> scores) {
        scores.putIfAbsent(username, new ArrayList<>());
        List<Integer> list = scores.get(username);
        list.add(score);
        if (list.size() > 10) list.remove(0);
    }



    public static Config loadConfig(String filename) throws IOException {
        String path = PathResolver.resolve(filename);
        System.out.println("Loading config from: " + path);
        Config cfg = new Config();
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("config.txt not found, using defaults.");
            return cfg;
        }
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] kv = line.split("=", 2);
            if (kv.length < 2) continue;
            String key = kv[0].trim().toUpperCase();
            String val = kv[1].trim();
            try {
                switch (key) {
                    case "PORT":              cfg.setPort(Integer.parseInt(val)); break;
                    case "QUESTION_DURATION": cfg.setQuestionDuration(Integer.parseInt(val)); break;
                    case "MIN_PLAYERS":       cfg.setMinPlayers(Integer.parseInt(val)); break;
                    case "MAX_PLAYERS":       cfg.setMaxPlayers(Integer.parseInt(val)); break;
                }
            } catch (NumberFormatException ignored) {}
        }
        return cfg;
    }
}