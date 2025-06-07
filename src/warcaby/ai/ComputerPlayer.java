package warcaby.ai;

import warcaby.gamelogic.Board;
import warcaby.gamelogic.PlayerColor;
import warcaby.gamelogic.boardcomponents.Move;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

/**
 * Prosta implementacja "sztucznej inteligencji" komputera.
 * Wykonuje losowy ruch, ale z priorytetem dla bić, jeśli są dostępne.
 */
public class ComputerPlayer {
    private PlayerColor aiColor;
    private Random random;

    public ComputerPlayer(PlayerColor aiColor) {
        this.aiColor = aiColor;
        this.random = new Random();
    }

    /**
     * Wybiera ruch dla komputera.
     * Najpierw szuka wszystkich dostępnych bić. Jeśli istnieją, losuje jedno z nich.
     * Jeśli nie ma bić, losuje jeden ze wszystkich dostępnych zwykłych ruchów.
     * @param board aktualny stan planszy.
     * @return wybrany ruch lub null, jeśli brak ruchów.
     */
    public Move getComputerMove(Board board) {
        if (board.getCurrentPlayer() != aiColor) {
            System.err.println("ComputerPlayer: Próba wykonania ruchu, gdy nie jest tura AI!");
            return null;
        }

        List<Move> allValidMoves = board.getAllValidMovesForPlayer(aiColor);

        if (allValidMoves == null || allValidMoves.isEmpty()) {
            return null;
        }

        // Zbierz wszystkie ruchy, które są biciami.
        List<Move> jumps = new ArrayList<>();
        for (Move move : allValidMoves) {
            if (move.isJump) {
                jumps.add(move);
            }
        }

        // Bicia mają priorytet.
        if (!jumps.isEmpty()) {
            return jumps.get(random.nextInt(jumps.size()));
        } else {
            return allValidMoves.get(random.nextInt(allValidMoves.size()));
        }
    }
}