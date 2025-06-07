package warcaby.gamelogic;

import warcaby.gamelogic.boardcomponents.*; // Import komponentów logiki planszy
import warcaby.utils.Logger;             // Import loggera
import java.util.List;
import java.util.ArrayList;

/**
 * Główna klasa logiki gry w warcaby. Działa jako fasada, koordynując działanie
 * mniejszych komponentów odpowiedzialnych za stan planszy, zarządzanie turą,
 * logikę ruchów, wykonywanie ruchów i sprawdzanie statusu gry.
 * Udostępnia interfejs dla warstwy GUI lub sieciowej do interakcji z logiką gry.
 */
public class Board {
    public static final int SIZE = BoardState.SIZE; // Publiczna stała rozmiaru planszy, pobierana z BoardState

    // Prywatne instancje komponentów logiki
    private BoardState boardState;
    private TurnManager turnManager;
    private MoveLogic moveLogic;
    private MoveExecutor moveExecutor;
    private GameStatusChecker gameStatusChecker;

    private String lastMoveValidationError = null; // Przechowuje komunikat o ostatnim błędzie walidacji ruchu
    public PlayerColor forcedWinner = null;      // Umożliwia wymuszenie zwycięzcy (np. przez poddanie się)
    private static final Logger logger = new Logger(Board.class); // Logger dla tej klasy

    public Board() {
        // Inicjalizacja wszystkich komponentów logiki
        this.boardState = new BoardState();
        this.turnManager = new TurnManager(); // Upewnij się, że TurnManager ma metodę forceSetCurrentPlayer
        this.moveLogic = new MoveLogic(boardState, turnManager);
        this.moveExecutor = new MoveExecutor(boardState, turnManager, moveLogic);
        this.gameStatusChecker = new GameStatusChecker(boardState, turnManager, moveLogic); // Przekazuj zależności
        initializeBoard(); // Inicjalizacja stanu początkowego planszy
    }

    /**
     * Inicjalizuje planszę do stanu początkowego nowej gry.
     * Resetuje stan komponentów planszy, tury, błędów walidacji i wymuszonego zwycięzcy.
     */
    public void initializeBoard() {
        boardState.initializeState();
        turnManager.reset();
        lastMoveValidationError = null;
        forcedWinner = null;
    }

    /**
     * Zwraca pionka znajdującego się na podanych koordynatach.
     * @param row Wiersz.
     * @param col Kolumna.
     * @return Obiekt Piece lub null, jeśli pole jest puste lub koordynaty są nieprawidłowe.
     */
    public Piece getPiece(int row, int col) {
        return boardState.getPiece(row, col);
    }

    /**
     * Zwraca kolor gracza, którego jest aktualnie tura.
     * @return PlayerColor aktualnego gracza.
     */
    public PlayerColor getCurrentPlayer() {
        return turnManager.getCurrentPlayer();
    }

    /**
     * Wymusza ustawienie aktualnego gracza. Używane głównie do synchronizacji
     * z serwerem w grze online, aby stan lokalny odpowiadał stanowi na serwerze.
     * @param serverPlayer Kolor gracza, który ma teraz turę według serwera.
     */
    public void forceSetCurrentPlayerFromServer(PlayerColor serverPlayer) {
        if (turnManager != null) {
            turnManager.forceSetCurrentPlayer(serverPlayer);
        } else {
            logger.error("TurnManager nie został zainicjalizowany w Board!");
        }
    }

    /**
     * Sprawdza, czy w obecnej turze wykonano już bicie (istotne dla wielokrotnych bić).
     * @return true, jeśli w tej turze wykonano bicie.
     */
    public boolean isJumpMadeThisTurn() {
        return turnManager.isJumpMadeThisTurn();
    }

    /**
     * Zwraca wiersz ostatniego pionka, który wykonał bicie w tej turze.
     * @return Wiersz pionka lub -1, jeśli nie było bicia.
     */
    public int getLastJumpingPieceRow() {
        return turnManager.getLastJumpingPieceRow();
    }

    /**
     * Zwraca kolumnę ostatniego pionka, który wykonał bicie w tej turze.
     * @return Kolumna pionka lub -1, jeśli nie było bicia.
     */
    public int getLastJumpingPieceCol() {
        return turnManager.getLastJumpingPieceCol();
    }

