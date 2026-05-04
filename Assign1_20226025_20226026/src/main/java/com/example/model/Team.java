package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private final String teamName;
    private final List<Player> players = new ArrayList<>();
    private int teamScore = 0;

    private String category;
    private String difficulty;
    private int numQuestions;
    private boolean gameStarted = false;
    private String gameId;

    public Team(String teamName) { this.teamName = teamName; }

    public String getTeamName()           { return teamName; }
    public List<Player> getPlayers()      { return players; }
    public int getTeamScore()             { return teamScore; }
    public String getCategory()           { return category; }
    public String getDifficulty()         { return difficulty; }
    public int getNumQuestions()          { return numQuestions; }
    public boolean isGameStarted()        { return gameStarted; }

    public void setCategory(String c)     { this.category = c; }
    public void setDifficulty(String d)   { this.difficulty = d; }
    public void setNumQuestions(int n)    { this.numQuestions = n; }
    public void setGameStarted(boolean b) { this.gameStarted = b; }
    public void setGameId(String id)      { this.gameId = id; }

    public synchronized void addPlayer(Player p)  { players.add(p); }
    public synchronized void addTeamScore(int s)  { teamScore += s; }

}