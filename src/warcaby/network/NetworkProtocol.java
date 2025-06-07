package warcaby.network;

// import warcaby.utils.Logger; // Opcjonalnie, jeśli potrzebny jest tu logger

public class NetworkProtocol {
    // private static final Logger logger = new Logger(NetworkProtocol.class);

    // Komendy od klienta do serwera
    public static final String CMD_FIND_GAME = "FIND_GAME";
    public static final String CMD_MOVE = "MOVE";
    public static final String CMD_CAPTURE_CONTINUED = "CAPTURE_CONTINUED";
    public static final String CMD_CANCEL_SEARCH = "CANCEL_SEARCH";
    public static final String CMD_QUIT = "QUIT";
    public static final String CMD_END_SESSION = "END_SESSION";

    // Odpowiedzi serwera do klienta
    public static final String RSP_WAITING = "WAITING";
    public static final String RSP_GAME_FOUND = "GAME_FOUND";
    public static final String RSP_GAME_STARTING = "GAME_STARTING";
    public static final String RSP_GAME_STARTED = "GAME_STARTED";
    public static final String RSP_OPPONENT_MOVE = "OPPONENT_MOVE";
    public static final String RSP_OPPONENT_CAPTURE_CONTINUED = "OPPONENT_CAPTURE_CONTINUED";
    public static final String RSP_TIME_UPDATE = "TIME_UPDATE";
    public static final String RSP_SEARCH_CANCELLED = "SEARCH_CANCELLED";
    public static final String RSP_OPPONENT_QUIT = "OPPONENT_QUIT";
    public static final String RSP_SESSION_ENDED = "SESSION_ENDED";
    public static final String RSP_ERROR = "ERROR";

    public static final String COLOR_WHITE = "WHITE";
    public static final String COLOR_BLACK = "BLACK";

    public static final String SEPARATOR = ":";

    public static String createMessage(String command, String data) {
        if (data != null && !data.isEmpty()) {
            return command + SEPARATOR + data;
        }
        return command;
    }

    public static String[] parseMessage(String message) {
        if (message == null) {
            return new String[]{"", ""};
        }
        String[] parts = message.split(SEPARATOR, 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return parts;
    }

    public static int[] parseMoveData(String moveData) {
        if (moveData == null || !moveData.matches("\\d+,\\d+->\\d+,\\d+")) {
            return null;
        }
        try {
            String[] positions = moveData.split("->");
            String[] fromCoords = positions[0].split(",");
            String[] toCoords = positions[1].split(",");

            return new int[]{
                    Integer.parseInt(fromCoords[0]), // fromCol
                    Integer.parseInt(fromCoords[1]), // fromRow
                    Integer.parseInt(toCoords[0]),   // toCol
                    Integer.parseInt(toCoords[1])    // toRow
            };
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static String createTimeUpdateMessage(long whiteSeconds, long blackSeconds, String currentTurnColor) {
        return RSP_TIME_UPDATE + SEPARATOR + whiteSeconds + SEPARATOR + blackSeconds + SEPARATOR + currentTurnColor;
    }

    public static Object[] parseTimeUpdateMessage(String messageData) {
        // messageData to "whiteSeconds:blackSeconds:currentTurnColor"
        try {
            String[] parts = messageData.split(SEPARATOR);
            if (parts.length == 3) {
                long whiteSeconds = Long.parseLong(parts[0]);
                long blackSeconds = Long.parseLong(parts[1]);
                String currentTurn = parts[2];
                return new Object[]{whiteSeconds, blackSeconds, currentTurn};
            }
        } catch (NumberFormatException e) {
            // logger.error("Błąd parsowania wiadomości czasu: " + messageData, e);
        }
        return null;
    }
}