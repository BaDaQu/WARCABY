package warcaby.gui.frame;

import warcaby.gui.BoardPanel;
import warcaby.gui.InfoPanel;
import warcaby.gamelogic.Board; // Potrzebny do interakcji z logiką gry
import warcaby.gamelogic.PlayerColor;
import warcaby.network.CheckersClient;
import warcaby.network.NetworkProtocol;
import warcaby.utils.Logger;

import javax.swing.*;
import java.awt.*; // Dla BorderLayout i JLabel, JProgressBar

/**
 * Klasa OnlineGameUIManager zarządza interfejsem użytkownika i logiką
 * specyficzną dla trybu gry online. Odpowiada za inicjowanie połączenia,
 * wyświetlanie dialogów (łączenia, oczekiwania na przeciwnika),
 * przetwarzanie informacji zwrotnych od serwera (np. o znalezieniu gry,
 * ruchu przeciwnika, końcu gry) i aktualizowanie odpowiednich komponentów GUI.
 */
public class OnlineGameUIManager {
    private final CheckersFrame mainFrame;     // Referencja do głównej ramki aplikacji (dla kontekstu i dialogów)
    private final CheckersClient client;       // Instancja klienta sieciowego do komunikacji z serwerem
    private final BoardPanel boardPanel;       // Referencja do panelu planszy gry
    private final InfoPanel infoPanel;         // Referencja do panelu informacji o grze
    private JDialog waitingDialog = null;      // Dialog wyświetlany podczas oczekiwania na przeciwnika
    private static final Logger logger = new Logger(OnlineGameUIManager.class);

    private boolean isCurrentlyOnlineGame = false; // Flaga wskazująca, czy aktualnie trwa gra online
    private PlayerColor myOnlineSide = null;       // Kolor pionków przypisany graczowi w grze online

    public OnlineGameUIManager(CheckersFrame mainFrame, CheckersClient client, BoardPanel boardPanel, InfoPanel infoPanel) {
        this.mainFrame = mainFrame;
        this.client = client;
        this.boardPanel = boardPanel;
        this.infoPanel = infoPanel;
    }

    /**
     * Sprawdza, czy aktualnie toczy się gra online.
     * @return true, jeśli gra online jest aktywna.
     */
    public boolean isOnlineGameActive() {
        return isCurrentlyOnlineGame;
    }

    /**
     * Sprawdza, czy dialog oczekiwania na przeciwnika jest aktualnie widoczny.
     * @return true, jeśli dialog jest widoczny.
     */
    public boolean isWaitingDialogVisible() { return waitingDialog != null && waitingDialog.isVisible(); }

    /**
     * Zwraca kolor pionków przypisany graczowi w bieżącej grze online.
     * @return PlayerColor gracza lub null, jeśli nie jest w grze online.
     */
    public PlayerColor getMyOnlineSide() {
        return myOnlineSide;
    }

    /**
     * Inicjuje proces wyszukiwania gry online.
     * Wyświetla dialog łączenia, próbuje połączyć się z serwerem,
     * a następnie (jeśli połączenie udane) wyświetla dialog oczekiwania na przeciwnika
     * i wysyła żądanie znalezienia gry do serwera.
     */
    public void initiateOnlineGameSearch() {
        if (isCurrentlyOnlineGame || (waitingDialog != null && waitingDialog.isVisible())) {
            JOptionPane.showMessageDialog(mainFrame, "Już jesteś w trakcie gry online lub wyszukiwania.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog connectingDialog = new JDialog(mainFrame, "Łączenie z serwerem", false);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Próba połączenia z serwerem...", SwingConstants.CENTER), BorderLayout.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);
        connectingDialog.add(panel);
        connectingDialog.pack();
        connectingDialog.setLocationRelativeTo(mainFrame);
        connectingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return client.connectToServer();
            }

