package warcaby.gamelogic;

/**
 * Reprezentuje pojedynczy pionek na planszy do gry w warcaby.
 * Przechowuje informacje o kolorze pionka (biały lub czarny)
 * oraz jego typie (zwykły pionek - MAN, lub damka - KING).
 */
public class Piece {
    private PlayerColor color; // Kolor pionka (WHITE lub BLACK)
    private PieceType type;    // Typ pionka (MAN lub KING)

    /**
     * Konstruktor tworzący nowy pionek o zadanym kolorze.
     * Domyślnie nowo utworzony pionek jest zwykłym pionkiem (MAN).
     * @param color Kolor pionka (PlayerColor.WHITE lub PlayerColor.BLACK).
     */
    public Piece(PlayerColor color) {
        this.color = color;
        this.type = PieceType.MAN; // Domyślny typ to zwykły pionek
    }

    /**
     * Konstruktor tworzący nowy pionek o zadanym kolorze i typie.
     * Może być używany do tworzenia damek bezpośrednio lub do celów testowych.
     * @param color Kolor pionka.
     * @param type Typ pionka (PieceType.MAN lub PieceType.KING).
     */
    public Piece(PlayerColor color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    /**
     * Zwraca kolor pionka.
     * @return PlayerColor reprezentujący kolor pionka.
     */
    public PlayerColor getColor() {
        return color;
    }

    /**
     * Zwraca typ pionka (czy jest to zwykły pionek, czy damka).
     * @return PieceType reprezentujący typ pionka.
     */
    public PieceType getType() {
        return type;
    }

    /**
     * Promuje zwykłego pionka (MAN) na damkę (KING).
     * Ta metoda zmienia typ pionka.
     */
    public void promoteToKing() {
        this.type = PieceType.KING;
    }

    /**
     * Zwraca tekstową reprezentację pionka.
     * Użyteczne do debugowania lub prostego wyświetlania stanu planszy w konsoli.
     * Przykład: "WM" dla białego pionka, "BK" dla czarnej damki.
     * @return String opisujący pionka.
     */
    @Override
    public String toString() {
        // Pierwsza litera koloru (W lub B)
        char colorChar = color.name().charAt(0);
        // Litera typu (M dla MAN, K dla KING)
        char typeChar = (type == PieceType.KING ? 'K' : 'M');
        return "" + colorChar + typeChar;
    }
}