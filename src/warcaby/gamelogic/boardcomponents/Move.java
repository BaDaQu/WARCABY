package warcaby.gamelogic.boardcomponents;

/**
 * Reprezentuje pojedynczy ruch na planszy.
 */
public class Move {
    public int fromRow, fromCol, toRow, toCol;
    public boolean isJump; // Określa, czy ruch jest biciem

    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean isJump) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.isJump = isJump;
    }
}