            @Override
            protected void done() {
                if (!connectingDialog.isVisible() && !mainFrame.isActive()) return;
                connectingDialog.dispose();
                try {
                    if (get()) {
                        displayWaitingForOpponentDialog();
                        client.findGame();
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "Nie można połączyć z serwerem.", "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
                        mainFrame.getViewManager().showGameModeSelectionPanel();
                    }
                } catch (Exception ex) {
                    logger.error("Błąd połączenia w SwingWorker (OnlineGameUIManager): ", ex);
                    if (mainFrame.isActive()) {
                        JOptionPane.showMessageDialog(mainFrame, "Błąd połączenia: " + ex.getMessage(), "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
                        mainFrame.getViewManager().showGameModeSelectionPanel();
                    }
                }
            }
        };
        worker.execute();
        SwingUtilities.invokeLater(() -> connectingDialog.setVisible(true));
    }

    /**
     * Wyświetla (niemodalny) dialog informujący o oczekiwaniu na przeciwnika.
     * Zawiera przycisk "Anuluj wyszukiwanie".
     */
    public void displayWaitingForOpponentDialog() {
        hideWaitingDialog(); // Zamknij poprzedni, jeśli istnieje
        waitingDialog = new JDialog(mainFrame, "Oczekiwanie na przeciwnika...", false); // Niemodalny, aby przycisk Anuluj działał
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Wyszukiwanie przeciwnika...", SwingConstants.CENTER), BorderLayout.CENTER);
        JButton cancelButton = new JButton("Anuluj wyszukiwanie");
        cancelButton.addActionListener(e -> {
            logger.info("Gracz kliknął Anuluj wyszukiwanie (OnlineGameUIManager).");
            if (client != null && client.isConnected()) {
                client.cancelSearch(); // Wyślij CMD_CANCEL_SEARCH
            }
            // Nie ukrywaj dialogu tutaj od razu. Serwer powinien odpowiedzieć RSP_SEARCH_CANCELLED,
            // które wywoła CheckersFrame.hideWaitingDialog() -> this.hideWaitingDialog().
        });
        panel.add(cancelButton, BorderLayout.SOUTH);
        waitingDialog.add(panel);
        waitingDialog.setModal(false); // Upewnij się, że jest niemodalny
        waitingDialog.pack();
        waitingDialog.setLocationRelativeTo(mainFrame);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Użytkownik musi użyć przycisku
        waitingDialog.setVisible(true);
    }

    /**
     * Ukrywa i zamyka dialog oczekiwania na przeciwnika, jeśli jest widoczny.
     */
    public void hideWaitingDialog() {
        if (waitingDialog != null) {
            waitingDialog.setVisible(false);
            waitingDialog.dispose();
            waitingDialog = null;
            logger.info("Dialog oczekiwania ukryty/anulowany (OnlineGameUIManager).");
        }
    }

    /**
     * Przetwarza informację od serwera o znalezieniu gry i przypisaniu koloru.
     * Ustawia flagi gry online, kolor gracza, informuje BoardPanel i pokazuje panel gry.
     * @param colorStringFromServer String ("WHITE" lub "BLACK") otrzymany od serwera.
     */
    public void processGameFound(String colorStringFromServer) {
        hideWaitingDialog(); // Najpierw ukryj dialog oczekiwania
        isCurrentlyOnlineGame = true;
        if (NetworkProtocol.COLOR_WHITE.equals(colorStringFromServer)) {
            myOnlineSide = PlayerColor.WHITE;
        } else if (NetworkProtocol.COLOR_BLACK.equals(colorStringFromServer)) {
            myOnlineSide = PlayerColor.BLACK;
        } else {
            logger.error("Nieznany kolor od serwera: " + colorStringFromServer);
            isCurrentlyOnlineGame = false; // Błąd, nie jesteśmy w grze online
            if (client != null) client.disconnect();
            JOptionPane.showMessageDialog(mainFrame, "Błąd konfiguracji gry. Nieznany kolor gracza.", "Błąd Gry Online", JOptionPane.ERROR_MESSAGE);
            mainFrame.getViewManager().showGameModeSelectionPanel(); // Wróć do wyboru trybu
            return;
        }

        logger.info("OnlineGameUIManager: Otrzymano GAME_FOUND. Kolor gracza: " + myOnlineSide);
        if (boardPanel != null) {
            boardPanel.setOnlineGameMode(true, myOnlineSide); // Poinformuj BoardPanel o trybie
        }
        mainFrame.getViewManager().showGamePanelContainer(); // Pokaż panel z planszą
        if (boardPanel != null) {
            boardPanel.resetGame(); // Zresetuj grę (planszę, timery lokalne)
        }
        JOptionPane.showMessageDialog(mainFrame, "Gra znaleziona! Grasz jako " + (myOnlineSide == PlayerColor.WHITE ? "BIAŁE" : "CZARNE") + ". Oczekiwanie na start...", "Gra Znaleziona", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Przetwarza informację od serwera o faktycznym rozpoczęciu gry (po odliczeniu lub potwierdzeniu).
     * Może odblokować interakcje na planszy lub wyświetlić komunikat "START!".
     */
    public void processGameActuallyStarting() {
        logger.info("OnlineGameUIManager: Serwer potwierdził RSP_GAME_STARTED.");
        if (isCurrentlyOnlineGame && boardPanel != null) {
            JOptionPane.showMessageDialog(mainFrame, "START!", "Gra Rozpoczęta", JOptionPane.INFORMATION_MESSAGE);
            boardPanel.repaint(); // Odśwież planszę
            // Tutaj można by odblokować interakcje na BoardPanel, jeśli były zablokowane
        }
    }

    /**
     * Obsługuje sytuację, gdy przeciwnik opuścił grę online.
     * Wyświetla stosowny komunikat i wraca do menu głównego.
     */
    public void processOpponentQuit() {
        if (!isCurrentlyOnlineGame) return;
        isCurrentlyOnlineGame = false; // Koniec gry online
        myOnlineSide = null;
        if (boardPanel != null) boardPanel.stopGameTime(); // Zatrzymaj lokalny timer gry
        JOptionPane.showMessageDialog(mainFrame, "Przeciwnik opuścił grę. Wygrałeś!", "Koniec Gry", JOptionPane.INFORMATION_MESSAGE);
        mainFrame.getViewManager().showStartMenu(); // Przejdź do menu głównego (co powinno też rozłączyć klienta)
    }

    /**
     * Obsługuje nieoczekiwane rozłączenie od serwera lub zakończenie sesji z innego powodu.
     * Wyświetla komunikat i wraca do odpowiedniego menu.
     * @param reason Tekstowy powód rozłączenia/zakończenia sesji (może zawierać wynik gry).
     */
    public void processServerDisconnection(String reason) {
        logger.info("OnlineGameUIManager: Obsługa rozłączenia z serwerem. Powód: " + reason);
        boolean wasOnline = isCurrentlyOnlineGame;
        boolean wasWaiting = (waitingDialog != null && waitingDialog.isVisible());

        isCurrentlyOnlineGame = false; // Zawsze resetuj stan gry online
        myOnlineSide = null;
        if (boardPanel != null) boardPanel.stopGameTime();
        hideWaitingDialog(); // Ukryj dialog oczekiwania, jeśli był widoczny

        // Wyświetl odpowiedni komunikat na podstawie powodu
        if (reason != null && (reason.toUpperCase().contains("WYGRYWAJĄ") || reason.toUpperCase().contains("WYGRAŁ") || reason.toUpperCase().contains("REMIS") || reason.toUpperCase().contains("PODDAŁ"))) {
            JOptionPane.showMessageDialog(mainFrame, reason, "Koniec Gry Online", JOptionPane.INFORMATION_MESSAGE);
        } else if (wasOnline) { // Jeśli byliśmy w trakcie gry online
            JOptionPane.showMessageDialog(mainFrame, "Połączenie z serwerem zostało przerwane. " + (reason != null && !reason.isEmpty() ? reason : ""), "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
        } else if (wasWaiting) { // Jeśli oczekiwaliśmy na grę
            JOptionPane.showMessageDialog(mainFrame, "Połączenie z serwerem przerwane podczas wyszukiwania. " + (reason != null && !reason.isEmpty() ? reason : ""), "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
        } else if (reason != null && !reason.isEmpty()){ // Inny powód od serwera, gdy nie byliśmy w grze ani nie czekaliśmy
            JOptionPane.showMessageDialog(mainFrame, reason, "Informacja od Serwera", JOptionPane.INFORMATION_MESSAGE);
        }
        // CheckersFrame.showStartMenu() (wywołane przez handleServerDisconnection w CheckersFrame) zajmie się rozłączeniem klienta.
    }

    /**
     * Aktualizuje wyświetlany czas gry i informację o turze na podstawie danych z serwera.
     * Wywołuje również `gameBoard.forceSetCurrentPlayerFromServer` w celu synchronizacji
     * lokalnego stanu tury z serwerem.
     * @param whiteSeconds Całkowity czas gry białych w sekundach.
     * @param blackSeconds Całkowity czas gry czarnych w sekundach.
     * @param whoseTurnString String ("WHITE" lub "BLACK") wskazujący, czyja jest tura.
     */
    public void processTimeUpdate(long whiteSeconds, long blackSeconds, String whoseTurnString) {
        if (!isCurrentlyOnlineGame) return;
        // Ta metoda powinna być wywoływana w wątku EDT (SwingUtilities.invokeLater w CheckersFrame)
        infoPanel.updateWhiteTime(whiteSeconds);
        infoPanel.updateBlackTime(blackSeconds);
        PlayerColor serversCurrentPlayer = NetworkProtocol.COLOR_WHITE.equals(whoseTurnString) ? PlayerColor.WHITE : PlayerColor.BLACK;
        infoPanel.updatePlayerInfo(serversCurrentPlayer);

        Board gameBoard = mainFrame.getGameBoard(); // Pobierz instancję Board z CheckersFrame
        // Synchronizuj lokalny stan tury z autorytatywnym stanem serwera
        if (gameBoard.getCurrentPlayer() != serversCurrentPlayer) {
            logger.info("OnlineGameUIManager: Synchronizacja tury z serwerem: lokalnie " + gameBoard.getCurrentPlayer() + ", serwer " + serversCurrentPlayer +". Wymuszanie zmiany.");
            gameBoard.forceSetCurrentPlayerFromServer(serversCurrentPlayer); // Użyj metody z Board
        }
        if (boardPanel != null) {
            boardPanel.updateMandatoryJumpStatus(); // Zaktualizuj podświetlanie obowiązkowych bić
            boardPanel.repaint(); // Odśwież planszę
        }
    }

    /**
     * Resetuje stan managera związany z grą online (flagi, dialogi).
     * Wywoływane przy powrocie do menu lub starcie nowej gry nie-online.
     */
    public void resetOnlineState() {
        isCurrentlyOnlineGame = false;
        myOnlineSide = null;
        hideWaitingDialog(); // Upewnij się, że dialog oczekiwania jest zamknięty
        if (boardPanel != null) {
            boardPanel.setOnlineGameMode(false, null); // Poinformuj BoardPanel o zmianie trybu
        }
    }
}