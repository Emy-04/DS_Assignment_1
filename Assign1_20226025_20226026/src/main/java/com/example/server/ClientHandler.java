package com.example.server;

import com.example.model.User;
import com.example.service.AuthService;
import com.example.service.GameService;
import com.example.service.IGameService;
import com.example.service.IScoreService;
import com.example.service.ScoreService;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuthService authService;
    private final IScoreService scoreService;
    private final IGameService gameService;

    private User user;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, AuthService authService,
                         IScoreService scoreService, IGameService gameService) {
        this.socket       = socket;
        this.authService  = authService;
        this.scoreService = scoreService;
        this.gameService  = gameService;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        try {
            while (user == null) {
                if (!showAuthMenu()) return;
            }
            GameService.registerHandler(user.getUsername(), this);
            while (running) showMainMenu();
        } catch (IOException e) {
            System.out.println("Client disconnected: "
                    + (user != null ? user.getUsername() : socket.getRemoteSocketAddress()));
        } finally {
            if (user != null) GameService.unregisterHandler(user.getUsername());
            closeConnection();
        }
    }



    private boolean showAuthMenu() throws IOException {
        sendMessage("   please choose one of the following to start: ");
        sendMessage("1) Register");
        sendMessage("2) Login");
        sendMessage("Type '-' to quit");
        sendMessage("Choose:");
        sendInput();

        String choice = reader.readLine();
        if (choice == null || choice.trim().equals("-")) {
            sendMessage("Client Quits!");
            return false;
        }
        switch (choice.trim()) {
            case "1": return doRegister();
            case "2": return doLogin();
            default:
                sendMessage("Invalid choice. Please enter 1 or 2.");
                return true;
        }
    }

    private boolean doRegister() throws IOException {
        sendMessage("\n Register ");
        sendMessage("\n");
        sendMessage("Full Name:");
        sendInput();
        String name = reader.readLine();
        if (name == null || name.trim().equals("-")) return false;

        sendMessage("Username:");
        sendInput();
        String username = reader.readLine();
        if (username == null || username.trim().equals("-")) return false;

        sendMessage("Password:");
        sendInput();
        String password = reader.readLine();
        if (password == null || password.trim().equals("-")) return false;

        try {
            authService.register(name.trim(), username.trim(), password.trim());
            sendMessage("Registration successful! Please login now.");
            return true;
        } catch (Exception e) {
            sendMessage("ERROR: " + e.getMessage());
            sendMessage("Please try again.");
            return true;
        }
    }

    private boolean doLogin() throws IOException {
        sendMessage("\n Login ");
        sendMessage("\n");
        sendMessage("Username:");
        sendInput();
        String username = reader.readLine();
        if (username == null || username.trim().equals("-")) return false;

        sendMessage("Password:");
        sendInput();
        String password = reader.readLine();
        if (password == null || password.trim().equals("-")) return false;

        try {
            user = authService.login(username.trim(), password.trim());
            sendMessage("Login successful! Welcome, " + user.getName() + "!");
            showScoreHistory();
            return true;
        } catch (Exception e) {
            sendMessage("ERROR: " + e.getMessage());
            sendMessage("Please try again.");
            return true;
        }
    }

    private void showScoreHistory() {
        if (scoreService instanceof ScoreService) {
            var history = ((ScoreService) scoreService).getHistory(user.getUsername());
            if (!history.isEmpty()) {
                sendMessage("Your last scores: " + history);
                sendMessage("Total accumulated: " + scoreService.getTotalScore(user));
            }
        }
    }


    private void showMainMenu() throws IOException {
        sendMessage("\n GAme Menu:");
        sendMessage("1) Single Player");
        sendMessage("2) Multiplayer (Team vs Team)");
        sendMessage("Type '-' to quit");
        sendMessage("Choose:");
        sendInput();

        String choice = reader.readLine();
        if (choice == null || choice.trim().equals("-")) {
            running = false;
            sendMessage("Client Quits!");
            return;
        }
        switch (choice.trim()) {
            case "1": gameService.singlePlayerGame(this); break;
            case "2": showMultiplayerMenu();               break;
            default:  sendMessage("Invalid choice. Please enter 1 or 2.");
        }
    }

    private void showMultiplayerMenu() throws IOException {
        sendMessage("\n MULTIPLAYER Mode Menu: ");
        sendMessage("1) Create a new game (set both team names and members)");
        sendMessage("2) Join an existing game");
        sendMessage("Type '-' to go back");
        sendMessage("Choose:");
        sendInput();

        String choice = reader.readLine();
        if (choice == null || choice.trim().equals("-")) return;

        switch (choice.trim()) {
            case "1": gameService.createTeam(this); break;
            case "2": gameService.joinTeam(this);   break;
            default:  sendMessage("Invalid choice.");
        }
    }


    public void sendInput() { writer.println(">>INPUT<<"); }

    public void sendMessage(String message) {
        if (writer != null && !socket.isClosed()) writer.println(message);
    }

    public String getUsername()        { return user != null ? user.getUsername() : null; }
    public User getUser()              { return user; }
    public BufferedReader getReader()  { return reader; }
    public Socket getSocket()          { return socket; }

    private void closeConnection() {
        running = false;
        try { if (!socket.isClosed()) socket.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }
}