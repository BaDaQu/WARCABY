package warcaby.gui.frame;

import warcaby.gamelogic.Board;
import warcaby.gamelogic.PlayerColor;
import warcaby.gui.BoardPanel;
import warcaby.gui.GameModeSelectionPanel;
import warcaby.gui.InfoPanel;
import warcaby.gui.StartMenuPanel;
import warcaby.network.CheckersClient;
import warcaby.network.NetworkProtocol; // Potrzebny do parsowania danych od serwera
import warcaby.utils.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Główna ramka (okno) aplikacji gry w warcaby.
 * Odpowiada za inicjalizację i zarządzanie głównymi panelami GUI (menu, plansza gry),
 * przełączanie między nimi za pomocą CardLayout, oraz koordynację interakcji
 * między komponentami GUI, logiką gry i klientem sieciowym.
 */
public class CheckersFrame extends JFrame {
    // Managery do obsługi specyficznych zadań
    private FrameViewManager viewManager;       // Zarządza przełączaniem widoków (kart)
    private OnlineGameUIManager onlineManager; // Zarządza logiką UI specyficzną dla gry online

    // Główne panele GUI
    private StartMenuPanel startMenuPanel;
    private GameModeSelectionPanel gameModeSelectionPanel;
    private BoardPanel boardPanel;     // Panel z planszą gry
    private InfoPanel infoPanel;       // Panel z informacjami o grze (tura, czas)

    // Komponenty logiki i sieci
    private Board gameBoard;           // Instancja logiki gry
    private CheckersClient client;     // Klient do komunikacji sieciowej
    private static final Logger logger = new Logger(CheckersFrame.class);

