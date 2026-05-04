package com.example.service;

import com.example.server.ClientHandler;

public interface IGameService {
     void singlePlayerGame(ClientHandler client);
     void createTeam(ClientHandler client);
     void joinTeam(ClientHandler client);
     void startTeamGame();
     void sendQuestion();
     void checkAnswer(ClientHandler client, String answer);
     void startQuestionTimer();
     void endGame();
}
