package warcaby.gamelogic;

/**
 * Typ wyliczeniowy (enum) reprezentujący możliwe typy pionków w grze w warcaby.
 * Pionek może być zwykłym pionkiem (MAN) lub damką (KING).
 */
public enum PieceType {
    /**
     * Reprezentuje zwykłego pionka, który porusza się tylko do przodu (z wyjątkiem bicia)
     * i może być promowany na damkę po osiągnięciu ostatniego rzędu planszy przeciwnika.
     */
    MAN,

    /**
     * Reprezentuje damkę (królową), która ma specjalne możliwości ruchu i bicia,
     * zazwyczaj poruszając się o dowolną liczbę pól po przekątnej.
     */
    KING
}