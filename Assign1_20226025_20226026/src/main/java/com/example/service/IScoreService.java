package com.example.service;

import com.example.model.User;

public interface IScoreService {
    void addScore(User user, int score);
    int getTotalScore(User user);
}
