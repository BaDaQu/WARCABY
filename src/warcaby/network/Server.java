package warcaby.network;

import warcaby.gamelogic.Board;
import warcaby.gamelogic.PlayerColor;
import warcaby.utils.GameConstants;
import warcaby.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Iterator;

public class Server {
    private static final int PORT = GameConstants.SERVER_PORT;
    private static final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
    private static final Logger logger = new Logger(Server.class);

    private static final Map<String, ClientHandler> waitingPlayers = new ConcurrentHashMap<>();
    private static final Map<String, GameSession> activeGameSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        logger.info("Serwer warcabów uruchamiany na porcie " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Serwer nasłuchuje na porcie " + PORT);
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Nowe połączenie od klienta: " + clientSocket.getInetAddress().getHostAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clientProcessingPool.execute(clientHandler);
                } catch (IOException e) {
                    logger.error("Błąd akceptowania połączenia od klienta: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Nie można uruchomić serwera na porcie " + PORT + ": " + e.getMessage(), e);
        } finally {
            clientProcessingPool.shutdown();
            logger.info("Serwer zakończył działanie.");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private final String playerId;
        private volatile GameSession currentSession = null;
        private volatile boolean isSearching = false;
        private volatile boolean isConnected = false;
        private final Object stateLock = new Object();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.playerId = UUID.randomUUID().toString();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                isConnected = true;
                logger.info("Handler dla klienta " + playerId + " (" + clientSocket.getRemoteSocketAddress() + ") uruchomiony.");

                String messageFromClient;
                while (isConnected && clientSocket != null && !clientSocket.isClosed() && !clientSocket.isInputShutdown() &&
                        (messageFromClient = in.readLine()) != null) {
                    logger.debug("Klient " + playerId + " wysłał: " + messageFromClient);
                    processClientCommand(messageFromClient);
                }
            } catch (IOException e) {
                String logMessage = "Połączenie z klientem " + playerId;
                if (e instanceof SocketException) {
                    if ("Socket closed".equalsIgnoreCase(e.getMessage()) ||
                            "Connection reset".equalsIgnoreCase(e.getMessage()) ||
                            "Broken pipe".equalsIgnoreCase(e.getMessage().toLowerCase())) {
                        logMessage += " zostało zamknięte lub zresetowane.";
                        logger.info(logMessage);
                    } else {
                        logMessage += " - błąd gniazda: " + e.getMessage();
                        if(isConnected) logger.warning(logMessage); else logger.info(logMessage + " (połączenie już zamykane)");
                    }
                } else {
                    logMessage += " - błąd We/Wy: " + e.getMessage();
                    if (isConnected) logger.error(logMessage, e); else logger.info(logMessage + " (połączenie już zamykane)");
                }
            } finally {
                cleanup();
            }
        }

        private void processClientCommand(String command) {
            if (!isConnected) { return; }
            String[] parts = NetworkProtocol.parseMessage(command);
            String action = parts[0];
            String data = parts[1];

            switch (action) {
                case NetworkProtocol.CMD_FIND_GAME:
                    handleFindGame();
                    break;
                case NetworkProtocol.CMD_MOVE:
                    if (currentSession != null) currentSession.forwardMove(this, NetworkProtocol.CMD_MOVE, data);
                    else logger.warning("Klient " + playerId + " próbował wysłać ruch bez aktywnej sesji.");
                    break;
                case NetworkProtocol.CMD_CAPTURE_CONTINUED:
                    if (currentSession != null) currentSession.forwardMove(this, NetworkProtocol.CMD_CAPTURE_CONTINUED, data);
                    else logger.warning("Klient " + playerId + " próbował wysłać kontynuację bicia bez aktywnej sesji.");
                    break;
                case NetworkProtocol.CMD_CANCEL_SEARCH:
                    handleCancelSearch();
                    break;
                case NetworkProtocol.CMD_QUIT:
                    if (currentSession != null) currentSession.playerQuit(this, false);
                    break;
                case NetworkProtocol.CMD_END_SESSION:
                    if (currentSession != null) currentSession.playerQuit(this, true);
                    isConnected = false;
                    break;
                default:
                    logger.warning("Klient " + playerId + ": Nieznana komenda: " + action);
                    sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Nieznana komenda"));
            }
        }

        private void handleFindGame() {
            synchronized (stateLock) {
                if (currentSession != null) {
                    sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Już jesteś w grze."));
                    return;
                }
                if (isSearching) {
                    sendMessage(NetworkProtocol.RSP_WAITING);
                    return;
                }
                isSearching = true;
                logger.info("Klient " + playerId + " ustawił isSearching=true.");
            }
            tryToPair();
        }

        private void tryToPair() {
            ClientHandler opponentHandler = null;
            String opponentIdToPair = null;

            synchronized (waitingPlayers) {
                synchronized(stateLock) {
                    if (!isSearching || !isConnected || currentSession != null) {
                        logger.info("Klient " + playerId + ": Anulowano próbę parowania (stan nie pozwala). isSearching=" + isSearching);
                        return;
                    }
                }

                for (Map.Entry<String, ClientHandler> entry : waitingPlayers.entrySet()) {
                    ClientHandler potentialOpponent = entry.getValue();
                    if (potentialOpponent != this && potentialOpponent.isClientConnected()) {
                        synchronized (potentialOpponent.stateLock) {
                            if (potentialOpponent.isSearching && potentialOpponent.currentSession == null) {
                                opponentHandler = potentialOpponent;
                                opponentIdToPair = entry.getKey();
                                break;
                            }
                        }
                    }
                }

                if (opponentHandler != null && opponentIdToPair != null) {
                    boolean pairMade = false;
                    synchronized(opponentHandler.stateLock) {
                        if (opponentHandler.isSearching && opponentHandler.isClientConnected() && opponentHandler.currentSession == null) {
                            if (waitingPlayers.remove(opponentIdToPair, opponentHandler)) {
                                opponentHandler.isSearching = false;
                                synchronized (this.stateLock) {
                                    if (this.isSearching && this.currentSession == null && this.isConnected) {
                                        waitingPlayers.remove(this.playerId, this);
                                        this.isSearching = false;
                                        pairMade = true;
                                    } else {
                                        waitingPlayers.put(opponentIdToPair, opponentHandler);
                                        opponentHandler.isSearching = true;
                                        logger.info("Nie udało się sparować - stan klienta " + this.playerId + " zmienił się. Przeciwnik " + opponentIdToPair + " wraca do kolejki.");
                                    }
                                }
                            } else {
                                logger.info("Nie udało się usunąć przeciwnika " + opponentIdToPair + " z kolejki.");
                            }
                        }
                    }

                    if (pairMade) {
                        logger.info("Parowanie graczy: " + this.playerId + " z " + opponentHandler.playerId);
                        GameSession newSession = new GameSession(this, opponentHandler);
                        activeGameSessions.put(newSession.getSessionId(), newSession);
                        this.setCurrentSession(newSession);
                        opponentHandler.setCurrentSession(newSession);
                        newSession.startGame();
                        return;
                    } else {
                        logger.info("Nie udało się sfinalizować parowania dla " + this.playerId + " (przeciwnik " + opponentIdToPair + " mógł być niedostępny).");
                    }
                }

                synchronized(this.stateLock) {
                    if (this.isSearching && this.currentSession == null && this.isConnected) {
                        if (!waitingPlayers.containsKey(this.playerId)) {
                            waitingPlayers.put(this.playerId, this);
                        }
                        sendMessage(NetworkProtocol.RSP_WAITING);
                        logger.info("Klient " + playerId + " dodany/pozostaje w kolejce oczekujących.");
                    }
                }
            }
        }

        private void handleCancelSearch() {
            boolean wasSearchingAndActuallyRemoved = false;
            synchronized (stateLock) {
                if (isSearching) {
                    isSearching = false;
                    synchronized (waitingPlayers) {
                        if (waitingPlayers.remove(playerId, this)) {
                            wasSearchingAndActuallyRemoved = true;
                        }
                    }
                }
            }

            if (wasSearchingAndActuallyRemoved) {
                sendMessage(NetworkProtocol.RSP_SEARCH_CANCELLED);
                logger.info("Klient " + playerId + " anulował wyszukiwanie gry i został usunięty z kolejki.");
            } else {
                logger.info("Klient " + playerId + " próbował anulować, ale nie był w trybie aktywnego wyszukiwania lub nie został znaleziony w kolejce.");
            }
        }

        public void sendMessage(String message) {
            if (isConnected && out != null && clientSocket != null && !clientSocket.isOutputShutdown() && !clientSocket.isClosed()) {
                out.println(message);
                logger.debug("Wysłano do klienta " + playerId + ": " + message);
            }
        }

        private void cleanup() {
            if (!isConnected && (in == null && out == null && (clientSocket == null || clientSocket.isClosed()))) {
                return;
            }
            logger.info("Rozpoczynanie cleanup dla klienta " + playerId);
            isConnected = false;

            synchronized (stateLock) {
                if (isSearching) isSearching = false;
            }
            synchronized (waitingPlayers) {
                waitingPlayers.remove(playerId, this);
            }

            if (currentSession != null) {
                currentSession.playerQuit(this, true);
                currentSession = null;
            }

            try { if (in != null) in.close(); } catch (IOException e) { logger.error("Błąd zamykania strumienia wejściowego dla " + playerId, e); } finally { in = null; }
            try { if (out != null) out.close(); } catch (Exception e) { logger.error("Błąd zamykania strumienia wyjściowego dla " + playerId, e); } finally { out = null; }
            try { if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); } catch (IOException e) { logger.error("Błąd zamykania gniazda dla " + playerId, e); }

            logger.info("Zakończono cleanup dla klienta " + playerId);
        }

        public String getPlayerId() { return playerId; }
        public GameSession getCurrentSession() { return currentSession; }
        public void setCurrentSession(GameSession session) { this.currentSession = session; }
        public boolean isClientConnected() { return isConnected && clientSocket != null && !clientSocket.isClosed(); }
    }

    private static class GameSession {
        private final String sessionId;
        private final ClientHandler player1;
        private final ClientHandler player2;
        private ClientHandler whitePlayer;
        private ClientHandler blackPlayer;

        private Timer gameLogicTimer;
        private long whiteTimeMillis = 0;
        private long blackTimeMillis = 0;
        private long turnStartTimeMillis = 0;
        private volatile boolean gameInProgress = false;
        private final Board serverSideBoard;

        public GameSession(ClientHandler p1, ClientHandler p2) {
            this.sessionId = UUID.randomUUID().toString();
            this.player1 = p1;
            this.player2 = p2;
            assignColors();
            this.serverSideBoard = new Board();
            logger.info("Utworzono sesję gry " + sessionId + ": Białe=" + (whitePlayer!=null?whitePlayer.getPlayerId():"null") + ", Czarne=" + (blackPlayer!=null?blackPlayer.getPlayerId():"null"));
        }

        private void assignColors() {
            if (new Random().nextBoolean()) {
                this.whitePlayer = player1;
                this.blackPlayer = player2;
            } else {
                this.whitePlayer = player2;
                this.blackPlayer = player1;
            }
        }

        public String getSessionId() { return sessionId; }

        public void startGame() {
            synchronized (this) {
                if (gameInProgress) {
                    logger.warning("Sesja " + sessionId + ": Próba ponownego rozpoczęcia gry.");
                    return;
                }
                if (whitePlayer == null || !whitePlayer.isClientConnected() || whitePlayer.getCurrentSession() != this ||
                        blackPlayer == null || !blackPlayer.isClientConnected() || blackPlayer.getCurrentSession() != this) {
                    logger.error("Sesja " + sessionId + ": Nie można rozpocząć gry, jeden z graczy nie jest poprawnie przypisany lub połączony.");
                    endSessionAbruptly("Problem ze startem gry - przeciwnik nie jest dostępny.");
                    return;
                }
                gameInProgress = true;
                logger.info("Sesja " + sessionId + ": Ustawiono gameInProgress=true.");
            }

            serverSideBoard.initializeBoard();
            turnStartTimeMillis = System.currentTimeMillis();
            whiteTimeMillis = 0; blackTimeMillis = 0;

            logger.info("Sesja " + sessionId + ": Rozpoczynanie gry. Białe: " + whitePlayer.getPlayerId() + ", Czarne: " + blackPlayer.getPlayerId());

            if (whitePlayer.isClientConnected()) whitePlayer.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_GAME_FOUND, NetworkProtocol.COLOR_WHITE));
            if (blackPlayer.isClientConnected()) blackPlayer.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_GAME_FOUND, NetworkProtocol.COLOR_BLACK));

            new Timer(sessionId + "-StartDelay", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (GameSession.this) {
                        if (!gameInProgress || !whitePlayer.isClientConnected() || !blackPlayer.isClientConnected()) {
                            if (gameInProgress) endSessionAbruptly("Problem z graczem podczas opóźnionego startu.");
                            return;
                        }
                    }
                    logger.info("Sesja " + sessionId + ": Wysyłanie RSP_GAME_STARTED.");
                    broadcastMessage(NetworkProtocol.RSP_GAME_STARTED);
                    startServerSideTimer();
                    sendTimeUpdateToPlayers();
                }
            }, 200);
        }

        private void startServerSideTimer() {
            if (gameLogicTimer != null) gameLogicTimer.cancel();
            gameLogicTimer = new Timer("GameSessionTimer-" + sessionId, true);
            gameLogicTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (gameInProgress) {
                        sendTimeUpdateToPlayers();
                    } else {
                        this.cancel();
                    }
                }
            }, 1000, 1000);
        }

        public synchronized void forwardMove(ClientHandler sender, String clientCommand, String moveData) {
            if (!gameInProgress) {
                if (sender.isClientConnected()) sender.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Gra zakończona."));
                return;
            }
            if (sender != whitePlayer && sender != blackPlayer) {
                if (sender.isClientConnected()) sender.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Błąd sesji."));
                return;
            }

            PlayerColor senderLogicColor = (sender == whitePlayer) ? PlayerColor.WHITE : PlayerColor.BLACK;
            if (serverSideBoard.getCurrentPlayer() != senderLogicColor) {
                sender.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Nie twoja tura (serwer)."));
                logger.warning("Gracz " + sender.getPlayerId() + " (sesja "+sessionId+") próbował ruszyć się nie w swojej turze wg serwera (oczekiwano: " + serverSideBoard.getCurrentPlayer() + ").");
                return;
            }

            int[] coords = NetworkProtocol.parseMoveData(moveData);
            if (coords == null) {
                sender.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Niepoprawny format ruchu."));
                return;
            }
            int fromCol = coords[0], fromRow = coords[1], toCol = coords[2], toRow = coords[3];

            boolean moveValidAndMadeOnServer = serverSideBoard.makeMove(fromRow, fromCol, toRow, toCol);

            if (!moveValidAndMadeOnServer) {
                String errorMsg = serverSideBoard.getLastMoveValidationError() != null ? serverSideBoard.getLastMoveValidationError() : "Nieprawidłowy ruch.";
                sender.sendMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_ERROR, "Serwer: " + errorMsg));
                logger.warning("Serwer odrzucił ruch gracza " + sender.getPlayerId() + ": " + errorMsg);
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (senderLogicColor == PlayerColor.WHITE) {
                whiteTimeMillis += (currentTime - turnStartTimeMillis);
            } else {
                blackTimeMillis += (currentTime - turnStartTimeMillis);
            }

            ClientHandler receiver = (sender == whitePlayer) ? blackPlayer : whitePlayer;
            String commandForOpponent = NetworkProtocol.CMD_MOVE.equals(clientCommand) ? NetworkProtocol.RSP_OPPONENT_MOVE : NetworkProtocol.RSP_OPPONENT_CAPTURE_CONTINUED;

            if (receiver != null && receiver.isClientConnected()) {
                receiver.sendMessage(NetworkProtocol.createMessage(commandForOpponent, moveData));
            } else {
                playerQuit(sender, true);
                return;
            }

            turnStartTimeMillis = System.currentTimeMillis();

            PlayerColor winnerOnServer = serverSideBoard.getWinner();
            if (winnerOnServer != null) {
                gameInProgress = false;
                if (gameLogicTimer != null) gameLogicTimer.cancel();

                String winnerName = (winnerOnServer == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE";
                String winnerMsg = winnerName + " WYGRYWAJĄ!";
                if (serverSideBoard.forcedWinner != null) {
                    PlayerColor loser = (winnerOnServer == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
                    winnerMsg = "Gracz " + ((loser == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE") + " poddał partię. " + winnerMsg;
                }
                logger.info("Sesja " + sessionId + ": Koniec gry! " + winnerMsg);
                broadcastMessage(NetworkProtocol.createMessage(NetworkProtocol.RSP_SESSION_ENDED, winnerMsg));
            } else {
                sendTimeUpdateToPlayers();
            }
        }

        public synchronized void sendTimeUpdateToPlayers() {
            if (!gameInProgress && serverSideBoard.getWinner() == null) {
                return;
            }
            if (!gameInProgress && serverSideBoard.getWinner() != null) {
                return;
            }

            long currentWhiteDisplayTime = whiteTimeMillis;
            long currentBlackDisplayTime = blackTimeMillis;
            long now = System.currentTimeMillis();
            PlayerColor currentTurnPlayerOnServer = serverSideBoard.getCurrentPlayer();


            if (currentTurnPlayerOnServer == PlayerColor.WHITE) {
                currentWhiteDisplayTime += (now - turnStartTimeMillis);
            } else if (currentTurnPlayerOnServer == PlayerColor.BLACK) {
                currentBlackDisplayTime += (now - turnStartTimeMillis);
            }

            String timeMessage = NetworkProtocol.createTimeUpdateMessage(
                    currentWhiteDisplayTime / 1000,
                    currentBlackDisplayTime / 1000,
                    currentTurnPlayerOnServer == PlayerColor.WHITE ? NetworkProtocol.COLOR_WHITE : NetworkProtocol.COLOR_BLACK
            );
            broadcastMessage(timeMessage);
        }

        public synchronized void playerQuit(ClientHandler quitter, boolean dueToDisconnect) {
            if (!gameInProgress && !activeGameSessions.containsKey(sessionId) && activeGameSessions.get(sessionId) != this ) return;

            boolean wasInProgress = gameInProgress;
            gameInProgress = false;
            if (gameLogicTimer != null) {
                gameLogicTimer.cancel();
                gameLogicTimer = null;
            }

            PlayerColor winnerColor = null;
            if (quitter == whitePlayer) winnerColor = PlayerColor.BLACK;
            else if (quitter == blackPlayer) winnerColor = PlayerColor.WHITE;

            String quitReason = dueToDisconnect ? "rozłączył się" : "poddał partię";
            String winnerLogMsg = (winnerColor != null && ((winnerColor == PlayerColor.WHITE && whitePlayer != null && whitePlayer.isClientConnected()) || (winnerColor == PlayerColor.BLACK && blackPlayer != null && blackPlayer.isClientConnected())) )
                    ? "Wygrywa " + (winnerColor == PlayerColor.WHITE ? whitePlayer.getPlayerId() : blackPlayer.getPlayerId())
                    : "Brak zwycięzcy.";
            logger.info("Gracz " + (quitter != null ? quitter.getPlayerId() : "NIEZNANY") + " " + quitReason + " z sesji " + sessionId + ". " + winnerLogMsg);

            ClientHandler remainingPlayer = (quitter == player1) ? player2 : player1;
            if (wasInProgress && remainingPlayer != null && remainingPlayer.isClientConnected()) {
                remainingPlayer.sendMessage(NetworkProtocol.RSP_OPPONENT_QUIT);
            }

            if (quitter != null && quitter.isClientConnected() && !dueToDisconnect) {
                quitter.sendMessage(NetworkProtocol.RSP_SESSION_ENDED);
            }

            activeGameSessions.remove(sessionId);
            if (player1 != null) player1.setCurrentSession(null);
            if (player2 != null) player2.setCurrentSession(null);
        }

        private void endSessionAbruptly(String reasonForClients) {
            if (!gameInProgress && !activeGameSessions.containsKey(sessionId) && activeGameSessions.get(sessionId) != this) return;
            gameInProgress = false;
            if (gameLogicTimer != null) {
                gameLogicTimer.cancel();
                gameLogicTimer = null;
            }

            String msg = NetworkProtocol.createMessage(NetworkProtocol.RSP_SESSION_ENDED, reasonForClients);
            if (player1 != null) {
                if (player1.isClientConnected()) player1.sendMessage(msg);
                player1.setCurrentSession(null);
            }
            if (player2 != null) {
                if (player2.isClientConnected()) player2.sendMessage(msg);
                player2.setCurrentSession(null);
            }
            activeGameSessions.remove(sessionId);
            logger.info("Sesja " + sessionId + " zakończona nagle. Powód: " + reasonForClients);
        }

        private void broadcastMessage(String message) {
            if (whitePlayer != null && whitePlayer.getCurrentSession() == this && whitePlayer.isClientConnected()) whitePlayer.sendMessage(message);
            if (blackPlayer != null && blackPlayer.getCurrentSession() == this && blackPlayer.isClientConnected()) blackPlayer.sendMessage(message);
        }
    }
}