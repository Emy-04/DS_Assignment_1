package com.example.server;

import com.example.model.Question;
import com.example.util.FileLoader;
import com.example.util.PathResolver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class QuestionServer {

    public static void main(String[] args) {
        int port = 6666;


        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println("Data dir resolved: " + PathResolver.getDataDir());

        List<Question> questions;
        try {
            questions = FileLoader.loadQuestions("questions.txt");
            System.out.println("Loaded " + questions.size() + " questions.");
        } catch (Exception e) {
            System.out.println("Failed to load questions: " + e.getMessage());
            System.out.println("Make sure the 'data' folder is inside your project root.");
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Running on port " + port);
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleRequest(client, questions)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket socket, List<Question> questions) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            String request = (String) in.readObject();
            if ("GET_ALL_QUESTIONS".equals(request)) {
                out.writeObject(questions);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
