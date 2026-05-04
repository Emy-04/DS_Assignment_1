package com.example.client;

import com.example.model.Config;
import com.example.util.FileLoader;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    private static final BlockingQueue<String> inputRequests = new LinkedBlockingQueue<>();
    private static volatile boolean connected = true;

    public static void main(String[] args) {
        String host = "localhost";
        int port;
        try {
            Config config = FileLoader.loadConfig("config.txt");
            port = config.getPort();
        } catch (Exception e) {
            System.out.println("Could not load config.txt, using default port 5555.");
            port = 5555;
        }
        System.out.println("Connecting to server...");

        try (Socket socket = new Socket(host, port)) {

            BufferedReader serverIn  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            Scanner        userIn    = new Scanner(System.in);

            Thread serverReader = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        if (line.equals(">>INPUT<<")) {
                            inputRequests.put("GO");
                        } else if (line.equals("BYE")) {
                            System.out.println("Goodbye!");
                            connected = false;
                            inputRequests.put("STOP");
                            break;
                        } else {
                            System.out.println(line);
                        }
                    }
                } catch (Exception e) {
                    if (connected) System.out.println("Disconnected from server");
                } finally {
                    connected = false;
                    try { inputRequests.put("STOP"); } catch (InterruptedException ignored) {}
                }
            });
            serverReader.setDaemon(true);
            serverReader.start();

            while (connected) {
                String signal = inputRequests.take();
                if (signal.equals("STOP") || !connected) break;

                if (userIn.hasNextLine()) {
                    String input = userIn.nextLine().trim();
                    serverOut.println(input);
                }
            }

            System.out.println("Disconnected.");

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}