    /**
     * Sprawdza, czy podane koordynaty znajdują się w granicach planszy.
     * @param row Wiersz.
     * @param col Kolumna.
     * @return true, jeśli koordynaty są prawidłowe.
     */
    public boolean isValidCoordinate(int row, int col) {
        return boardState.isValidCoordinate(row, col);
    }

    /**
     * Zwraca komunikat o ostatnim błędzie walidacji ruchu.
     * Po odczytaniu, komunikat jest resetowany do null.
     * @return String z opisem błędu lub null.
     */
    public String getLastMoveValidationError() {
        String error = lastMoveValidationError;
        lastMoveValidationError = null;
        return error;
    }

    /**
     * Główna metoda do wykonania ruchu. Najpierw sprawdza, czy gra nie jest zakończona.
     * Następnie waliduje ruch, uwzględniając zasady gry, takie jak przynależność pionka
     * do aktualnego gracza, poprawność pola docelowego, reguły obowiązkowego bicia
     * oraz kontynuacji bicia. Jeśli ruch jest prawidłowy, wykonuje go na planszy
     * za pomocą komponentu MoveExecutor.
     *
     * @param fromRow Wiersz startowy ruchu.
     * @param fromCol Kolumna startowa ruchu.
     * @param toRow Wiersz docelowy ruchu.
     * @param toCol Kolumna docelowa ruchu.
     * @return true, jeśli ruch został poprawnie wykonany i zaaplikowany na planszy,
     *         false w przeciwnym razie (np. ruch nieprawidłowy, gra zakończona).
     */
    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (getWinner() != null) {
            lastMoveValidationError = "Gra została już zakończona.";
            logger.warning("Board.makeMove: Próba ruchu w zakończonej grze. Zwycięzca: " + getWinner());
            return false;
        }
        lastMoveValidationError = null;

        Piece piece = boardState.getPiece(fromRow, fromCol);
        if (piece == null) {
            lastMoveValidationError = "Na wybranym polu ("+fromRow+","+fromCol+") nie ma pionka.";
            logger.warning("Board.makeMove: " + lastMoveValidationError);
            return false;
        }

        PlayerColor actualCurrentPlayer = getCurrentPlayer();
        if (piece.getColor() != actualCurrentPlayer) {
            lastMoveValidationError = "To nie jest pionek gracza, którego jest aktualnie tura (" + actualCurrentPlayer + "). Próba ruchu pionkiem " + piece.getColor() + " z ("+fromRow+","+fromCol+").";
            logger.warning("Board.makeMove: " + lastMoveValidationError);
            return false;
        }
        if (!boardState.isValidCoordinate(toRow, toCol) || boardState.getPiece(toRow, toCol) != null) {
            lastMoveValidationError = "Pole docelowe ("+toRow+","+toCol+") jest nieprawidłowe lub zajęte.";
            logger.warning("Board.makeMove: " + lastMoveValidationError);
            return false;
        }

        List<Move> allPossibleJumps = moveLogic.getAllPossibleJumpsForPlayer(actualCurrentPlayer);
        boolean mandatoryJumpExists = !allPossibleJumps.isEmpty();
        boolean isAttemptedMoveAJump = moveLogic.isJump(piece, fromRow, fromCol, toRow, toCol);

        if (turnManager.isJumpMadeThisTurn()) {
            if (fromRow != turnManager.getLastJumpingPieceRow() || fromCol != turnManager.getLastJumpingPieceCol()) {
                lastMoveValidationError = "Musisz kontynuować bicie pionkiem z pozycji (" + turnManager.getLastJumpingPieceRow() + "," + turnManager.getLastJumpingPieceCol() + ").";
                logger.warning("Board.makeMove: " + lastMoveValidationError);
                return false;
            }
            if (!isAttemptedMoveAJump) {
                lastMoveValidationError = "Po biciu, jeśli to możliwe, następny ruch musi być biciem.";
                logger.warning("Board.makeMove: " + lastMoveValidationError);
                return false;
            }
        } else if (mandatoryJumpExists) {
            if (!isAttemptedMoveAJump) {
                lastMoveValidationError = "Obowiązkowe bicie! Wybierz pionka i wykonaj bicie.";
                logger.warning("Board.makeMove: " + lastMoveValidationError);
                return false;
            }
            boolean isThisSpecificJumpAllowed = allPossibleJumps.stream()
                    .anyMatch(jump -> jump.fromRow == fromRow && jump.fromCol == fromCol && jump.toRow == toRow && jump.toCol == toCol);
            if (!isThisSpecificJumpAllowed) {
                lastMoveValidationError = "Wybrane bicie nie jest jednym z dostępnych obowiązkowych bić.";
                logger.warning("Board.makeMove: " + lastMoveValidationError);
                return false;
            }
        }

