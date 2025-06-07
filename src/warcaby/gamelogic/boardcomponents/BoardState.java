package warcaby.gamelogic.boardcomponents;

import warcaby.gamelogic.Piece;
import warcaby.gamelogic.PlayerColor;

/**
 * Przechowuje stan planszy: pozycje pionków oraz ich liczbę.
 */
public class BoardState {
    public static final int SIZE = 8;
    private Piece[][] boardGrid;
    private int whitePiecesCount;
    private int blackPiecesCount;

    public BoardState() {
        this.boardGrid = new Piece[SIZE][SIZE];
    }

    public void initializeState() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                boardGrid[row][col] = null;
                if ((row + col) % 2 != 0) { // Tylko ciemne pola
                    if (row < 3) {
                        boardGrid[row][col] = new Piece(PlayerColor.BLACK);
                    } else if (row > 4) {
                        boardGrid[row][col] = new Piece(PlayerColor.WHITE);
                    }
                }
            }
        }
        whitePiecesCount = 12;
        blackPiecesCount = 12;
    }

    public Piece getPiece(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return boardGrid[row][col];
        }
        return null;
    }

    public void setPiece(int row, int col, Piece piece) {
        if (isValidCoordinate(row, col)) {
            boardGrid[row][col] = piece;
        }
    }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public int getWhitePiecesCount() { return whitePiecesCount; }
    public int getBlackPiecesCount() { return blackPiecesCount; }
    public void decrementWhitePieces() { whitePiecesCount--; }
    public void decrementBlackPieces() { blackPiecesCount--; }
}