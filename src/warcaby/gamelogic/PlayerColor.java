package warcaby.gamelogic;

/**
 * Typ wyliczeniowy (enum) reprezentujący możliwe kolory graczy lub stan pola na planszy.
 * W grze w warcaby mamy dwóch graczy: białego i czarnego.
 * Dodatkowo, wartość NONE może być używana do oznaczenia pustego pola
 * lub sytuacji, gdy nie ma jednoznacznego zwycięzcy (np. remis).
 */
public enum PlayerColor {
    /**
     * Reprezentuje gracza grającego białymi pionkami.
     * Zazwyczaj rozpoczyna grę.
     */
    WHITE,

    /**
     * Reprezentuje gracza grającego czarnymi pionkami.
     */
    BLACK,

    /**
     * Wartość specjalna używana do różnych celów:
     * - Może reprezentować puste pole na planszy.
     * - Może oznaczać brak zwycięzcy w grze (np. w przypadku remisu,
     *   gdy obaj gracze stracą wszystkie pionki, lub w innych specyficznych
     *   sytuacjach końca gry bez wyłonienia jednego zwycięzcy).
     */
    NONE
}