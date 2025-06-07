# Gra w Warcaby w Javie

Klasyczna gra w warcaby zaimplementowana w języku Java z wykorzystaniem biblioteki Swing do obsługi interfejsu graficznego.

## Główne Funkcjonalności

  **Rozgrywka Lokalna**: Umożliwia grę dla dwóch graczy na jednym komputerze.\n
  **Gra z Komputerem**: Pozwala na rozgrywkę przeciwko prostej sztucznej inteligencji.
  **Gra Sieciowa (Multiplayer Online)**: Wspiera rozgrywkę między dwoma graczami na różnych komputerach poprzez połączenie klient-serwer.
  **Interfejs Graficzny Użytkownika (GUI)**: Intuicyjny interfejs oparty na bibliotece Swing, obejmujący menu startowe, wybór trybu gry oraz planszę.
  **Logika Gry**: Zaimplementowano pełne zasady warcabów, w tym ruchy pionków i damek, obowiązkowe bicia (również wielokrotne) oraz długie bicia damek.
  **Pomiar Czasu**: Gra mierzy i wyświetla czas ruchów wykonywanych przez każdego z graczy.
  **Walidacja Ruchów**: System kontroluje poprawność wszystkich wykonywanych ruchów zgodnie z zasadami gry.
  **Struktura Projektu**: Kod został podzielony na logiczne pakiety (gamelogic, gui, network, utils, main) dla lepszej organizacji i czytelności.
  **Sztuczna Inteligencja**: Podstawowy przeciwnik komputerowy wybiera ruchy na podstawie hierarchii priorytetów (np. preferowanie bić i promocji).
  **Technologie**: Projekt wykorzystuje Javę, Swing do GUI oraz standardowe gniazda sieciowe (java.net) do komunikacji w trybie multiplayer.

## Struktura Projektu

```
src
└── warcaby
    ├── ai
    │   └── ComputerPlayer.java
    ├── gamelogic
    │   ├── boardcomponents
    │   │   ├── BoardState.java
    │   │   ├── GameStatusChecker.java
    │   │   ├── Move.java
    │   │   ├── MoveExecutor.java
    │   │   ├── MoveLogic.java
    │   │   ├── MoveValidator.java
    │   │   └── TurnManager.java
    │   ├── Board.java
    │   ├── Piece.java
    │   ├── PieceType.java
    │   └── PlayerColor.java
    ├── gui
    │   ├── frame
    │   │   ├── CheckersFrame.java
    │   │   ├── FrameViewManager.java
    │   │   └── OnlineGameUIManager.java
    │   ├── BoardPanel.java
    │   ├── GameModeSelectionPanel.java
    │   ├── InfoPanel.java
    │   ├── RoundedButton.java
    │   └── StartMenuPanel.java
    ├── main
    │   └── Main.java
    ├── network
    │   ├── CheckersClient.java
    │   ├── NetworkProtocol.java
    │   └── Server.java
    └── utils
        ├── ApplicationConfig.java
        ├── GameConstants.java
        └── Logger.java
```

## Uruchamianie
Aby uruchomić grę, skompiluj projekt i uruchom klasę `warcaby.main.Main`. Dla gry sieciowej, najpierw uruchom `warcaby.network.Server`.
