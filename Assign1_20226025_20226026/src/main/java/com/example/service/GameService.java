package com.example.service;

import com.example.model.Config;
import com.example.model.Question;
import com.example.model.User;
import com.example.model.Player;
import com.example.model.Team;
import com.example.server.ClientHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GameService implements IGameService {

    private final List<Question> allQuestions;
    private final Config config;
    private final IScoreService scoreService;


    private static final Map<String, ClientHandler> handlerRegistry = new ConcurrentHashMap<>();

    public static void registerHandler(String username, ClientHandler handler) {
        handlerRegistry.put(username, handler);
    }

    public static void unregisterHandler(String username) {
        handlerRegistry.remove(username);
    }


    private static final Map<String, Team[]> pendingGames = new ConcurrentHashMap<>();
    private static int gameCounter = 0;

    public GameService(List<Question> allQuestions, Config config, IScoreService scoreService) {
        this.allQuestions = allQuestions;
        this.config       = config;
        this.scoreService = scoreService;
    }


    @Override
    public void singlePlayerGame(ClientHandler client) {
        client.sendMessage("\n SINGLE PLAYER GAME ");
        client.sendMessage("\n");
        String category = chooseCategory(client);
        if (category == null) return;

        String difficulty = chooseDifficulty(client);
        if (difficulty == null) return;

        int numQ = chooseNumQuestions(client, category, difficulty);
        if (numQ <= 0) return;

        List<Question> gameQuestions = filterQuestions(category, difficulty, numQ);
        if (gameQuestions.isEmpty()) {
            client.sendMessage("No questions found. Returning to menu.");
            return;
        }

        Player p = new Player(client.getUsername(), client.getSocket());

        client.sendMessage("\nStarting! " + gameQuestions.size() + " question(s), "
                + config.getQuestionDuration() + " seconds each.");
        client.sendMessage("---------------------------------------");

        for (int i = 0; i < gameQuestions.size(); i++) {
            Question q = gameQuestions.get(i);
            client.sendMessage("\nQuestion " + (i + 1) + "/" + gameQuestions.size()
                    + "  [" + q.getDifficulty() + "]");
            sendQuestion(client, q);

            String answer = readAnswerWithTimer(client, config.getQuestionDuration());

            char ca = q.getCorrectAnswer();
            String correctText = q.getChoices().get(ca - 'A');
            boolean correct = q.checkAnswer(answer);
            int points = calcPoints(correct, q.getDifficulty());

            if (correct) {
                p.addScore(points);
                client.sendMessage("Correct! +" + points + " points  |  Total: " + p.getCurrentScore());
            } else {
                client.sendMessage("Wrong!  |  Score: " + p.getCurrentScore());
            }
            client.sendMessage("[Correct answer: " + ca + ") " + correctText + "]");
            p.addHistory(q.getText(), correct);
        }

        showSinglePlayerResult(client, p, gameQuestions);
        scoreService.addScore(client.getUser(), p.getCurrentScore());
    }

    @Override
    public void createTeam(ClientHandler client) {
        client.sendMessage("\nCREATE TEAM VS TEAM GAME ");
        client.sendMessage("\n");
        client.sendMessage("Enter Team 1 name:");
        client.sendInput();
        String team1Name = readLineOrNull(client);
        if (team1Name == null || team1Name.trim().equals("-")) return;
        team1Name = team1Name.trim();
        if (team1Name.isEmpty()) { client.sendMessage("Name cannot be empty."); return; }

        client.sendMessage("How many players in Team 1? (1-" + config.getMaxPlayers() + "):");
        client.sendInput();
        int t1size = readPositiveInt(client, config.getMaxPlayers());
        if (t1size <= 0) return;

        Set<String> team1Members = new LinkedHashSet<>();
        team1Members.add(client.getUsername());
        client.sendMessage("(You are in Team 1 as: " + client.getUsername() + ")");
        for (int i = team1Members.size() + 1; i <= t1size; i++) {
            client.sendMessage("Enter username of Team 1 member " + i + ":");
            client.sendInput();
            String uname = readLineOrNull(client);
            if (uname == null || uname.trim().equals("-")) return;
            uname = uname.trim();
            if (team1Members.contains(uname)) {
                client.sendMessage("Already added. Try again.");
                i--;
            } else {
                team1Members.add(uname);
            }
        }

        String team2Name;
        while (true) {
            client.sendMessage("Enter Team 2 name:");
            client.sendInput();
            team2Name = readLineOrNull(client);
            if (team2Name == null || team2Name.trim().equals("-")) return;
            team2Name = team2Name.trim();
            if (team2Name.isEmpty()) { client.sendMessage("Name cannot be empty."); continue; }
            if (team2Name.equalsIgnoreCase(team1Name)) {
                client.sendMessage("Team names must be unique. Choose another.");
                continue;
            }
            break;
        }

        client.sendMessage("How many players in Team 2? (must equal " + t1size + "):");
        client.sendInput();
        int t2size = readPositiveInt(client, config.getMaxPlayers());
        if (t2size <= 0) return;
        if (t2size != t1size) {
            client.sendMessage("ERROR: Both teams must have equal number of players.");
            return;
        }

        Set<String> team2Members = new LinkedHashSet<>();
        for (int i = 1; i <= t2size; i++) {
            client.sendMessage("Enter username of Team 2 member " + i + ":");
            client.sendInput();
            String uname = readLineOrNull(client);
            if (uname == null || uname.trim().equals("-")) return;
            uname = uname.trim();
            if (team1Members.contains(uname)) {
                client.sendMessage("ERROR: " + uname + " is already in Team 1. Cannot be in both teams.");
                i--;
            } else if (team2Members.contains(uname)) {
                client.sendMessage("Already added. Try again.");
                i--;
            } else {
                team2Members.add(uname);
            }
        }

        String category = chooseCategory(client);
        if (category == null) return;
        String difficulty = chooseDifficulty(client);
        if (difficulty == null) return;
        int numQ = chooseNumQuestions(client, category, difficulty);
        if (numQ <= 0) return;

        Team t1 = new Team(team1Name);
        t1.setCategory(category); t1.setDifficulty(difficulty); t1.setNumQuestions(numQ);

        Team t2 = new Team(team2Name);
        t2.setCategory(category); t2.setDifficulty(difficulty); t2.setNumQuestions(numQ);

        String gameId;
        synchronized (GameService.class) { gameId = "GAME-" + (++gameCounter); }
        t1.setGameId(gameId);
        t2.setGameId(gameId);
        pendingGames.put(gameId, new Team[]{t1, t2});


        t1.addPlayer(new Player(client.getUsername(), client.getSocket()));

        client.sendMessage("\nGame [" + gameId + "] created!");
        client.sendMessage("Team 1: " + team1Name + " — members: " + team1Members);
        client.sendMessage("Team 2: " + team2Name + " — members: " + team2Members);
        client.sendMessage("Other players: choose Multiplayer -> Join -> enter ID: " + gameId);
        client.sendMessage("Waiting for all players to connect...");

        waitForAllMembers(client, t1, t2, team1Members, team2Members, gameId);
    }

    private void waitForAllMembers(ClientHandler creatorClient, Team t1, Team t2,
                                   Set<String> t1Names, Set<String> t2Names, String gameId) {
        creatorClient.sendMessage("Type '-' to cancel.");
        AtomicBoolean cancelled = new AtomicBoolean(false);

        Thread cancelThread = new Thread(() -> {
            try {
                String line = creatorClient.getReader().readLine();
                if ("-".equals(line)) cancelled.set(true);
            } catch (Exception ignored) { cancelled.set(true); }
        });
        cancelThread.setDaemon(true);
        cancelThread.start();
        creatorClient.sendInput();

        while (!cancelled.get()) {
            for (String u : t1Names) {
                boolean inTeam = t1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(u));
                if (!inTeam && handlerRegistry.containsKey(u)) {
                    ClientHandler ch = handlerRegistry.get(u);
                    t1.addPlayer(new Player(u, ch.getSocket()));
                    creatorClient.sendMessage(u + " joined Team 1.");
                }
            }
            for (String u : t2Names) {
                boolean inTeam = t2.getPlayers().stream().anyMatch(p -> p.getUsername().equals(u));
                if (!inTeam && handlerRegistry.containsKey(u)) {
                    ClientHandler ch = handlerRegistry.get(u);
                    t2.addPlayer(new Player(u, ch.getSocket()));
                    creatorClient.sendMessage(u + " joined Team 2.");
                }
            }

            long t1Joined = t1Names.stream().filter(u ->
                    t1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(u))).count();
            long t2Joined = t2Names.stream().filter(u ->
                    t2.getPlayers().stream().anyMatch(p -> p.getUsername().equals(u))).count();

            if (t1Joined == t1Names.size() && t2Joined == t2Names.size()) {
                cancelThread.interrupt();
                pendingGames.remove(gameId);
                creatorClient.sendMessage("All players ready! Starting...");
                playTeamVsTeam(t1, t2);
                return;
            }

            try { Thread.sleep(2000); } catch (InterruptedException ignored) { break; }
            creatorClient.sendMessage("Waiting... T1: " + t1Joined + "/" + t1Names.size()
                    + "  T2: " + t2Joined + "/" + t2Names.size());
        }

        cancelThread.interrupt();
        pendingGames.remove(gameId);
        creatorClient.sendMessage("Cancelled. Returning to menu.");
    }

    @Override
    public void joinTeam(ClientHandler client) {
        if (pendingGames.isEmpty()) {
            client.sendMessage("No games are waiting for players right now.");
            return;
        }

        StringBuilder sb = new StringBuilder("\n PENDING GAMES \n");
        for (Map.Entry<String, Team[]> entry : pendingGames.entrySet()) {
            String gid = entry.getKey();
            Team t1    = entry.getValue()[0];
            Team t2    = entry.getValue()[1];
            sb.append("\n[").append(gid).append("]\n");
            sb.append("  Team 1: ").append(t1.getTeamName())
                    .append("  (").append(t1.getPlayers().size()).append(" joined)\n");
            sb.append("  Team 2: ").append(t2.getTeamName())
                    .append("  (").append(t2.getPlayers().size()).append(" joined)\n");
            sb.append("  Category: ").append(t1.getCategory())
                    .append(" | Difficulty: ").append(t1.getDifficulty())
                    .append(" | Questions: ").append(t1.getNumQuestions()).append("\n");
        }
        client.sendMessage(sb.toString().trim());

        client.sendMessage("\nEnter Game ID to join (e.g. GAME-1):");
        client.sendInput();
        String gameId = readLineOrNull(client);
        if (gameId == null || gameId.trim().equals("-")) return;
        gameId = gameId.trim().toUpperCase();

        Team[] pair = pendingGames.get(gameId);
        if (pair == null) { client.sendMessage("Game ID not found."); return; }

        Team t1 = pair[0];
        Team t2 = pair[1];

        boolean inT1 = t1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(client.getUsername()));
        boolean inT2 = t2.getPlayers().stream().anyMatch(p -> p.getUsername().equals(client.getUsername()));

        if (!inT1 && !inT2) {
            client.sendMessage("Which team are you joining?");
            client.sendMessage("1) " + t1.getTeamName());
            client.sendMessage("2) " + t2.getTeamName());
            client.sendInput();
            String pick = readLineOrNull(client);
            if (pick == null || pick.trim().equals("-")) return;
            Team chosen = pick.trim().equals("1") ? t1 : (pick.trim().equals("2") ? t2 : null);
            if (chosen == null) { client.sendMessage("Invalid choice."); return; }
            chosen.addPlayer(new Player(client.getUsername(), client.getSocket()));
            client.sendMessage("Joined team '" + chosen.getTeamName() + "'.");
        } else {
            client.sendMessage("You are already registered in this game.");
        }

        client.sendMessage("Waiting for game to start...");
        while (pendingGames.containsKey(gameId) && !t1.isGameStarted()) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) { return; }
        }
    }


    private synchronized void playTeamVsTeam(Team team1, Team team2) {
        if (team1.isGameStarted()) return;
        team1.setGameStarted(true);
        team2.setGameStarted(true);

        List<Question> qs = filterQuestions(
                team1.getCategory(), team1.getDifficulty(), team1.getNumQuestions());

        List<ClientHandler> allClients = new ArrayList<>();
        allClients.addAll(getHandlersForTeam(team1));
        allClients.addAll(getHandlersForTeam(team2));

        if (qs.isEmpty()) {
            broadcastToAll(allClients, "No questions found. Game cancelled.");
            return;
        }

        broadcastToAll(allClients, "\n TEAM VS TEAM: "
                + team1.getTeamName() + " vs " + team2.getTeamName() );
        broadcastToAll(allClients, "Category: " + team1.getCategory()
                + " | Difficulty: " + team1.getDifficulty()
                + " | Questions: " + qs.size()
                + " | Time/Q: " + config.getQuestionDuration() + "s");
        broadcastToAll(allClients, "First correct answer scores points for that team!");
        broadcastToAll(allClients, "---------------------------------------");

        for (int i = 0; i < qs.size(); i++) {
            Question q = qs.get(i);
            broadcastToAll(allClients,
                    "\nQuestion " + (i + 1) + "/" + qs.size() + "  [" + q.getDifficulty() + "]");

            for (ClientHandler c : allClients) sendQuestion(c, q);

            for (Player p : team1.getPlayers()) p.resetForNextQuestion();
            for (Player p : team2.getPlayers()) p.resetForNextQuestion();

            firstCorrectWins(team1, team2, q, allClients);

            char ca = q.getCorrectAnswer();
            broadcastToAll(allClients,
                    "[Correct answer: " + ca + ") " + q.getChoices().get(ca - 'A') + "]");

            broadcastToAll(allClients, "\n Scores after Q" + (i + 1) );
            broadcastScoreTable(team1, team2, allClients);
        }

        showTeamResults(team1, team2, qs, allClients);

        for (Player p : team1.getPlayers())
            scoreService.addScore(new User(p.getUsername(), p.getUsername(), ""), p.getCurrentScore());
        for (Player p : team2.getPlayers())
            scoreService.addScore(new User(p.getUsername(), p.getUsername(), ""), p.getCurrentScore());
    }

    private void firstCorrectWins(Team team1, Team team2, Question q,
                                  List<ClientHandler> allClients) {
        AtomicBoolean questionWon = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(allClients.size());

        for (ClientHandler ch : allClients) {
            Player p = getPlayerForHandler(ch, team1, team2);
            if (p == null) { latch.countDown(); continue; }

            boolean inTeam1 = team1.getPlayers().contains(p);
            Team myTeam     = inTeam1 ? team1 : team2;

            new Thread(() -> {
                try {
                    String answer = readAnswerWithTimer(ch, config.getQuestionDuration());

                    if (questionWon.get()) {
                        ch.sendMessage("Answer received but question already answered. No points.");
                        p.addHistory(q.getText(), false);
                        return;
                    }

                    boolean correct = q.checkAnswer(answer);

                    if (correct && questionWon.compareAndSet(false, true)) {
                        int pts = calcPoints(true, q.getDifficulty());
                        p.addScore(pts);
                        myTeam.addTeamScore(pts);
                        ch.sendMessage("FIRST CORRECT ANSWER! +" + pts + " pts!");
                        broadcastToTeams(team1, team2,
                                "  " + p.getUsername() + " (" + myTeam.getTeamName()
                                        + ") answered correctly first!");
                        p.addHistory(q.getText(), true);
                    } else if (correct) {
                        ch.sendMessage("Correct, but someone else answered first. No points.");
                        p.addHistory(q.getText(), false);
                    } else {
                        char ca = q.getCorrectAnswer();
                        ch.sendMessage("Wrong! Correct: " + ca + ") " + q.getChoices().get(ca - 'A'));
                        p.addHistory(q.getText(), false);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try { latch.await(); } catch (InterruptedException ignored) {}

        if (!questionWon.get()) {
            char ca = q.getCorrectAnswer();
            broadcastToTeams(team1, team2,
                    "No one answered correctly! Answer: "
                            + ca + ") " + q.getChoices().get(ca - 'A'));
        }
    }


    private void sendQuestion(ClientHandler client, Question q) {
        client.sendMessage("Question: " + q.getText());
        char opt = 'A';
        for (String choice : q.getChoices()) {
            client.sendMessage("  " + opt + ") " + choice);
            opt++;
        }
        client.sendMessage("Your answer (A/B/C/D):");
        client.sendInput();
    }

    private String readAnswerWithTimer(ClientHandler client, int durationSeconds) {
        long durationMs = durationSeconds * 1000L;
        AtomicReference<String> answerRef = new AtomicReference<>(null);

        Thread readerThread = new Thread(() -> {
            try {
                String line = client.getReader().readLine();
                answerRef.set(line == null ? "" : line.trim());
            } catch (IOException e) { answerRef.set(""); }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        long start = System.currentTimeMillis();
        int[] milestones = {25, 20, 15, 10, 5, 3, 2, 1};
        Set<Integer> sent = new HashSet<>();

        while (System.currentTimeMillis() - start < durationMs) {
            if (answerRef.get() != null) {
                readerThread.interrupt();
                return answerRef.get();
            }
            long remaining = (durationMs - (System.currentTimeMillis() - start)) / 1000;
            for (int m : milestones) {
                if (remaining <= m && !sent.contains(m)) {
                    sent.add(m);
                    client.sendMessage("[Timer] " + remaining + " second(s) left!");
                }
            }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        readerThread.interrupt();
        client.sendMessage("[Timer] Time's up!");
        return null;
    }


    private String chooseCategory(ClientHandler client) {
        List<String> cats = allQuestions.stream()
                .map(Question::getCategory).distinct().sorted().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("Choose a category:\n");
        for (int i = 0; i < cats.size(); i++)
            sb.append((i + 1)).append(") ").append(cats.get(i)).append("\n");
        client.sendMessage(sb.toString().trim());
        client.sendMessage("Enter number:");
        client.sendInput();
        String input = readLineOrNull(client);
        if (input == null || input.trim().equals("-")) return null;
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx >= 0 && idx < cats.size()) return cats.get(idx);
        } catch (NumberFormatException ignored) {}
        for (String c : cats) if (c.equalsIgnoreCase(input.trim())) return c;
        client.sendMessage("Invalid. Using first category.");
        return cats.get(0);
    }

    private String chooseDifficulty(ClientHandler client) {
        client.sendMessage("Choose difficulty:\n1) EASY\n2) MEDIUM\n3) HARD");
        client.sendMessage("Enter number:");
        client.sendInput();
        String input = readLineOrNull(client);
        if (input == null || input.trim().equals("-")) return null;
        switch (input.trim()) {
            case "1": return "EASY";
            case "2": return "MEDIUM";
            case "3": return "HARD";
            default:
                String up = input.trim().toUpperCase();
                if (up.equals("EASY") || up.equals("MEDIUM") || up.equals("HARD")) return up;
                client.sendMessage("Invalid. Defaulting to EASY.");
                return "EASY";
        }
    }

    private int chooseNumQuestions(ClientHandler client, String category, String difficulty) {
        long available = allQuestions.stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(category)
                        && q.getDifficulty().equalsIgnoreCase(difficulty))
                .count();
        if (available == 0) {
            client.sendMessage("No " + difficulty + " questions in " + category + ".");
            return 0;
        }
        client.sendMessage("How many questions? (1-" + available + "):");
        client.sendInput();
        String input = readLineOrNull(client);
        if (input == null || input.trim().equals("-")) return -1;
        try {
            int n = Integer.parseInt(input.trim());
            if (n >= 1 && n <= available) return n;
            client.sendMessage("Out of range. Using max: " + available + ".");
            return (int) available;
        } catch (NumberFormatException e) {
            client.sendMessage("Invalid. Using max: " + available + ".");
            return (int) available;
        }
    }

    private List<Question> filterQuestions(String category, String difficulty, int count) {
        List<Question> filtered = allQuestions.stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(category)
                        && q.getDifficulty().equalsIgnoreCase(difficulty))
                .collect(Collectors.toList());
        Collections.shuffle(filtered);
        return filtered.subList(0, Math.min(count, filtered.size()));
    }

    private int calcPoints(boolean correct, String difficulty) {
        if (!correct) return 0;
        switch (difficulty.toUpperCase()) {
            case "HARD":   return 20;
            case "MEDIUM": return 15;
            default:       return 10;
        }
    }

    private void showSinglePlayerResult(ClientHandler client, Player p, List<Question> qs) {
        client.sendMessage("\n GAME OVER ");
        int correct = 0, wrong = 0;
        client.sendMessage(" Question Summary ");
        for (Question q : qs) {
            Boolean result = p.getGameHistory().get(q.getText());
            boolean ok = result != null && result;
            if (ok) correct++; else wrong++;
            client.sendMessage((ok ? "Correct" : "Wrong ") + ": " + q.getText()
                    + "  (Answer: " + q.getCorrectAnswer() + ")");
        }
        client.sendMessage("\nCorrectly answered: " + correct
                + "  |  Wrong: " + wrong
                + "  |  Final Score: " + p.getCurrentScore());
        client.sendMessage("\n");
    }

    private void showTeamResults(Team team1, Team team2, List<Question> qs,
                                 List<ClientHandler> allClients) {
        StringBuilder sb = new StringBuilder("\n TEAM GAME OVER \n");
        if (team1.getTeamScore() > team2.getTeamScore())
            sb.append("WINNER: ").append(team1.getTeamName()).append("!\n");
        else if (team2.getTeamScore() > team1.getTeamScore())
            sb.append("WINNER: ").append(team2.getTeamName()).append("!\n");
        else sb.append("It's a DRAW!\n");

        sb.append("\n Final Scores \n");
        sb.append(String.format("%-20s : %-15s : %-14s : %s%n",
                "Username", "Team Name", "Player Score", "Team Total"));
        sb.append("-".repeat(70)).append("\n");
        for (Player pl : team1.getPlayers())
            sb.append(String.format("%-20s : %-15s : %-14d : %d%n",
                    pl.getUsername(), team1.getTeamName(),
                    pl.getCurrentScore(), team1.getTeamScore()));
        for (Player pl : team2.getPlayers())
            sb.append(String.format("%-20s : %-15s : %-14d : %d%n",
                    pl.getUsername(), team2.getTeamName(),
                    pl.getCurrentScore(), team2.getTeamScore()));

        sb.append("\n Questions \n");
        for (Question q : qs)
            sb.append(q.getText()).append("  (Answer: ").append(q.getCorrectAnswer()).append(")\n");
        sb.append("===================================");
        String msg = sb.toString();
        for (ClientHandler c : allClients) c.sendMessage(msg);
    }

    // Format: username : teamName : playerScore : teamTotal
    private void broadcastScoreTable(Team team1, Team team2, List<ClientHandler> allClients) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s : %-15s : %-14s : %s%n",
                "Username", "Team Name", "Player Score", "Team Total"));
        sb.append("-".repeat(70)).append("\n");
        for (Player p : team1.getPlayers())
            sb.append(String.format("%-20s : %-15s : %-14d : %d%n",
                    p.getUsername(), team1.getTeamName(),
                    p.getCurrentScore(), team1.getTeamScore()));
        for (Player p : team2.getPlayers())
            sb.append(String.format("%-20s : %-15s : %-14d : %d%n",
                    p.getUsername(), team2.getTeamName(),
                    p.getCurrentScore(), team2.getTeamScore()));
        String table = sb.toString().trim();
        for (ClientHandler c : allClients) c.sendMessage(table);
    }


    private void broadcastToTeams(Team t1, Team t2, String msg) {
        for (ClientHandler c : getHandlersForTeam(t1)) c.sendMessage(msg);
        for (ClientHandler c : getHandlersForTeam(t2)) c.sendMessage(msg);
    }

    private void broadcastToAll(List<ClientHandler> clients, String msg) {
        for (ClientHandler c : clients) c.sendMessage(msg);
    }

    private List<ClientHandler> getHandlersForTeam(Team t) {
        List<ClientHandler> list = new ArrayList<>();
        for (Player p : t.getPlayers()) {
            ClientHandler ch = handlerRegistry.get(p.getUsername());
            if (ch != null) list.add(ch);
        }
        return list;
    }

    private Player getPlayerForHandler(ClientHandler ch, Team t1, Team t2) {
        for (Player p : t1.getPlayers()) if (p.getUsername().equals(ch.getUsername())) return p;
        for (Player p : t2.getPlayers()) if (p.getUsername().equals(ch.getUsername())) return p;
        return null;
    }

    private int readPositiveInt(ClientHandler client, int max) {
        String input = readLineOrNull(client);
        if (input == null || input.trim().equals("-")) return -1;
        try {
            int n = Integer.parseInt(input.trim());
            if (n >= 1 && n <= max) return n;
            client.sendMessage("Please enter a number between 1 and " + max + ".");
            return -1;
        } catch (NumberFormatException e) {
            client.sendMessage("Invalid number.");
            return -1;
        }
    }

    private String readLineOrNull(ClientHandler client) {
        try { return client.getReader().readLine(); }
        catch (IOException e) { return null; }
    }

    @Override public void startTeamGame()  {}
    @Override public void sendQuestion()   {}
    @Override public void checkAnswer(ClientHandler client, String answer) {}
    @Override public void startQuestionTimer() {}
    @Override public void endGame() {}
}