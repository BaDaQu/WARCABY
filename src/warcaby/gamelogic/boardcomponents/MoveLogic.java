package warcaby.gamelogic.boardcomponents;

import warcaby.gamelogic.Piece;
import warcaby.gamelogic.PieceType;
import warcaby.gamelogic.PlayerColor;
import warcaby.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Klasa MoveLogic zawiera główną logikę dotyczącą ruchów i bić w grze w warcaby.
 * Odpowiada za walidację ruchów, generowanie możliwych ruchów (w tym bić)
 * dla pionków i damek, uwzględniając zasady takie jak obowiązkowe bicie
 * oraz specyfikę długich bić damek.
 */
public class MoveLogic {
    private BoardState boardState; // Referencja do aktualnego stanu planszy
    private TurnManager turnManager; // Referencja do managera tury (kto gra, czy jest kontynuacja bicia)
    private MoveValidator moveValidator; // Pomocniczy walidator dla prostszych sprawdzeń
    private static final Logger logger = new Logger(MoveLogic.class); // Logger dla tej klasy

    public MoveLogic(BoardState boardState, TurnManager turnManager) {
        this.boardState = boardState;
        this.turnManager = turnManager;
        this.moveValidator = new MoveValidator(boardState);
    }

    /**
     * Identyfikuje koordynaty pionka, który zostałby zbity podczas ruchu
     * z (fromR, fromC) do (toR, toC).
     * Ta metoda jest kluczowa dla poprawnego wykonania bicia, zarówno krótkiego przez pionka,
     * jak i długiego przez damkę. Sprawdza, czy na ścieżce diagonalnej między polem
     * startowym a docelowym (nie wliczając tych pól) znajduje się dokładnie jeden pionek
     * przeciwnika, a wszystkie pozostałe pola na tej ścieżce są puste.
     * Dla pionka (MAN) dodatkowo weryfikuje, czy ląduje on bezpośrednio za bitym pionkiem.
     *
     * @param movingPiece Pionek wykonujący potencjalne bicie.
     * @param fromR Wiersz startowy ruchu.
     * @param fromC Kolumna startowa ruchu.
     * @param toR Wiersz docelowy ruchu (lądowania).
     * @param toC Kolumna docelowa ruchu (lądowania).
     * @return Tablica int[2] z koordynatami {wiersz, kolumna} zbitego pionka,
     *         lub null jeśli ruch nie jest prawidłowym biciem.
     */
    public int[] getCapturedPieceCoordinatesIfValidJump(Piece movingPiece, int fromR, int fromC, int toR, int toC) {
        if (movingPiece == null || !boardState.isValidCoordinate(toR, toC) || boardState.getPiece(toR, toC) != null) {
            return null; // Warunki początkowe: pionek istnieje, pole docelowe jest na planszy i puste
        }

        int rowDiff = toR - fromR;
        int colDiff = toC - fromC;

        // Ruch musi być diagonalny i mieć długość co najmniej 2 (aby przeskoczyć przynajmniej jedno pole)
        if (Math.abs(rowDiff) != Math.abs(colDiff) || Math.abs(rowDiff) < 2) {
            return null;
        }

        int stepR = Integer.signum(rowDiff); // Kierunek ruchu w wierszach (-1, 0, lub 1)
        int stepC = Integer.signum(colDiff); // Kierunek ruchu w kolumnach (-1, 0, lub 1)
        int distance = Math.abs(rowDiff);    // Całkowita "długość" ruchu w liczbie kroków po diagonali

        int opponentPiecesOnPath = 0;    // Licznik pionków przeciwnika na ścieżce
        int[] capturedPieceCoords = null; // Koordynaty znalezionego pionka przeciwnika

        // Iteruj po wszystkich polach znajdujących się MIĘDZY polem startowym a docelowym
        for (int i = 1; i < distance; i++) {
            int currentR = fromR + i * stepR;
            int currentC = fromC + i * stepC;

            Piece pieceOnPath = boardState.getPiece(currentR, currentC);
            if (pieceOnPath != null) { // Jeśli na sprawdzanym polu jest jakiś pionek
                if (pieceOnPath.getColor() != movingPiece.getColor()) { // Jeśli to pionek przeciwnika
                    opponentPiecesOnPath++;
                    if (opponentPiecesOnPath > 1) return null; // Nie można przeskoczyć więcej niż jednego pionka
                    capturedPieceCoords = new int[]{currentR, currentC}; // Zapamiętaj koordynaty tego pionka
                } else { // Jeśli to własny pionek na drodze
                    return null; // Bicie jest niemożliwe
                }
            }
            // Jeśli pole jest puste:
            // - Dla damki: jeśli już minęła pionka przeciwnika (opponentPiecesOnPath == 1),
            //   puste pole jest częścią drogi do lądowania - kontynuuj sprawdzanie.
            // - Dla pionka MAN: jeśli minął pionka przeciwnika i napotkał puste pole,
            //   to oznacza, że nie ląduje bezpośrednio za nim (chyba że toR, toC jest tym polem).
            //   Warunek dla MAN (distance == 2) jest sprawdzany później.
        }

        // Aby ruch był prawidłowym biciem, na ścieżce musi być dokładnie jeden pionek przeciwnika.
        if (opponentPiecesOnPath == 1) {
            // Dla pionka (MAN) istnieje dodatkowe ograniczenie: musi wylądować dokładnie jedno pole
            // za bitym pionkiem, co oznacza, że całkowita długość ruchu musi wynosić 2.
            if (movingPiece.getType() == PieceType.MAN) {
                if (distance == 2) { // Pionek skacze o 2 pola
                    // Upewnij się, że capturedPieceCoords nie jest null (powinno być, jeśli opponentPiecesOnPath==1)
                    // i że jest to rzeczywiście pole między startem a końcem.
                    if (capturedPieceCoords != null && capturedPieceCoords[0] == fromR + stepR && capturedPieceCoords[1] == fromC + stepC) {
                        return capturedPieceCoords;
                    } else {
                        //logger.error("Błąd logiki w getCapturedPieceCoordinatesIfValidJump dla MANA");
                        return null; // Coś poszło nie tak z identyfikacją bitego pionka
                    }
                } else {
                    return null; // Pionek nie może wykonać "długiego" bicia
                }
            }
            return capturedPieceCoords; // Dla damki, jeśli warunki są spełnione, zwraca koordynaty
        }
        return null; // Nie znaleziono dokładnie jednego pionka przeciwnika do zbicia lub inne warunki nie spełnione
    }

