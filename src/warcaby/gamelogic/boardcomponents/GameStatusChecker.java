package warcaby.gamelogic.boardcomponents;

import warcaby.gamelogic.PlayerColor;
import warcaby.utils.Logger;
import java.util.List;

/**
 * Klasa odpowiedzialna za sprawdzanie statusu gry, w tym określanie zwycięzcy.
 */
public class GameStatusChecker {
    private BoardState boardState;
    private TurnManager turnManager;
    private MoveLogic moveLogic;
    private static final Logger logger = new Logger(GameStatusChecker.class);

    public GameStatusChecker(BoardState boardState, TurnManager turnManager, MoveLogic moveLogic) {
        this.boardState = boardState;
        this.turnManager = turnManager;
        this.moveLogic = moveLogic;
    }

    /**
     * Określa, czy gra się zakończyła i kto jest zwycięzcą.
     * Zwycięstwo następuje, gdy przeciwnik straci wszystkie pionki LUB
     * gdy aktualny gracz nie ma żadnych dostępnych ruchów.
     *
     * @return Kolor zwycięzcy (WHITE lub BLACK), NONE dla remisu (obaj bez pionków),
     *         lub null, jeśli gra jest wciąż w toku.
     */
    public PlayerColor getWinner() {
        boolean whiteHasPieces = boardState.getWhitePiecesCount() > 0;
        boolean blackHasPieces = boardState.getBlackPiecesCount() > 0;

        // Warunek 1: Zwycięstwo przez zbicie wszystkich pionków przeciwnika.
        if (!whiteHasPieces && blackHasPieces) {
            logger.info("GameStatusChecker: Białe nie mają pionków. Czarne wygrywają.");
            return PlayerColor.BLACK;
        }
        if (!blackHasPieces && whiteHasPieces) {
            logger.info("GameStatusChecker: Czarne nie mają pionków. Białe wygrywają.");
            return PlayerColor.WHITE;
        }
        // Teoretyczny warunek remisu, jeśli obaj gracze stracą wszystkie pionki.
        if (!whiteHasPieces && !blackHasPieces) {
            logger.info("GameStatusChecker: Żaden gracz nie ma pionków. Remis.");
            return PlayerColor.NONE;
        }

        // Warunek 2: Zwycięstwo przez zablokowanie przeciwnika (brak możliwych ruchów).
        PlayerColor currentPlayer = turnManager.getCurrentPlayer();
        if (currentPlayer == null) {
            logger.error("GameStatusChecker: Nie można określić aktualnego gracza.");
            return null; // Nie można podjąć decyzji
        }

        List<Move> validMoves = moveLogic.getAllValidMovesForPlayer(currentPlayer);
        if (validMoves.isEmpty()) { // Sprawdzenie, czy lista jest pusta (getAllValidMovesForPlayer nie powinno zwracać null)
            PlayerColor winner = (currentPlayer == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
            logger.info("GameStatusChecker: Gracz " + currentPlayer + " nie ma dostępnych ruchów. Przegrywa. Wygrywa " + winner + ".");
            return winner;
        }

        return null; // Gra w toku
    }
}