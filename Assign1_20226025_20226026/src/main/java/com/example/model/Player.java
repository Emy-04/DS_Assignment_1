package com.example.model;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Player {

    private final String username;
    private final Socket socket;
    private int currentScore = 0;
    private boolean answered = false;
    private final Map<String, Boolean> gameHistory = new HashMap<>();

    public Player(String username, Socket socket) {
        this.username = username;
        this.socket   = socket;
    }

    public String getUsername()                    { return username; }
    public int getCurrentScore()                   { return currentScore; }
    public Map<String, Boolean> getGameHistory()   { return gameHistory; }

    public void addScore(int score)                { currentScore += score; }

    public void addHistory(String questionText, boolean correct) {
        gameHistory.put(questionText, correct);
    }

    public void resetForNextQuestion() { answered = false; }

    public void resetForNewGame() {
        currentScore = 0;
        answered = false;
        gameHistory.clear();
    }
}
