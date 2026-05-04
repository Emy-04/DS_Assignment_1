package com.example.model;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L; // versioning

    private String category;
    private String text;
    private List<String> choices;
    private char correctAnswer;
    private String difficulty;

    public Question(String category, String text, List<String> choices, char correctAnswer, String difficulty) {
        this.category = category;
        this.text = text;
        this.choices = choices;
        this.correctAnswer = Character.toUpperCase(correctAnswer);
        this.difficulty = difficulty.toUpperCase();
    }

    public String getCategory() { return category; }
    public String getText() { return text; }
    public List<String> getChoices() { return choices; }
    public char getCorrectAnswer() { return correctAnswer; }
    public String getDifficulty() { return difficulty; }

    public boolean checkAnswer(String answer) {
        if(answer == null || answer.isEmpty()) return false;
        return Character.toUpperCase(answer.charAt(0)) == correctAnswer;
    }
}