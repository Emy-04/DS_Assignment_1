package com.example;

import com.example.client.Client;
import com.example.server.GameServer;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java Main [server|client]");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "server":
                GameServer.main(new String[]{});
                break;
            case "client":
                Client.main(new String[]{});
                break;
            default:
                System.out.println("Unknown argument: " + args[0]);
        }
    }
}