    public CheckersFrame() {
        setTitle("Warcaby");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Umożliwia własną obsługę zamknięcia
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing(); // Logika przy zamykaniu okna
            }
        });

        CardLayout cardLayout = new CardLayout();
        JPanel mainPanelContainer = new JPanel(cardLayout); // Główny kontener z CardLayout

        // Inicjalizacja managerów i głównych komponentów
        viewManager = new FrameViewManager(mainPanelContainer, cardLayout);
        gameBoard = new Board();
        infoPanel = new InfoPanel();
        // BoardPanel potrzebuje referencji do tej ramki (this) dla niektórych akcji (np. powrót do menu)
        boardPanel = new BoardPanel(gameBoard, infoPanel, this);

        client = new CheckersClient(this); // CheckersClient potrzebuje tej ramki do callbacków
        onlineManager = new OnlineGameUIManager(this, client, boardPanel, infoPanel);

        // Tworzenie paneli interfejsu
        startMenuPanel = new StartMenuPanel();
        gameModeSelectionPanel = new GameModeSelectionPanel();

        // Kontener dla ekranu gry, łączący InfoPanel, BoardPanel i przycisk poddania
        JPanel gameScreenPanel = new JPanel(new BorderLayout());
        gameScreenPanel.add(infoPanel, BorderLayout.NORTH);
        gameScreenPanel.add(boardPanel, BorderLayout.CENTER);
        gameScreenPanel.add(boardPanel.getSurrenderButton(), BorderLayout.SOUTH); // Przycisk jest częścią BoardPanel

        // Dodawanie głównych widoków (kart) do managera widoków
        viewManager.addView(startMenuPanel, FrameViewManager.MENU_PANEL_ID);
        viewManager.addView(gameModeSelectionPanel, FrameViewManager.MODE_SELECTION_PANEL_ID);
        viewManager.addView(gameScreenPanel, FrameViewManager.GAME_PANEL_CONTAINER_ID);

        add(mainPanelContainer); // Dodanie głównego kontenera do ramki

        setupActionListeners(); // Ustawienie reakcji na przyciski menu

        pack(); // Dopasowanie rozmiaru okna do zawartości
        setMinimumSize(getSize()); // Ustawienie minimalnego rozmiaru okna
        setLocationRelativeTo(null); // Wyśrodkowanie okna
        setResizable(false); // Zablokowanie możliwości zmiany rozmiaru okna

        viewManager.showStartMenu(); // Wyświetlenie menu startowego przy uruchomieniu
    }

    /**
     * Konfiguruje ActionListener'y dla przycisków w panelach menu.
     */
    private void setupActionListeners() {
        // Menu Główne
        startMenuPanel.addStartGameListener(e -> viewManager.showGameModeSelectionPanel());
        startMenuPanel.addInstructionsListener(e -> showInstructionsDialog());
        startMenuPanel.addExitListener(e -> handleWindowClosing());

        // Menu Wyboru Trybu Gry
        gameModeSelectionPanel.addLocalMultiplayerListener(e -> {
            onlineManager.resetOnlineState(); // Upewnij się, że tryb online jest wyłączony
            if (boardPanel != null) {
                boardPanel.setOnlineGameMode(false, null);
                boardPanel.setComputerGameMode(false);
            }
            viewManager.showGamePanelContainer(); // Pokaż panel gry
            if (boardPanel != null) boardPanel.resetGame(); // Zresetuj grę dla trybu lokalnego
        });
        gameModeSelectionPanel.addLocalVsComputerListener(e -> {
            onlineManager.resetOnlineState();
            if (boardPanel != null) {
                boardPanel.setOnlineGameMode(false, null);
                boardPanel.setComputerGameMode(true); // Włącz tryb gry z komputerem
            }
            viewManager.showGamePanelContainer();
            if (boardPanel != null) boardPanel.resetGame();
        });
        gameModeSelectionPanel.addOnlineMultiplayerListener(e -> {
            if (boardPanel != null) boardPanel.setComputerGameMode(false); // Wyłącz AI przed grą online
            onlineManager.initiateOnlineGameSearch(); // Rozpocznij proces gry online
        });
        gameModeSelectionPanel.addBackToMainMenuListener(e -> {
            onlineManager.resetOnlineState();
            if (client.isConnected() && (onlineManager.isWaitingDialogVisible() || onlineManager.isOnlineGameActive())) {
                client.cancelSearch(); // Jeśli szukał lub był w grze online, spróbuj anulować/zakończyć
            }
            viewManager.showStartMenu(); // Wróć do menu głównego (co powinno też rozłączyć klienta)
        });
    }

    /**
     * Obsługuje zdarzenie zamknięcia okna aplikacji.
     * Zapewnia poprawne zakończenie sesji sieciowej i rozłączenie klienta.
     */
    private void handleWindowClosing() {
        logger.info("Obsługa zamknięcia okna (CheckersFrame)...");
        if (onlineManager.isOnlineGameActive() && client != null && client.isConnected()) {
            logger.info("Wysyłanie CMD_END_SESSION do serwera (CheckersFrame).");
            client.endSession(); // Poinformuj serwer o końcu sesji
        } else if (client != null && client.isConnected()) { // Jeśli tylko połączony, ale nie w grze (np. szuka)
            client.cancelSearch(); // Anuluj ewentualne wyszukiwanie
            client.endSession();   // Zakończ sesję z serwerem
        } else if (client != null) { // Jeśli klient istnieje, ale nie jest połączony
            client.disconnect(); // Upewnij się, że zasoby klienta są zwolnione
        }
        // Krótkie opóźnienie, aby dać czas na wysłanie wiadomości sieciowych
        try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        System.exit(0); // Zamknij aplikację
    }

    // Gettery dla dostępu z innych klas (np. OnlineGameUIManager, BoardPanel)
    public FrameViewManager getViewManager() { return viewManager; }
    public Board getGameBoard() { return gameBoard; }
    public CheckersClient getClient() { return client; }

    // Metody wywoływane przez CheckersClient jako callbacki po otrzymaniu wiadomości od serwera
    public void startOnlineGame(String colorStringFromServer) {
        onlineManager.processGameFound(colorStringFromServer);
    }
    public void handleGameActuallyStarting() {
        onlineManager.processGameActuallyStarting();
    }

    /**
     * Stosuje ruch przeciwnika otrzymany od serwera na lokalnej planszy.
     * Ta metoda jest wywoływana w wątku EDT przez CheckersClient.
     * @param moveData String reprezentujący ruch (np. "fromCol,fromRow->toCol,toRow").
     */
    public void applyOpponentMove(String moveData) {
        if (!onlineManager.isOnlineGameActive()) return; // Działaj tylko w grze online
        SwingUtilities.invokeLater(() -> { // Upewnij się, że operacje na GUI/stanie gry są w EDT
            try {
                int[] coords = NetworkProtocol.parseMoveData(moveData);
                if (coords == null) {
                    logger.error("Błąd parsowania danych ruchu przeciwnika (CheckersFrame): " + moveData);
                    return;
                }
                int fromCol = coords[0]; int fromRow = coords[1];
                int toCol = coords[2]; int toRow = coords[3];

                logger.info("CheckersFrame (applyOpponentMove): Otrzymano ruch przeciwnika od serwera: (" + fromRow + "," + fromCol + ") -> (" + toRow + "," + toCol + ")");

                PlayerColor opponentColor = (onlineManager.getMyOnlineSide() == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
                // Przed wykonaniem ruchu przeciwnika, upewnij się, że lokalna logika gry (gameBoard)
                // jest ustawiona na turę tego przeciwnika. To jest synchronizowane przez RSP_TIME_UPDATE.
                if (gameBoard.getCurrentPlayer() != opponentColor) {
                    logger.info("CheckersFrame (applyOpponentMove): Wymuszanie tury na " + opponentColor + " przed zastosowaniem ruchu przeciwnika (lokalnie było " + gameBoard.getCurrentPlayer() + ").");
                    gameBoard.forceSetCurrentPlayerFromServer(opponentColor); // Kluczowe dla synchronizacji
                }

                if (gameBoard.makeMove(fromRow, fromCol, toRow, toCol)) {
                    logger.info("CheckersFrame: Ruch przeciwnika (" + fromRow + "," + fromCol + ")->(" + toRow + "," + toCol + ") zastosowany. Nowa tura lokalnie: " + gameBoard.getCurrentPlayer());
                    if (boardPanel != null) {
                        boardPanel.repaint(); // Odśwież widok planszy
                        boardPanel.updateMandatoryJumpStatus(); // Zaktualizuj podświetlanie obowiązkowych bić
                    }
                    // InfoPanel (tura i czas) zostanie zaktualizowany przez następny komunikat RSP_TIME_UPDATE od serwera.
                } else {
                    logger.error("Nie udało się zastosować ruchu przeciwnika (CheckersFrame) na lokalnej planszy: " + gameBoard.getLastMoveValidationError());
                    // TODO: Rozważ mechanizm resynchronizacji z serwerem w przypadku krytycznego błędu.
                }
            } catch (Exception e) {
                logger.error("Błąd krytyczny podczas stosowania ruchu przeciwnika (CheckersFrame): " + moveData, e);
            }
        });
    }

    public void applyOpponentCapture(String moveData) {
        applyOpponentMove(moveData); // Na razie ta sama logika, serwer rozróżnia typ ruchu
    }

    public void showOpponentQuitDialog() {
        onlineManager.processOpponentQuit();
    }

    /**
     * Obsługuje rozłączenie od serwera lub zakończenie sesji przez serwer.
     * Wyświetla odpowiedni komunikat i wraca do menu głównego.
     * @param reason Powód zakończenia/rozłączenia (może zawierać informację o wyniku gry).
     */
    public void handleServerDisconnection(String reason) {
        logger.info("CheckersFrame: Obsługa rozłączenia z serwerem. Powód: " + reason);
        SwingUtilities.invokeLater(() -> {
            boolean wasOnline = onlineManager.isOnlineGameActive();
            boolean wasWaiting = onlineManager.isWaitingDialogVisible();

            onlineManager.resetOnlineState(); // Resetuje flagi i dialogi związane z grą online

            // Wyświetl odpowiedni komunikat w zależności od przyczyny
            if (reason != null && (reason.toUpperCase().contains("WYGRYWAJĄ") || reason.toUpperCase().contains("WYGRAŁ") || reason.toUpperCase().contains("REMIS"))) {
                JOptionPane.showMessageDialog(this, reason, "Koniec Gry Online", JOptionPane.INFORMATION_MESSAGE);
            } else if (wasOnline) {
                JOptionPane.showMessageDialog(this, "Połączenie z serwerem zostało przerwane. " + (reason != null ? reason : ""), "Koniec Sesji", JOptionPane.WARNING_MESSAGE);
            } else if (wasWaiting) {
                JOptionPane.showMessageDialog(this, "Połączenie z serwerem przerwane podczas wyszukiwania. " + (reason != null ? reason : ""), "Błąd Połączenia", JOptionPane.ERROR_MESSAGE);
            }

            if (client != null && client.isConnected()) { // Rozłącz klienta, jeśli wciąż jest połączony
                client.disconnect();
            }
            viewManager.showStartMenu(); // Zawsze wracaj do menu głównego
        });
    }

    /**
     * Aktualizuje czas gry i informację o turze na podstawie danych z serwera.
     * Wywoływane przez CheckersClient po otrzymaniu wiadomości RSP_TIME_UPDATE.
     */
    public void updateOnlineGameTime(long whiteSeconds, long blackSeconds, String whoseTurnString) {
        if (onlineManager != null && onlineManager.isOnlineGameActive()) {
            onlineManager.processTimeUpdate(whiteSeconds, blackSeconds, whoseTurnString);
        } else if (onlineManager == null) {
            logger.error("onlineManager jest null w updateOnlineGameTime!");
        }
    }

    /**
     * Ukrywa dialog oczekiwania na przeciwnika. Wywoływane przez CheckersClient.
     */
    public void hideWaitingDialog() {
        if (onlineManager != null) onlineManager.hideWaitingDialog();
    }

    /**
     * Pokazuje menu startowe, resetując stan gry online i rozłączając klienta.
     */
    public void showStartMenu() {
        logger.info("Wyświetlanie menu startowego (CheckersFrame).");
        if (boardPanel != null && boardPanel.gameTimer != null && boardPanel.gameTimer.isRunning()) {
            boardPanel.stopGameTime(); // Zatrzymaj lokalny timer gry
        }
        if (onlineManager != null) onlineManager.resetOnlineState(); // Zresetuj stan UI gry online
        if (client != null && client.isConnected()) {
            logger.info("Rozłączanie klienta przy powrocie do menu (CheckersFrame).");
            client.disconnect(); // Rozłącz klienta sieciowego
        }
        viewManager.showStartMenu(); // Pokaż panel menu startowego
    }


    private void showInstructionsDialog() {
        String instructions = "Instrukcja Gry w Warcaby:\n\n" +
                "1. Gracze wykonują ruchy na przemian, zaczynają białe.\n" +
                "2. Pionki (M) poruszają się o jedno pole do przodu po przekątnej na wolne ciemne pola.\n" +
                "3. Bicie pionkiem (M) jest możliwe do przodu i do tyłu przez przeskoczenie pionka przeciwnika\n" +
                "   na wolne pole bezpośrednio za nim. Bicie jest obowiązkowe.\n" +
                "4. Jeśli po biciu można wykonać kolejne bicie tym samym pionkiem, należy je wykonać (bicie wielokrotne).\n" +
                "5. Pionek, który dotrze do ostatniego rzędu planszy po stronie przeciwnika, staje się damką (K).\n" +
                "6. Damka (K) porusza się o dowolną liczbę pól po przekątnej (do przodu i do tyłu) po wolnych polach.\n" +
                "7. Damka (K) bije przez przeskoczenie pionka przeciwnika na dowolne wolne pole za nim na tej samej przekątnej.\n" +
                "   Wszystkie pola między damką a bitym pionkiem muszą być wolne. Bicie jest obowiązkowe.\n" +
                "8. Wygrywa gracz, który zbije wszystkie pionki przeciwnika lub zablokuje wszystkie jego możliwe ruchy.\n" +
                "9. Pomiar czasu: Lokalnie dla każdego gracza. W grze online czas synchronizowany z serwerem.\n\n" +
                "Miłej gry!";
        JOptionPane.showMessageDialog(this, instructions, "Instrukcja Gry", JOptionPane.INFORMATION_MESSAGE);
    }
}