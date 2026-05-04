Here’s a clean and professional `README.md` file generated from your document, ready to paste directly into your GitHub repo:

---

```markdown
# 🎮 Multiplayer Trivia Game

## 📌 Overview
This project is a **Multiplayer Trivia Game** implemented using **Java Socket Programming** and **Multithreading**.

It follows a **client-server architecture** where:
- The **server** manages the game logic and multiple client connections.
- The **clients** interact with the server through a command-line interface.

---

## 🏗️ System Architecture

### 🔹 Server Responsibilities
- Accept multiple client connections
- Handle user authentication (register/login)
- Load game configuration and question bank
- Manage game sessions (single & multiplayer)
- Control timers and scoring
- Maintain score history

Each client is handled in a separate thread (`ClientHandler`).

### 🔹 Client Responsibilities
- Register or login
- Choose game mode
- Answer questions within time limits
- Receive real-time updates (scores, timers)

Communication is done via **TCP sockets** using:
- `BufferedReader`
- `PrintWriter`

---

## 🛠️ Technologies Used
- Java JDK 21
- Java Socket Programming (`Socket`, `ServerSocket`)
- Multithreading (`Thread`, `java.util.concurrent`)
- File-based storage

No external libraries are used.

---

## 📂 Data Files

The server loads the following files from the `data/` folder:

| File            | Purpose |
|-----------------|--------|
| `config.txt`    | Game configuration |
| `questions.txt` | Question bank |
| `users.txt`     | User credentials |
| `scores.txt`    | Score history |

> ⚠️ `users.txt` and `scores.txt` are created automatically if missing.

---

## ⚙️ Configuration (`config.txt`)
```

PORT=5555
QUESTION_DURATION=30
MIN_PLAYERS=1
MAX_PLAYERS=4

```

---

## ❓ Question Format
Each question follows this format:
```

CATEGORY|QUESTION|A|B|C|D|CORRECT|DIFFICULTY

```

### Example:
```

SCIENCE|What is the chemical formula for water?|HO|H2O|H3O|H2O2|B|EASY

```

---

## 🔐 Authentication

### Registration
Users provide:
- Name
- Username
- Password

Stored as:
```

name:username:password

```

### Login Errors
- `401` → Incorrect password
- `404` → Username not found
- Custom → Username already exists

---

## 🎮 Game Modes

### 1️⃣ Single Player
- Choose:
  - Category
  - Difficulty (EASY / MEDIUM / HARD)
  - Number of questions
- Questions are randomized
- One answer per question
- Time-limited (default: 30 seconds)

---

### 2️⃣ Multiplayer (Team vs Team)

#### Features:
- Two teams compete
- Equal number of players per team
- Game starts when all players join

#### Rules:
- First correct answer earns points
- Late correct answers = **no points**
- If no correct answer → no points awarded

---

## ⏱️ Timer System
- Each question has a countdown timer
- Warnings at:
```

25, 20, 15, 10, 5, 3, 2, 1 seconds

```
- Late answers are ignored

---

## 🧮 Scoring System

| Difficulty | Points |
|------------|--------|
| EASY       | +10    |
| MEDIUM     | +15    |
| HARD       | +20    |

### End of Game Shows:
- Total score
- Correct answers
- Wrong answers
- Per-question feedback

---

## 📊 Score History
- Stored in `scores.txt`
- Keeps last **10 scores per user**

Format:
```

username:10:20:30:15

```

---

## ⚔️ Multiplayer Details

### Setup:
1. Create teams
2. Assign players by username
3. Generate Game ID
4. Players join using Game ID

### Additional Rules:
- Teams must have equal players
- Only listed players can join
- Lobby timeout: **5 minutes**

---

## 🚨 Exception Handling

Handled cases include:
- Invalid login credentials
- Duplicate usernames
- Unequal team sizes
- Player disconnects
- Timeout answers
- Missing config file (defaults applied)

---

## ▶️ How to Run

### 1. Setup
- Open project in IntelliJ (or any Java IDE)
- Ensure **JDK 21** is configured

### 2. Prepare Data Folder
Create `data/` with:
```

config.txt
questions.txt
(users.txt and scores.txt auto-generated)

```

---

### 3. Start Server
Run:
```

com.example.server.GameServer

```

Expected output:
```

Listening on port 5555
Loaded questions...
Ready — waiting for clients...

```

---

### 4. Start Client(s)
Run:
```

com.example.client.Client

```

Open multiple terminals for multiple players.

---

### 5. Play
- Register or login
- Choose mode:
  - Single Player
  - Multiplayer

---

## 📁 Project Structure

```

src/main/java/com/example/
├── client/
│   └── Client.java
├── server/
│   ├── GameServer.java
│   ├── ClientHandler.java
│   └── QuestionServer.java
├── model/
│   ├── User.java
│   ├── Player.java
│   ├── Question.java
│   ├── Team.java
│   └── Config.java
├── service/
│   ├── GameService.java
│   ├── AuthService.java
│   ├── ScoreService.java
│   ├── QuestionService.java
│   ├── Interfaces...
└── util/
├── FileLoader.java
└── PathResolver.java

data/
├── config.txt
├── questions.txt
├── users.txt
└── scores.txt

```

---

## ⚙️ Design Decisions
- Server controls all logic and timing
- Clients are input/output only
- One answer per question
- Questions are randomized
- Multiplayer uses **first correct answer wins**
- Score history limited to last 10 entries
- System is resilient to disconnections

---

## 📌 Notes
- Answers are **case-insensitive**
- Players can type `-` to quit anytime
- System ensures fairness in multiplayer mode

---

## 👨‍💻 Authors
- Basant Hatem  
- Emy Ihab  
 

---

## 📄 License
This project is for educational purposes (Distributed Systems Assignment – Winter 2026).
```

