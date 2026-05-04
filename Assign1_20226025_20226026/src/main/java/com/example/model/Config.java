package com.example.model;

public class Config {

    private int port            = 5555;
    private int questionDuration = 30;
    private int minPlayers       = 1;
    private int maxPlayers       = 4;

    public int getPort()             { return port; }
    public int getQuestionDuration() { return questionDuration; }
    public int getMinPlayers()       { return minPlayers; }
    public int getMaxPlayers()       { return maxPlayers; }

    public void setPort(int port)                         { this.port = port; }
    public void setQuestionDuration(int questionDuration) { this.questionDuration = questionDuration; }
    public void setMinPlayers(int minPlayers)             { this.minPlayers = minPlayers; }
    public void setMaxPlayers(int maxPlayers)             { this.maxPlayers = maxPlayers; }
}