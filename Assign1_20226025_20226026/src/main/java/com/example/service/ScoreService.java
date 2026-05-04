package com.example.service;

import com.example.model.User;
import com.example.util.FileLoader;

import java.io.IOException;
import java.util.*;

public class ScoreService implements IScoreService {

    private final Map<String, List<Integer>> scores;
    private final String scoreFile;

    public ScoreService(String scoreFile) {
        this.scoreFile = scoreFile;
        Map<String, List<Integer>> loaded;
        try { loaded = FileLoader.loadScores(scoreFile); }
        catch (IOException e) { loaded = new LinkedHashMap<>(); }
        this.scores = loaded;
    }

    @Override
    public synchronized void addScore(User user, int score) {
        FileLoader.addScore(user.getUsername(), score, scores);
        try { FileLoader.saveScores(scoreFile, scores); }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public synchronized int getTotalScore(User user) {
        return scores.getOrDefault(user.getUsername(), Collections.emptyList())
                .stream().mapToInt(Integer::intValue).sum();
    }

    public synchronized List<Integer> getHistory(String username) {
        return new ArrayList<>(scores.getOrDefault(username, Collections.emptyList()));
    }
}