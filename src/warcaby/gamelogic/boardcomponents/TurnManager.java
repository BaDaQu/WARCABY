package warcaby.gamelogic.boardcomponents;

import warcaby.gamelogic.PlayerColor;
import warcaby.utils.Logger; // Zakładając, że Logger jest w utils

/**
 * Klasa TurnManager zarządza informacjami o aktualnej turze w grze,
 * w tym który gracz wykonuje ruch oraz czy ostatni ruch był biciem,
 * co jest istotne dla logiki wielokrotnych bić.
 */
public class TurnManager {
    private PlayerColor currentPlayer; // Kolor gracza, którego jest aktualnie tura
    private boolean jumpMadeThisTurn;  // Flaga wskazująca, czy w tej turze wykonano już bicie (dla wielokrotnych bić)
    private int lastJumpingPieceRow;   // Wiersz pionka, który ostatnio wykonał bicie
    private int lastJumpingPieceCol;   // Kolumna pionka, który ostatnio wykonał bicie
    private static final Logger logger = new Logger(TurnManager.class);

    public TurnManager() {
        reset(); // Inicjalizacja stanu na początku gry
    }

    /**
     * Resetuje stan managera tury do wartości początkowych (start gry, białe zaczynają).
     */
    public void reset() {
        this.currentPlayer = PlayerColor.WHITE;
        this.jumpMadeThisTurn = false;
        this.lastJumpingPieceRow = -1;
        this.lastJumpingPieceCol = -1;
    }

    public PlayerColor getCurrentPlayer() { return currentPlayer; }

    /**
     * Przełącza turę na drugiego gracza.
     * Resetuje również flagi związane z kontynuacją bicia, ponieważ nowa tura
     * (innego gracza) oznacza nowy kontekst.
     */
    public void switchPlayer() {
        currentPlayer = (currentPlayer == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
        this.jumpMadeThisTurn = false;
        this.lastJumpingPieceRow = -1;
        this.lastJumpingPieceCol = -1;
        logger.info("Tura przełączona (switchPlayer) na: " + currentPlayer);
    }

    /**
     * Bezwarunkowo ustawia aktualnego gracza na podanego.
     * Używane głównie do synchronizacji stanu tury z informacją od serwera w grze online.
     * Jeśli tura faktycznie się zmienia, resetuje stan kontynuacji bicia.
     * @param player Gracz, który ma teraz turę.
     */
    public void forceSetCurrentPlayer(PlayerColor player) {
        if (this.currentPlayer != player) {
            logger.info("Wymuszanie ustawienia tury z " + this.currentPlayer + " na " + player + " (np. przez serwer).");
            this.currentPlayer = player;
            // Jeśli tura jest wymuszana na innego gracza, a poprzedni był w trakcie bicia,
            // to ta sekwencja bicia jest przerywana.
            if (this.isJumpMadeThisTurn()) {
                logger.info("Resetowanie isJumpMadeThisTurn z powodu wymuszonej zmiany tury na " + player);
            }
            setJumpMadeThisTurn(false); // Resetuje też lastJumpingPiece
        } else {
            // Jeśli serwer potwierdza turę tego samego gracza, nie resetujemy isJumpMadeThisTurn,
            // aby umożliwić kontynuację bicia.
            logger.debug("Serwer potwierdził turę gracza: " + player + ", isJumpMadeThisTurn: " + this.isJumpMadeThisTurn());
        }
    }

    public boolean isJumpMadeThisTurn() { return this.jumpMadeThisTurn; }

    /**
     * Ustawia flagę informującą, czy w tej turze wykonano bicie.
     * Jeśli ustawiono na false, resetuje również informacje o ostatnim skaczącym pionku.
     * @param jumpMade true, jeśli wykonano bicie; false w przeciwnym razie.
     */
    public void setJumpMadeThisTurn(boolean jumpMade) {
        this.jumpMadeThisTurn = jumpMade;
        if (!jumpMade) {
            this.lastJumpingPieceRow = -1;
            this.lastJumpingPieceCol = -1;
        }
    }

    public int getLastJumpingPieceRow() { return lastJumpingPieceRow; }
    public int getLastJumpingPieceCol() { return lastJumpingPieceCol; }

    /**
     * Zapamiętuje pozycję pionka, który właśnie wykonał bicie.
     * Używane do wymuszenia kontynuacji bicia tym samym pionkiem.
     * @param row Wiersz pionka.
     * @param col Kolumna pionka.
     */
    public void setLastJumpingPiece(int row, int col) {
        this.lastJumpingPieceRow = row;
        this.lastJumpingPieceCol = col;
    }
}