    /**
     * Sprawdza, czy dany ruch z (fromRow, fromCol) do (toRow, toCol) jest prawidłowym biciem.
     * Wykorzystuje metodę getCapturedPieceCoordinatesIfValidJump do weryfikacji.
     * @return true, jeśli ruch jest prawidłowym biciem.
     */
    public boolean isJump(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        return getCapturedPieceCoordinatesIfValidJump(piece, fromRow, fromCol, toRow, toCol) != null;
    }

    /**
     * Sprawdza, czy dany pionek może wykonać jakiekolwiek bicie ze swojej aktualnej pozycji.
     * @param piece Pionek do sprawdzenia.
     * @param r Wiersz pozycji pionka.
     * @param c Kolumna pozycji pionka.
     * @return true, jeśli pionek może wykonać przynajmniej jedno bicie.
     */
    public boolean canPieceMakeAnyJump(Piece piece, int r, int c) {
        if (piece == null) return false;
        // Generuje wszystkie możliwe bicia dla tego pionka i sprawdza, czy lista nie jest pusta.
        // To jest bardziej kosztowne, ale pewniejsze niż iterowanie po wszystkich polach planszy.
        List<Move> potentialJumps = new ArrayList<>();
        if (piece.getType() == PieceType.MAN) {
            int[] dR_offsets = {-2, -2, 2, 2};
            int[] dC_offsets = {-2, 2, -2, 2};
            for (int i = 0; i < 4; i++) {
                if (isJump(piece, r, c, r + dR_offsets[i], c + dC_offsets[i])) return true;
            }
        } else if (piece.getType() == PieceType.KING) {
            checkAndAddKingJumpsInDirection(piece, r, c, -1, -1, potentialJumps);
            if (!potentialJumps.isEmpty()) return true;
            checkAndAddKingJumpsInDirection(piece, r, c, -1, 1, potentialJumps);
            if (!potentialJumps.isEmpty()) return true;
            checkAndAddKingJumpsInDirection(piece, r, c, 1, -1, potentialJumps);
            if (!potentialJumps.isEmpty()) return true;
            checkAndAddKingJumpsInDirection(piece, r, c, 1, 1, potentialJumps);
            if (!potentialJumps.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Pomocnicza metoda do znajdowania i dodawania do listy `jumpsList` wszystkich możliwych
     * długich bić dla damki (`king`) z pozycji (`startRow`, `startCol`) w określonym
     * kierunku diagonalnym (`rowDir`, `colDir`).
     */
    private void checkAndAddKingJumpsInDirection(Piece king, int startRow, int startCol, int rowDir, int colDir, List<Move> jumpsList) {
        int opponentPieceR = -1, opponentPieceC = -1;
        boolean opponentFoundOnPath = false;

        // Krok 1: Znajdź pierwszego pionka na diagonali (pionka do zbicia)
        for (int k = 1; k < BoardState.SIZE; k++) {
            int currentR = startRow + k * rowDir;
            int currentC = startCol + k * colDir;

            if (!boardState.isValidCoordinate(currentR, currentC)) break; // Poza planszą

            Piece pieceOnPath = boardState.getPiece(currentR, currentC);
            if (pieceOnPath != null) { // Znaleziono jakiś pionek
                if (pieceOnPath.getColor() != king.getColor()) { // Jeśli to pionek przeciwnika
                    opponentPieceR = currentR;
                    opponentPieceC = currentC;
                    opponentFoundOnPath = true;
                }
                // Niezależnie czy swój, czy przeciwnika, to jest pierwszy napotkany,
                // więc przerywamy szukanie *pionka do zbicia* dalej w tym kierunku.
                break;
            }
            // Jeśli pole jest puste, kontynuuj szukanie pionka do zbicia
        }

        // Krok 2: Jeśli znaleziono pionka przeciwnika do zbicia
        if (opponentFoundOnPath) {
            // Teraz szukaj wszystkich pustych pól ZA tym pionkiem (na tej samej diagonali) do lądowania
            for (int m = 1; m < BoardState.SIZE; m++) {
                int landR = opponentPieceR + m * rowDir; // Ląduj za zbitym pionkiem
                int landC = opponentPieceC + m * colDir;

                if (!boardState.isValidCoordinate(landR, landC)) break; // Poza planszą

                if (boardState.getPiece(landR, landC) == null) { // Puste pole do lądowania
                    // isJump zweryfikuje, czy ruch od (startRow,startCol) do (landR,landC)
                    // jest prawidłowym biciem (przeskakuje tylko TEGO JEDNEGO zidentyfikowanego
                    // pionka przeciwnika na (opponentPieceR, opponentPieceC), a droga
                    // między startem a tym bitym pionkiem była pusta).
                    if (isJump(king, startRow, startCol, landR, landC)) {
                        jumpsList.add(new Move(startRow, startCol, landR, landC, true));
                    }
                } else {
                    // Napotkano inny pionek za pionkiem do bicia - dalej w tym kierunku lądować nie można.
                    break;
                }
            }
        }
    }

    /**
     * Generuje listę wszystkich możliwych (prawidłowych) bić dla gracza o danym kolorze.
     * Uwzględnia krótkie bicia dla pionków (MAN) i długie bicia dla damek (KING).
     * @param playerColor Kolor gracza, dla którego generowane są bicia.
     * @return Lista obiektów Move reprezentujących wszystkie możliwe bicia.
     */
    public List<Move> getAllPossibleJumpsForPlayer(PlayerColor playerColor) {
        List<Move> jumps = new ArrayList<>();
        for (int r = 0; r < BoardState.SIZE; r++) {
            for (int c = 0; c < BoardState.SIZE; c++) {
                Piece piece = boardState.getPiece(r, c);
                if (piece != null && piece.getColor() == playerColor) {
                    if (piece.getType() == PieceType.MAN) {
                        // Dla pionka, sprawdzamy tylko 4 kierunki skoku o 2 pola
                        int[] dR_offsets = {-2, -2, 2, 2};
                        int[] dC_offsets = {-2, 2, -2, 2};
                        for (int i = 0; i < 4; i++) {
                            int toR = r + dR_offsets[i];
                            int toC = c + dC_offsets[i];
                            if (isJump(piece, r, c, toR, toC)) {
                                jumps.add(new Move(r, c, toR, toC, true));
                            }
                        }
                    } else if (piece.getType() == PieceType.KING) {
                        // Dla damki, używamy dedykowanej logiki do znajdowania długich bić
                        checkAndAddKingJumpsInDirection(piece, r, c, -1, -1, jumps); // G-L
                        checkAndAddKingJumpsInDirection(piece, r, c, -1, 1, jumps);  // G-P
                        checkAndAddKingJumpsInDirection(piece, r, c, 1, -1, jumps);  // D-L
                        checkAndAddKingJumpsInDirection(piece, r, c, 1, 1, jumps);   // D-P
                    }
                }
            }
        }
        // Teoretycznie, jeśli logika jest poprawna, duplikaty nie powinny powstawać.
        // Jeśli jednak by powstały (np. przez różne ścieżki prowadzące do tego samego bicia),
        // można by je tu odfiltrować, ale wymagałoby to implementacji equals/hashCode w Move.
        return jumps;
    }

    /**
     * Wewnętrzna metoda walidująca, czy dany ruch (zwykły lub bicie) jest poprawny
     * zgodnie z zasadami gry dla aktualnego gracza i stanu planszy.
     * Uwzględnia zasady obowiązkowego bicia i kontynuacji bicia, jeśli
     * parametr `checkingSpecificJump` jest ustawiony na `false`.
     *
     * @param fromRow Wiersz startowy.
     * @param fromCol Kolumna startowa.
     * @param toRow Wiersz docelowy.
     * @param toCol Kolumna docelowa.
     * @param checkingSpecificJump Jeśli `true`, metoda pomija globalne reguły obowiązkowego bicia
     *                             (np. gdy walidujemy pojedynczy skok z listy możliwych skoków).
     *                             Jeśli `false` (domyślnie), pełne reguły są stosowane.
     * @return `true`, jeśli ruch jest poprawny, `false` w przeciwnym razie.
     */
    public boolean isValidMoveInternal(int fromRow, int fromCol, int toRow, int toCol, boolean checkingSpecificJump) {
        if (!boardState.isValidCoordinate(fromRow, fromCol) || !boardState.isValidCoordinate(toRow, toCol)) return false;
        if (boardState.getPiece(toRow, toCol) != null) return false; // Pole docelowe musi być puste

        Piece piece = boardState.getPiece(fromRow, fromCol);
        // Pionek musi istnieć i należeć do aktualnego gracza
        if (piece == null || piece.getColor() != turnManager.getCurrentPlayer()) return false;

        boolean isAttemptedMoveAJump = isJump(piece, fromRow, fromCol, toRow, toCol);

        // Sprawdzanie reguł obowiązkowego bicia i kontynuacji,
        // tylko jeśli nie sprawdzamy poprawności pojedynczego, konkretnego skoku z listy.
        if (!checkingSpecificJump) {
            List<Move> allPossibleJumpsForCurrentPlayer = getAllPossibleJumpsForPlayer(turnManager.getCurrentPlayer());
            boolean mandatoryJumpExistsForPlayer = !allPossibleJumpsForCurrentPlayer.isEmpty();

            if (turnManager.isJumpMadeThisTurn()) { // Jeśli gracz jest w trakcie wielokrotnego bicia
                if (fromRow != turnManager.getLastJumpingPieceRow() || fromCol != turnManager.getLastJumpingPieceCol()) return false; // Musi użyć tego samego pionka
                if (!isAttemptedMoveAJump) return false; // Następny ruch musi być biciem
            } else if (mandatoryJumpExistsForPlayer) { // Jeśli jest obowiązkowe bicie na planszy
                if (!isAttemptedMoveAJump) return false; // Próbowany ruch musi być biciem
                // Dodatkowo, sprawdź, czy to próbowane bicie jest jednym z dostępnych obowiązkowych bić
                boolean isThisSpecificJumpValid = allPossibleJumpsForCurrentPlayer.stream()
                        .anyMatch(jump -> jump.fromRow == fromRow && jump.fromCol == fromCol && jump.toRow == toRow && jump.toCol == toCol);
                if (!isThisSpecificJumpValid) return false; // To nie jest jedno z obowiązkowych bić
            }
            // Jeśli nie ma `jumpMadeThisTurn` i nie ma `mandatoryJumpExistsForPlayer`,
            // to próbowany ruch może być zwykłym ruchem (jeśli `isAttemptedMoveAJump` jest false).
        }

        // Logika specyficzna dla typu pionka
        if (piece.getType() == PieceType.MAN) {
            int rowDiff = toRow - fromRow;
            int colDiffAbs = Math.abs(toCol - fromCol);
            if (isAttemptedMoveAJump) {
                // isJump już zweryfikowało poprawność bicia (w tym odległość 2 dla MANa)
                return Math.abs(rowDiff) == 2 && colDiffAbs == 2;
            } else { // Zwykły ruch pionka
                int expectedForwardStep = (piece.getColor() == PlayerColor.WHITE) ? -1 : 1;
                return rowDiff == expectedForwardStep && colDiffAbs == 1;
            }
        } else if (piece.getType() == PieceType.KING) {
            int rowDiff = toRow - fromRow;
            int colDiff = toCol - fromCol;
            if (Math.abs(rowDiff) != Math.abs(colDiff)) return false; // Ruch damki musi być diagonalny

            if (isAttemptedMoveAJump) {
                return true; // isJump już zweryfikowało poprawność długiego bicia dla damki
            } else { // Zwykły ruch damki (nie bicie)
                int stepRow = Integer.signum(rowDiff);
                int stepCol = Integer.signum(colDiff);
                // Sprawdź, czy wszystkie pola na ścieżce są puste
                for (int i = 1; i < Math.abs(rowDiff); i++) {
                    if (boardState.getPiece(fromRow + i * stepRow, fromCol + i * stepCol) != null) {
                        return false; // Ścieżka jest zablokowana
                    }
                }
                return true; // Ścieżka czysta
            }
        }
        return false; // Domyślnie ruch jest nieprawidłowy
    }


    /**
     * Generuje listę wszystkich prawidłowych ruchów (bić lub zwykłych ruchów)
     * dla gracza o określonym kolorze, uwzględniając zasadę obowiązkowego bicia.
     * Jeśli dostępne są jakiekolwiek bicia, zwraca tylko listę tych bić.
     * Jeśli gracz jest w trakcie kontynuacji wielokrotnego bicia, zwraca tylko
     * bicia możliwe do wykonania przez pionka, który ostatnio bił.
     * @param playerColor Kolor gracza.
     * @return Lista obiektów Move reprezentujących wszystkie prawidłowe ruchy.
     */
    public List<Move> getAllValidMovesForPlayer(PlayerColor playerColor) {
        List<Move> allPossibleJumps = getAllPossibleJumpsForPlayer(playerColor);

        if (!allPossibleJumps.isEmpty()) { // Jeśli są dostępne jakiekolwiek bicia
            // Jeśli gracz kontynuuje wielokrotne bicie (i to jego tura)
            if (turnManager.isJumpMadeThisTurn() && playerColor == turnManager.getCurrentPlayer()) {
                // Zwróć tylko te bicia, które mogą być wykonane przez pionka, który ostatnio skakał
                return allPossibleJumps.stream()
                        .filter(jump -> jump.fromRow == turnManager.getLastJumpingPieceRow() &&
                                jump.fromCol == turnManager.getLastJumpingPieceCol())
                        .collect(Collectors.toList());
            }
            // W przeciwnym razie (początek tury lub nie ma kontynuacji), zwróć wszystkie dostępne bicia
            return allPossibleJumps;
        }

        // Jeśli nie ma dostępnych bić, zbierz wszystkie zwykłe (niebędące biciami) ruchy
        List<Move> simpleMoves = new ArrayList<>();
        for (int r = 0; r < BoardState.SIZE; r++) {
            for (int c = 0; c < BoardState.SIZE; c++) {
                Piece piece = boardState.getPiece(r, c);
                if (piece != null && piece.getColor() == playerColor) {
                    // Generowanie zwykłych ruchów dla pionka
                    if (piece.getType() == PieceType.MAN) {
                        int forwardStep = (playerColor == PlayerColor.WHITE) ? -1 : 1;
                        int[] dc = {-1, 1}; // Możliwe zmiany w kolumnie
                        for (int colOffset : dc) {
                            int toR = r + forwardStep;
                            int toC = c + colOffset;
                            // isValidMoveInternal z false sprawdzi, czy to poprawny zwykły ruch
                            // (zakładając, że nie ma obowiązkowych bić, co już sprawdziliśmy)
                            if (isValidMoveInternal(r, c, toR, toC, false)) {
                                simpleMoves.add(new Move(r, c, toR, toC, false));
                            }
                        }
                    } else if (piece.getType() == PieceType.KING) { // Generowanie zwykłych ruchów dla damki
                        int[] drKing = {-1, -1, 1, 1}; // Kierunki delta dla wierszy
                        int[] dcKing = {-1, 1, -1, 1}; // Kierunki delta dla kolumn
                        for (int i = 0; i < 4; i++) { // Dla każdego z 4 kierunków diagonalnych
                            int currentStepRow = drKing[i];
                            int currentStepCol = dcKing[i];
                            for (int step = 1; step < BoardState.SIZE; step++) { // Iteruj po odległości
                                int toR = r + step * currentStepRow;
                                int toC = c + step * currentStepCol;
                                if (!boardState.isValidCoordinate(toR, toC)) break; // Poza planszą

                                if (boardState.getPiece(toR, toC) == null) { // Jeśli pole jest puste
                                    if (isValidMoveInternal(r, c, toR, toC, false)) {
                                        simpleMoves.add(new Move(r, c, toR, toC, false));
                                    }
                                } else {
                                    break; // Napotkano przeszkodę, dalej w tym kierunku nie można iść
                                }
                            }
                        }
                    }
                }
            }
        }
        return simpleMoves;
    }
}