        // Użyj isValidMoveInternal(..., false), ponieważ reguły obowiązkowości są już sprawdzone powyżej
        if (!moveLogic.isValidMoveInternal(fromRow, fromCol, toRow, toCol, false)) {
            if (lastMoveValidationError == null) { // Ustaw domyślny błąd, jeśli inny nie został ustawiony
                lastMoveValidationError = "Nieprawidłowy ruch dla wybranego pionka (isValidMoveInternal).";
            }
            logger.warning("Board.makeMove (isValidMoveInternal failed): " + lastMoveValidationError + " dla gracza " + actualCurrentPlayer + " z (" + fromRow + "," + fromCol + ")->(" + toRow + "," + toCol + ")");
            return false;
        }

        logger.info("Board.makeMove: Wykonywanie ruchu (" + fromRow + "," + fromCol + ") -> (" + toRow + "," + toCol + ") przez " + actualCurrentPlayer);
        moveExecutor.executeValidatedMove(fromRow, fromCol, toRow, toCol); // Wykonuje ruch i zmienia turę w TurnManager
        // logger.info("Board.makeMove: Ruch wykonany. Nowa tura (po wykonaniu w MoveExecutor): " + getCurrentPlayer()); // Logika nowej tury jest już w MoveExecutor/TurnManager
        return true;
    }

    /**
     * Zwraca listę wszystkich prawidłowych ruchów (bić lub zwykłych ruchów)
     * możliwych do wykonania przez gracza o podanym kolorze.
     * Uwzględnia zasadę obowiązkowego bicia.
     * @param playerColor Kolor gracza.
     * @return Lista obiektów Move. Jeśli gra się zakończyła, zwraca pustą listę.
     */
    public List<Move> getAllValidMovesForPlayer(PlayerColor playerColor) {
        if (getWinner() != null) return new ArrayList<>(); // Jeśli jest zwycięzca, nie ma już ważnych ruchów
        return moveLogic.getAllValidMovesForPlayer(playerColor);
    }

    /**
     * Zwraca aktualną liczbę białych pionków na planszy.
     * @return Liczba białych pionków.
     */
    public int getWhitePiecesCount() {
        return boardState.getWhitePiecesCount();
    }

    /**
     * Zwraca aktualną liczbę czarnych pionków na planszy.
     * @return Liczba czarnych pionków.
     */
    public int getBlackPiecesCount() {
        return boardState.getBlackPiecesCount();
    }

    /**
     * Sprawdza i zwraca zwycięzcę gry. Najpierw sprawdza, czy zwycięzca nie został
     * wymuszony (np. przez poddanie się gracza), a następnie deleguje sprawdzenie
     * standardowych warunków końca gry (brak pionków, brak ruchów) do komponentu GameStatusChecker.
     * @return Kolor zwycięzcy (WHITE lub BLACK), NONE dla specyficznych przypadków remisu (np. obaj bez pionków),
     *         lub null jeśli gra jest wciąż w toku.
     */
    public PlayerColor getWinner() {
        if (forcedWinner != null) {
            return forcedWinner;
        }
        return gameStatusChecker.getWinner(); // GameStatusChecker używa getCurrentPlayer() do sprawdzenia braku ruchów
    }

    /**
     * Umożliwia aktualnemu graczowi poddanie partii.
     * Przeciwnik tego gracza staje się wymuszonym zwycięzcą.
     * Działa tylko jeśli gra nie została jeszcze w inny sposób zakończona.
     */
    public void surrenderGame() {
        if (getWinner() == null) { // Sprawdź, czy gra już się nie zakończyła
            PlayerColor currentPlayer = turnManager.getCurrentPlayer();
            if (currentPlayer == PlayerColor.WHITE) {
                forcedWinner = PlayerColor.BLACK;
            } else if (currentPlayer == PlayerColor.BLACK) {
                forcedWinner = PlayerColor.WHITE;
            }
            if (forcedWinner != null) {
                logger.info("Gracz " + (currentPlayer == PlayerColor.WHITE ? "BIAŁE" : "CZARNE") + " poddał partię. Wygrywa " + (forcedWinner == PlayerColor.WHITE ? "BIAŁE" : "CZARNE"));
            }
        }
    }
}