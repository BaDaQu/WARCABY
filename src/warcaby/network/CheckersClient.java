package warcaby.network;

import warcaby.gui.frame.CheckersFrame;
import warcaby.utils.GameConstants;
import warcaby.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CheckersClient {

    private static final Logger logger = new Logger(CheckersClient.class);

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final CheckersFrame frame;
    private volatile boolean connected = false;

    public CheckersClient(CheckersFrame frame) {
        this.frame = frame;
    }

    public boolean connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                logger.info("Zamykanie istniejącego połączenia przed ponowną próbą...");
                disconnect();
                try {
                    Thread.sleep(GameConstants.DISCONNECT_WAIT_TIME);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Przerwano oczekiwanie podczas zamykania starego połączenia.");
                }
            }

            socket = null;
            out = null;
            in = null;
            connected = false;

            logger.info("Próba połączenia z serwerem: " + GameConstants.SERVER_ADDRESS + ":" + GameConstants.SERVER_PORT);
            socket = new Socket(GameConstants.SERVER_ADDRESS, GameConstants.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            Thread receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();

            logger.info("Połączono z serwerem");
            return true;
        } catch (IOException e) {
            logger.error("Nie można połączyć z serwerem: " + e.getMessage(), e);
            disconnect();
            return false;
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && socket != null && !socket.isClosed() && (message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (SocketException e) {
            if (connected) {
                logger.warning("Połączenie z serwerem zostało nieoczekiwanie zamknięte: " + e.getMessage());
                if (frame != null) SwingUtilities.invokeLater(() -> frame.handleServerDisconnection("Połączenie z serwerem przerwane.")); // Dodano domyślny powód
            } else {
                logger.debug("Gniazdo zostało zamknięte (prawdopodobnie celowo).");
            }
        } catch (IOException e) {
            if (connected) {
                logger.error("Błąd podczas odbierania wiadomości od serwera: " + e.getMessage(), e);
                if (frame != null) SwingUtilities.invokeLater(() -> frame.handleServerDisconnection("Błąd We/Wy z serwerem.")); // Dodano domyślny powód
            }
        } finally {
            disconnect();
        }
    }

    private void processServerMessage(String message) {
        logger.debug("Otrzymano od serwera: " + message);
        String[] msgParts = NetworkProtocol.parseMessage(message);
        String action = msgParts[0];
        String data = msgParts.length > 1 ? msgParts[1] : "";

        SwingUtilities.invokeLater(() -> {
            if (frame == null) return;

            switch (action) {
                case NetworkProtocol.RSP_WAITING:
                    logger.debug("Serwer potwierdził oczekiwanie na przeciwnika");
                    break;
                case NetworkProtocol.RSP_GAME_FOUND:
                    if (!data.isEmpty()) {
                        // String color = data; // Zmienna 'color' była redundantna, używamy 'data' bezpośrednio
                        logger.info("Rozpoczynam grę jako kolor: " + data);
                        frame.startOnlineGame(data);
                    }
                    break;
                case NetworkProtocol.RSP_GAME_STARTED:
                    logger.info("Serwer zasygnalizował faktyczny start gry (RSP_GAME_STARTED).");
                    frame.handleGameActuallyStarting();
                    break;
                case NetworkProtocol.RSP_OPPONENT_MOVE:
                    if (!data.isEmpty()) frame.applyOpponentMove(data);
                    break;
                case NetworkProtocol.RSP_OPPONENT_CAPTURE_CONTINUED:
                    if (!data.isEmpty()) frame.applyOpponentCapture(data);
                    break;
                case NetworkProtocol.RSP_TIME_UPDATE:
                    // `parseTimeUpdateMessage` oczekuje tylko części z danymi, a nie całej wiadomości
                    Object[] timeData = NetworkProtocol.parseTimeUpdateMessage(data); // POPRAWKA: przekazuj 'data'
                    if (timeData != null) {
                        frame.updateOnlineGameTime((long) timeData[0], (long) timeData[1], (String) timeData[2]);
                    } else {
                        logger.warning("Nie udało się sparsować wiadomości TIME_UPDATE_DATA: " + data);
                    }
                    break;
                case NetworkProtocol.RSP_SEARCH_CANCELLED:
                    frame.hideWaitingDialog();
                    logger.info("Wyszukiwanie gry anulowane przez serwer.");
                    break;
                case NetworkProtocol.RSP_OPPONENT_QUIT:
                    frame.showOpponentQuitDialog();
                    break;
                case NetworkProtocol.RSP_SESSION_ENDED:
                    logger.info("Sesja gry zakończona przez serwer. Dane: " + data);
                    frame.handleServerDisconnection(data.isEmpty() ? "Sesja zakończona przez serwer." : data);
                    break;
                case NetworkProtocol.RSP_ERROR:
                    logger.error("Błąd od serwera: " + data);
                    JOptionPane.showMessageDialog(frame, "Błąd od serwera: " + data, "Błąd Serwera", JOptionPane.ERROR_MESSAGE);
                    break;
                default:
                    logger.warning("Nieznana wiadomość od serwera: " + message);
            }
        });
    }

    public void findGame() {
        if (connected) {
            sendMessage(NetworkProtocol.CMD_FIND_GAME);
        } else {
            logger.warning("Nie można znaleźć gry - brak połączenia z serwerem.");
        }
    }

    public void cancelSearch() {
        if (connected) {
            sendMessage(NetworkProtocol.CMD_CANCEL_SEARCH);
        }
    }

    public void sendMove(String moveData) {
        if (connected) {
            sendMessage(NetworkProtocol.createMessage(NetworkProtocol.CMD_MOVE, moveData));
        } else {
            logger.warning("Nie można wysłać ruchu - brak połączenia z serwerem.");
        }
    }

    public void sendCaptureContinued(String moveData) {
        if (connected) {
            sendMessage(NetworkProtocol.createMessage(NetworkProtocol.CMD_CAPTURE_CONTINUED, moveData));
        } else {
            logger.warning("Nie można wysłać kontynuacji bicia - brak połączenia z serwerem.");
        }
    }

    public void quitGame() {
        if (connected) {
            sendMessage(NetworkProtocol.CMD_QUIT);
        }
    }

    public void endSession() {
        if (connected) {
            sendMessage(NetworkProtocol.CMD_END_SESSION);
        }
        disconnect();
    }

    public void disconnect() {
        if (!connected && socket == null) {
            return;
        }
        logger.info("Rozłączanie z serwerem...");
        connected = false;

        try {
            if (out != null) out.close();
        } catch (Exception e) { /* ignoruj */ } finally { out = null; }

        try {
            if (in != null) in.close();
        } catch (IOException e) { /* ignoruj */ } finally { in = null; }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignoruj */ } finally { socket = null; }

        logger.info("Rozłączono.");
    }

    private void sendMessage(String message) {
        if (out != null && connected && socket != null && !socket.isOutputShutdown()) {
            out.println(message);
            logger.debug("Wysłano do serwera: " + message);
        } else if (!connected) {
            logger.warning("Próba wysłania wiadomości bez aktywnego połączenia: " + message);
        } else {
            logger.error("Strumień wyjściowy (out) jest null lub gniazdo zamknięte. Nie można wysłać: " + message);
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}