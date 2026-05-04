package com.example.server;

import com.example.model.Config;
import com.example.model.Question;
import com.example.service.AuthService;
import com.example.service.GameService;
import com.example.service.ScoreService;
import com.example.util.FileLoader;
import com.example.util.PathResolver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class GameServer {

    public static void main(String[] args) throws Exception {
        System.out.println("Working dir : " + System.getProperty("user.dir"));
        System.out.println("Data dir    : " + PathResolver.getDataDir());

        Config config;
        try {
            config = FileLoader.loadConfig("config.txt");
        } catch (Exception e) {
            System.out.println("Could not load config, using defaults. " + e.getMessage());
            config = new Config();
        }

        List<Question> questions = FileLoader.loadQuestions("questions.txt");
        System.out.println("Loaded " + questions.size() + " questions.");


        AuthService  authService  = new AuthService("users.txt");
        ScoreService scoreService = new ScoreService("scores.txt");
        GameService  gameService  = new GameService(questions, config, scoreService);

        int port = config.getPort();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);
            System.out.println("Min players=" + config.getMinPlayers()
                    + ", Max=" + config.getMaxPlayers()
                    + ", Q duration=" + config.getQuestionDuration() + "s");
            System.out.println("Ready — waiting for clients...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[GameServer] Client connected: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, authService, scoreService, gameService);
                new Thread(handler).start();
            }
        }
    }
}
