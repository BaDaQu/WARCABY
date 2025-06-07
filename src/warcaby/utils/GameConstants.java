package warcaby.utils;

// import java.awt.Color; // Importy jeśli będziesz tu definiować obiekty Color
// import java.awt.Font;

public class GameConstants {

    // --- Stałe Sieciowe ---
    public static final int SERVER_PORT = 5000;
    public static final String SERVER_ADDRESS = "localhost";

    public static final int CONNECT_TIMEOUT = 5000;
    public static final int DISCONNECT_WAIT_TIME = 300;

    // --- Stałe Logiki Gry ---
    // public static final int BOARD_SIZE_LOGIC = 8; // Lepiej używać Board.SIZE lub BoardState.SIZE

    // --- Stałe GUI (jeśli chcesz je scentralizować) ---
    // public static final int SQUARE_SIZE_GUI = 70;
    // public static final Color LIGHT_BOARD_COLOR = new Color(230, 200, 160);
    // public static final Color DARK_BOARD_COLOR = new Color(160, 100, 40);


    private GameConstants() {
        // Prywatny konstruktor, aby zapobiec tworzeniu instancji
        throw new IllegalStateException("Klasa ze stałymi nie powinna być instancjonowana");
    }
}