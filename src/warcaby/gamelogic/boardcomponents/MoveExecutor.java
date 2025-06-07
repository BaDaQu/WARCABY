package warcaby.gamelogic.boardcomponents;

import warcaby.gamelogic.Piece;
import warcaby.gamelogic.PieceType;
import warcaby.gamelogic.PlayerColor;
import warcaby.utils.Logger;

public class MoveExecutor {
    private BoardState boardState;
    private TurnManager turnManager;
    private MoveLogic moveLogic;
    private static final Logger logger = new Logger(MoveExecutor.class);

    public MoveExecutor(BoardState boardState, TurnManager turnManager, MoveLogic moveLogic) {
        this.boardState = boardState;
        this.turnManager = turnManager;
        this.moveLogic = moveLogic;
    }

    /**
     * Wykonuje ruch, który został już zwalidowany jako poprawny.
     * Przesuwa pionka, obsługuje bicia (w tym długie dla damek), promocję
     * i decyduje, czy gracz kontynuuje turę (wielokrotne bicie) czy następuje zmiana gracza.
     *
     * @param fromRow Wiersz startowy.
     * @param fromCol Kolumna startowa.
     * @param toRow   Wiersz docelowy.
     * @param toCol   Kolumna docelowa.
     * @return true, jeśli gracz wykonujący ruch kontynuuje turę (np. kolejne bicie),
     *         false, jeśli tura przechodzi na drugiego gracza.
     */
    public boolean executeValidatedMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece movingPiece = boardState.getPiece(fromRow, fromCol);

        if (movingPiece == null) {
            // Ten warunek nie powinien być nigdy spełniony, jeśli walidacja w Board.makeMove działa poprawnie.
            logger.error("MoveExecutor: Próba wykonania ruchu nieistniejącym pionkiem z (" + fromRow + "," + fromCol + ")");
            // Zwrócenie false może spowodować przełączenie tury, co może być niepożądane, jeśli to błąd systemowy.
            // Można rozważyć rzucenie wyjątku RuntimeException.
            return false;
        }
        // logger.debug("MoveExecutor: Wykonywanie ruchu dla " + movingPiece + " z ("+fromRow+","+fromCol+") do ("+toRow+","+toCol+")");

        // Krok 1: Zidentyfikuj, czy to jest bicie i gdzie jest bity pionek
        // Metoda getCapturedPieceCoordinatesIfValidJump z MoveLogic powinna poprawnie
        // identyfikować bity pionek dla krótkich i długich bić.
        int[] capturedCoords = moveLogic.getCapturedPieceCoordinatesIfValidJump(movingPiece, fromRow, fromCol, toRow, toCol);
        boolean isThisMoveAJump = (capturedCoords != null);

        // Krok 2: Przesuń pionka wykonującego ruch na planszy
        boardState.setPiece(toRow, toCol, movingPiece); // Umieść pionka na nowym miejscu
        boardState.setPiece(fromRow, fromCol, null);   // Usuń pionka ze starego miejsca

        // Krok 3: Jeśli to było bicie, usuń zbitego pionka
        if (isThisMoveAJump) {
            int jumpedRow = capturedCoords[0]; // Koordynaty faktycznie bitego pionka
            int jumpedCol = capturedCoords[1];

            Piece pieceBeingJumped = boardState.getPiece(jumpedRow, jumpedCol); // Powinien tu być pionek przeciwnika

            if (pieceBeingJumped != null && pieceBeingJumped.getColor() != movingPiece.getColor()) {
                logger.info("MoveExecutor: Bicie pionka " + pieceBeingJumped + " na ("+jumpedRow+","+jumpedCol+")");
                if (pieceBeingJumped.getColor() == PlayerColor.WHITE) {
                    boardState.decrementWhitePieces();
                } else {
                    boardState.decrementBlackPieces();
                }
                boardState.setPiece(jumpedRow, jumpedCol, null); // Usuń zbitego pionka z planszy
            } else {
                // Ten scenariusz wskazuje na poważny błąd w logice walidacji bicia (isJump lub getCapturedPieceCoordinatesIfValidJump)
                // lub na nieoczekiwaną zmianę stanu planszy między walidacją a wykonaniem.
                logger.error("MoveExecutor: KRYTYCZNY BŁĄD! Ruch oznaczony jako bicie, ale na polu ("+jumpedRow+","+jumpedCol+") nie ma pionka przeciwnika do usunięcia. Znaleziono: " + pieceBeingJumped);
                // W tej sytuacji gra może być w niespójnym stanie.
                // Można by rzucić wyjątek, aby zatrzymać dalsze przetwarzanie.
            }
            turnManager.setJumpMadeThisTurn(true);
            turnManager.setLastJumpingPiece(toRow, toCol); // Zapamiętaj pozycję pionka, który właśnie skoczył
        } else {
            turnManager.setJumpMadeThisTurn(false); // To nie był skok
        }

        // Krok 4: Promocja na damkę
        if (movingPiece.getType() == PieceType.MAN) {
            if (movingPiece.getColor() == PlayerColor.WHITE && toRow == 0) {
                movingPiece.promoteToKing();
                logger.debug("MoveExecutor: Biały pionek promowany na damkę na ("+toRow+","+toCol+")");
            } else if (movingPiece.getColor() == PlayerColor.BLACK && toRow == BoardState.SIZE - 1) {
                movingPiece.promoteToKing();
                logger.debug("MoveExecutor: Czarny pionek promowany na damkę na ("+toRow+","+toCol+")");
            }
        }

        // Krok 5: Sprawdzenie możliwości kontynuacji bicia i decyzja o zmianie tury
        if (isThisMoveAJump && moveLogic.canPieceMakeAnyJump(movingPiece, toRow, toCol)) {
            // Pionek właśnie wykonał bicie i MOŻE wykonać kolejne bicie z nowej pozycji
            logger.debug("MoveExecutor: Gracz " + turnManager.getCurrentPlayer() + " kontynuuje bicie pionkiem na ("+toRow+","+toCol+")");
            return true; // Sygnalizuje, że gracz musi kontynuować, tura się NIE zmienia
        } else {
            // Ruch zakończony (był to zwykły ruch lub ostatnie bicie w sekwencji)
            logger.debug("MoveExecutor: Koniec ruchu/sekwencji bić. Zmiana tury z " + turnManager.getCurrentPlayer());
            turnManager.switchPlayer(); // Zmień gracza
            return false; // Sygnalizuje, że tura się zmieniła
        }
    }
}