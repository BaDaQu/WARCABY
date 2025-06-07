package warcaby.gui;

import warcaby.gamelogic.Board;
import warcaby.ai.ComputerPlayer;
import warcaby.gamelogic.Piece;
import warcaby.gamelogic.PieceType;
import warcaby.gamelogic.PlayerColor;
import warcaby.gamelogic.boardcomponents.Move;
import warcaby.gui.frame.CheckersFrame;
import warcaby.network.CheckersClient;
import warcaby.utils.Logger;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BoardPanel extends JPanel {
    private final Board gameBoard;
    private final InfoPanel infoPanel;
    public static final int SQUARE_SIZE = 70;
    public static final int BOARD_DIMENSION = Board.SIZE * SQUARE_SIZE;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Move> possibleMovesForSelectedPiece = null;
    private boolean mandatoryJumpExistsForCurrentPlayer = false;

    public Timer gameTimer;
    private long whitePlayerTimeSeconds = 0;
    private long blackPlayerTimeSeconds = 0;

    private CheckersFrame mainFrame;
    private JButton surrenderButton;

    private boolean isOnlineGameMode = false;
    private PlayerColor myOnlineColor = null;
    private static final Logger logger = new Logger(BoardPanel.class);

    private boolean playingWithComputer = false;
    private ComputerPlayer computerAI;
    private PlayerColor computerColor = PlayerColor.BLACK;
    private Timer computerMoveTimer;

    public BoardPanel(Board board, InfoPanel infoPanel, CheckersFrame mainFrame) {
        this.gameBoard = board;
        this.infoPanel = infoPanel;
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(BOARD_DIMENSION, BOARD_DIMENSION));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
        // Inicjalizacje zostaną wywołane przez resetGame()
    }

    public void setOnlineGameMode(boolean isOnline, PlayerColor myColor) {
        this.isOnlineGameMode = isOnline;
        this.myOnlineColor = myColor;
        if (isOnline) this.playingWithComputer = false;
        updateSurrenderButtonText();
    }

    public void setComputerGameMode(boolean vsComputer) {
        this.playingWithComputer = vsComputer;
        if (vsComputer) {
            this.isOnlineGameMode = false;
            if (computerAI == null) {
                computerAI = new ComputerPlayer(computerColor);
            }
            logger.info("BoardPanel: Tryb gry z komputerem włączony. Komputer gra jako " + computerColor);
        } else {
            logger.info("BoardPanel: Tryb gry z komputerem wyłączony.");
        }
        updateSurrenderButtonText();
    }

    private void updateSurrenderButtonText() {
        if (getSurrenderButton() != null) {
            String text = "Poddaj Grę / Menu";
            if (isOnlineGameMode) text = "Poddaj Grę Online";
            else if (playingWithComputer) text = "Poddaj Grę (vs AI)";
            getSurrenderButton().setText(text);
        }
    }

    private void initializeTimer() {
        if (gameTimer == null) {
            gameTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gameBoard.getWinner() != null && !isOnlineGameMode) {
                        stopGameTime();
                        return;
                    }
                    if (!isOnlineGameMode) {
                        if (gameBoard.getCurrentPlayer() == PlayerColor.WHITE) {
                            whitePlayerTimeSeconds++;
                            infoPanel.updateWhiteTime(whitePlayerTimeSeconds);
                        } else if (gameBoard.getCurrentPlayer() == PlayerColor.BLACK) {
                            blackPlayerTimeSeconds++;
                            infoPanel.updateBlackTime(blackPlayerTimeSeconds);
                        }
                    }
                }
            });
        }
    }

    private void initializeComputerMoveTimer() {
        if (computerMoveTimer == null) {
            computerMoveTimer = new Timer(1000, e -> executeComputerMove());
            computerMoveTimer.setRepeats(false);
        }
    }

    public void startGameTime() {
        initializeTimer();
        if (gameTimer != null && !gameTimer.isRunning() && !isOnlineGameMode) {
            gameTimer.start();
        }
        infoPanel.updatePlayerInfo(gameBoard.getCurrentPlayer());
        if (!isOnlineGameMode) {
            infoPanel.updateWhiteTime(whitePlayerTimeSeconds);
            infoPanel.updateBlackTime(blackPlayerTimeSeconds);
        } else {
            infoPanel.updateWhiteTime(0);
            infoPanel.updateBlackTime(0);
        }
    }

    public void stopGameTime() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
    }

    private void switchPlayerTime() {
        if (!isOnlineGameMode) {
            stopGameTime();
            startGameTime();
        }
    }

    public void updateMandatoryJumpStatus() {
        if ((isOnlineGameMode && myOnlineColor == null) || gameBoard.getWinner() != null) {
            mandatoryJumpExistsForCurrentPlayer = false;
            return;
        }
        PlayerColor playerToCheck = gameBoard.getCurrentPlayer();
        if (isOnlineGameMode && playerToCheck != myOnlineColor && selectedRow == -1) {
            mandatoryJumpExistsForCurrentPlayer = false;
            return;
        }
        if (playerToCheck == null) return;

        List<Move> allMoves = gameBoard.getAllValidMovesForPlayer(playerToCheck);
        if (allMoves != null && !allMoves.isEmpty()) {
            mandatoryJumpExistsForCurrentPlayer = allMoves.get(0).isJump;
        } else {
            mandatoryJumpExistsForCurrentPlayer = false;
        }
    }

    private void handleSurrender() {
        if (isOnlineGameMode) {
            if (gameBoard.getWinner() != null && gameBoard.forcedWinner == null) {
                mainFrame.showStartMenu();
                return;
            }
            int response = JOptionPane.showConfirmDialog(this,
                    "Czy na pewno chcesz poddać partię online?",
                    "Poddanie Partii Online",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                CheckersClient client = mainFrame.getClient();
                if (client != null && client.isConnected()) {
                    client.quitGame();
                }
            }
            return;
        }

        if (gameBoard.getWinner() != null) {
            mainFrame.showStartMenu();
            return;
        }
        PlayerColor currentPlayer = gameBoard.getCurrentPlayer();
        String playerName = (currentPlayer == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE";
        int response = JOptionPane.showConfirmDialog(this,
                "Graczu " + playerName + ", czy na pewno chcesz poddać partię?\n" +
                        "Przeciwnik wygra.",
                "Poddanie Partii",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            stopGameTime();
            gameBoard.surrenderGame();
            showGameOverDialog(gameBoard.getWinner());
        }
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        if (gameBoard.getWinner() != null) return;

        if (isOnlineGameMode && gameBoard.getCurrentPlayer() != myOnlineColor) {
            logger.info("BoardPanel: Kliknięcie zignorowane, nie tura gracza online (Lokalnie: " + gameBoard.getCurrentPlayer() + ", Mój kolor: " + myOnlineColor + ").");
            return;
        }
        if (playingWithComputer && gameBoard.getCurrentPlayer() == computerColor) {
            logger.info("BoardPanel: Kliknięcie zignorowane, tura komputera.");
            return;
        }

        int col = mouseX / SQUARE_SIZE;
        int row = mouseY / SQUARE_SIZE;

        if (!gameBoard.isValidCoordinate(row, col)) return;

        Piece clickedPiece = gameBoard.getPiece(row, col);
        PlayerColor currentPlayerForLogic = gameBoard.getCurrentPlayer();
        PlayerColor playerBeforeMove = currentPlayerForLogic;
        boolean movePerformedThisClick = false;

        if (selectedRow == -1 && selectedCol == -1) {
            if (clickedPiece != null && clickedPiece.getColor() == currentPlayerForLogic) {
                if (gameBoard.isJumpMadeThisTurn() &&
                        (row != gameBoard.getLastJumpingPieceRow() || col != gameBoard.getLastJumpingPieceCol())) {
                    JOptionPane.showMessageDialog(this, "Musisz kontynuować bicie pionkiem z pozycji (" + gameBoard.getLastJumpingPieceRow() + "," + gameBoard.getLastJumpingPieceCol() + ").", "Kontynuacja Bicia", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                updateMandatoryJumpStatus();
                if (mandatoryJumpExistsForCurrentPlayer && !gameBoard.isJumpMadeThisTurn()) {
                    boolean canThisPieceJump = false;
                    List<Move> allJumps = gameBoard.getAllValidMovesForPlayer(currentPlayerForLogic);
                    if (allJumps != null) {
                        for (Move jump : allJumps) { if (jump.fromRow == row && jump.fromCol == col) { canThisPieceJump = true; break; } }
                    }
                    if (!canThisPieceJump) {
                        JOptionPane.showMessageDialog(this, "Obowiązkowe bicie! Wybierz pionka, który może bić.", "Obowiązkowe Bicie", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                selectedRow = row;
                selectedCol = col;
                possibleMovesForSelectedPiece = getValidMovesForPiece(selectedRow, selectedCol);
            }
        } else {
            boolean targetClicked = false;
            if (possibleMovesForSelectedPiece != null) {
                for (Move currentMoveObject : possibleMovesForSelectedPiece) {
                    if (currentMoveObject.toRow == row && currentMoveObject.toCol == col) {
                        targetClicked = true;
                        boolean localMoveSuccess = gameBoard.makeMove(selectedRow, selectedCol, currentMoveObject.toRow, currentMoveObject.toCol);

                        if (localMoveSuccess) {
                            movePerformedThisClick = true;
                            logger.info("BoardPanel: Ruch lokalny wykonany: (" + selectedRow + "," + selectedCol + ") -> (" + currentMoveObject.toRow + "," + currentMoveObject.toCol + "). Nowa tura (po ruchu lokalnym): " + gameBoard.getCurrentPlayer());

                            if (isOnlineGameMode) {
                                String moveData = selectedCol + "," + selectedRow + "->" + currentMoveObject.toCol + "," + currentMoveObject.toRow;
                                CheckersClient client = mainFrame.getClient();
                                if (client != null && client.isConnected()) {
                                    boolean isNowContinuation = gameBoard.isJumpMadeThisTurn() &&
                                            currentMoveObject.toRow == gameBoard.getLastJumpingPieceRow() &&
                                            currentMoveObject.toCol == gameBoard.getLastJumpingPieceCol();
                                    if (isNowContinuation) {
                                        client.sendCaptureContinued(moveData);
                                    } else {
                                        client.sendMove(moveData);
                                    }
                                }
                            }

                            if (gameBoard.isJumpMadeThisTurn() && gameBoard.getLastJumpingPieceRow() == currentMoveObject.toRow && gameBoard.getLastJumpingPieceCol() == currentMoveObject.toCol) {
                                selectedRow = currentMoveObject.toRow;
                                selectedCol = currentMoveObject.toCol;
                                possibleMovesForSelectedPiece = getValidMovesForPiece(selectedRow, selectedCol);
                                if (possibleMovesForSelectedPiece.isEmpty()) {
                                    deselectPiece();
                                }
                            } else {
                                deselectPiece();
                            }
                        } else {
                            String errorMessage = gameBoard.getLastMoveValidationError();
                            if (errorMessage != null) { JOptionPane.showMessageDialog(this, errorMessage, "Nieprawidłowy Ruch", JOptionPane.ERROR_MESSAGE); }
                            else { JOptionPane.showMessageDialog(this, "Nieprawidłowy ruch.", "Błąd Ruchu", JOptionPane.ERROR_MESSAGE); }
                        }
                        break;
                    }
                }
            }
            if (!targetClicked) {
                if (clickedPiece != null && clickedPiece.getColor() == currentPlayerForLogic) {
                    updateMandatoryJumpStatus();
                    if (mandatoryJumpExistsForCurrentPlayer && !gameBoard.isJumpMadeThisTurn()) {
                        boolean canNewPieceJump = false;
                        List<Move> allJumps = gameBoard.getAllValidMovesForPlayer(currentPlayerForLogic);
                        if (allJumps != null) {
                            for (Move jump : allJumps) { if (jump.fromRow == row && jump.fromCol == col) { canNewPieceJump = true; break; } }
                        }
                        if (canNewPieceJump) {
                            selectedRow = row; selectedCol = col; possibleMovesForSelectedPiece = getValidMovesForPiece(selectedRow, selectedCol);
                        } else {
                            JOptionPane.showMessageDialog(this, "Obowiązkowe bicie! Wybierz pionka, który może bić.", "Obowiązkowe Bicie", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        selectedRow = row; selectedCol = col; possibleMovesForSelectedPiece = getValidMovesForPiece(selectedRow, selectedCol);
                    }
                } else {
                    deselectPiece();
                }
            }
        }

        if (movePerformedThisClick) {
            boolean turnPassedToNext = (gameBoard.getCurrentPlayer() != playerBeforeMove) ||
                    (!gameBoard.isJumpMadeThisTurn() && playerBeforeMove == gameBoard.getCurrentPlayer()) ||
                    (gameBoard.isJumpMadeThisTurn() && playerBeforeMove == gameBoard.getCurrentPlayer() && (possibleMovesForSelectedPiece == null || possibleMovesForSelectedPiece.isEmpty()));

            if (!isOnlineGameMode) {
                if (turnPassedToNext) {
                    switchPlayerTime();
                }
                infoPanel.updatePlayerInfo(gameBoard.getCurrentPlayer());
                infoPanel.updateWhiteTime(whitePlayerTimeSeconds);
                infoPanel.updateBlackTime(blackPlayerTimeSeconds);
            } else {
                if (myOnlineColor == PlayerColor.WHITE) infoPanel.updateWhiteTime(whitePlayerTimeSeconds);
                else infoPanel.updateBlackTime(blackPlayerTimeSeconds);
            }

            updateMandatoryJumpStatus();
            repaint();

            if (playingWithComputer && gameBoard.getCurrentPlayer() == computerColor && gameBoard.getWinner() == null) {
                if (computerMoveTimer.isRunning()) computerMoveTimer.restart();
                else computerMoveTimer.start();
            } else if (!isOnlineGameMode) {
                PlayerColor winner = gameBoard.getWinner();
                if (winner != null) {
                    stopGameTime();
                    showGameOverDialog(winner);
                }
            }
        } else {
            updateMandatoryJumpStatus();
            repaint();
        }
    }

    private void executeComputerMove() {
        if (!playingWithComputer || gameBoard.getCurrentPlayer() != computerColor || gameBoard.getWinner() != null) {
            return;
        }
        logger.info("Komputer (" + computerColor + ") wykonuje ruch...");
        Move computerMove = computerAI.getComputerMove(gameBoard);

        if (computerMove != null) {
            boolean success = gameBoard.makeMove(computerMove.fromRow, computerMove.fromCol, computerMove.toRow, computerMove.toCol);
            if (success) {
                logger.info("Komputer wykonał ruch: " + computerMove);
                boolean canComputerJumpAgain = gameBoard.isJumpMadeThisTurn() &&
                        gameBoard.getCurrentPlayer() == computerColor &&
                        !gameBoard.getAllValidMovesForPlayer(computerColor).isEmpty() &&
                        gameBoard.getAllValidMovesForPlayer(computerColor).get(0).isJump;

                if (canComputerJumpAgain) {
                    logger.info("Komputer kontynuuje bicie.");
                    if (computerMoveTimer.isRunning()) computerMoveTimer.restart();
                    else computerMoveTimer.start();
                } else {
                    logger.info("Komputer zakończył turę. Nowa tura: " + gameBoard.getCurrentPlayer());
                    switchPlayerTime();
                }
            } else {
                logger.error("Komputer próbował wykonać nieprawidłowy ruch: " + computerMove + ". Błąd: " + gameBoard.getLastMoveValidationError());
            }
        } else {
            logger.info("Komputer nie ma dostępnych ruchów.");
        }

        infoPanel.updatePlayerInfo(gameBoard.getCurrentPlayer());
        infoPanel.updateWhiteTime(whitePlayerTimeSeconds);
        infoPanel.updateBlackTime(blackPlayerTimeSeconds);
        updateMandatoryJumpStatus();
        repaint();

        PlayerColor winner = gameBoard.getWinner();
        if (winner != null) {
            stopGameTime();
            if(computerMoveTimer.isRunning()) computerMoveTimer.stop();
            showGameOverDialog(winner);
        }
    }

    public void resetGame() {
        initializeTimer();
        initializeComputerMoveTimer();
        if (computerAI == null && playingWithComputer) {
            computerAI = new ComputerPlayer(computerColor);
        }

        stopGameTime();
        if (computerMoveTimer != null && computerMoveTimer.isRunning()) {
            computerMoveTimer.stop();
        }
        gameBoard.initializeBoard();
        deselectPiece();
        whitePlayerTimeSeconds = 0;
        blackPlayerTimeSeconds = 0;
        infoPanel.resetInfo(gameBoard.getCurrentPlayer());
        updateMandatoryJumpStatus();

        if (!isOnlineGameMode && !playingWithComputer) {
            startGameTime();
        } else if (playingWithComputer) {
            if (gameBoard.getCurrentPlayer() == PlayerColor.WHITE) {
                startGameTime();
            } else {
                infoPanel.updatePlayerInfo(computerColor);
                infoPanel.updateWhiteTime(0);
                infoPanel.updateBlackTime(0);
                if (computerMoveTimer.isRunning()) computerMoveTimer.restart(); else computerMoveTimer.start();
            }
        } else {
            infoPanel.updateWhiteTime(0);
            infoPanel.updateBlackTime(0);
            infoPanel.updatePlayerInfo(PlayerColor.WHITE);
        }
        repaint();
    }

    private List<Move> getValidMovesForPiece(int r, int c) {
        if (gameBoard.getWinner() != null && !isOnlineGameMode) return new ArrayList<>();
        PlayerColor playerToCheck = gameBoard.getCurrentPlayer();

        if (isOnlineGameMode && playerToCheck != myOnlineColor) {
            return new ArrayList<>();
        }
        if (playerToCheck == null) return new ArrayList<>();

        List<Move> allPlayerMoves = gameBoard.getAllValidMovesForPlayer(playerToCheck);
        List<Move> pieceMoves = new ArrayList<>();
        if (allPlayerMoves != null) {
            for (Move move : allPlayerMoves) {
                if (move.fromRow == r && move.fromCol == c) {
                    pieceMoves.add(move);
                }
            }
        }
        return pieceMoves;
    }

    private void deselectPiece() {
        selectedRow = -1;
        selectedCol = -1;
        possibleMovesForSelectedPiece = null;
    }

    private void showGameOverDialog(PlayerColor winner) {
        String message;
        if (winner == null || winner == PlayerColor.NONE) {
            message = "Gra zakończona bez rozstrzygnięcia lub remis.";
        } else {
            if(gameBoard.forcedWinner != null && gameBoard.forcedWinner == winner){
                PlayerColor loser = (winner == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
                String loserName = (loser == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE";
                String winnerName = (winner == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE";
                message = "Gracz " + loserName + " poddał partię.\nWygrywają " + winnerName + "!";
            } else {
                String winnerName = (winner == PlayerColor.WHITE) ? "BIAŁE" : "CZARNE";
                message = "Wygrywają " + winnerName + "!";
            }
        }
        JOptionPane.showMessageDialog(this, message, "Koniec Gry", JOptionPane.INFORMATION_MESSAGE);
        mainFrame.showStartMenu();
    }

    public JButton getSurrenderButton() {
        if (surrenderButton == null) {
            surrenderButton = new JButton("Poddaj Grę / Menu");
            surrenderButton.setFont(new Font("Arial", Font.PLAIN, 12));
            surrenderButton.setFocusPainted(false);
            surrenderButton.addActionListener(e -> handleSurrender());
        }
        return surrenderButton;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (int loopRow = 0; loopRow < Board.SIZE; loopRow++) {
            for (int loopCol = 0; loopCol < Board.SIZE; loopCol++) {
                if ((loopRow + loopCol) % 2 == 0) {
                    g2d.setColor(new Color(230, 200, 160));
                } else {
                    g2d.setColor(new Color(160, 100, 40));
                }
                g2d.fillRect(loopCol * SQUARE_SIZE, loopRow * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);

                PlayerColor playerForHighlight = gameBoard.getCurrentPlayer();
                if (isOnlineGameMode && playerForHighlight != myOnlineColor) {
                    // Nie podświetlaj
                } else if (playerForHighlight != null && selectedRow == -1 && mandatoryJumpExistsForCurrentPlayer && !gameBoard.isJumpMadeThisTurn()) {
                    Piece p = gameBoard.getPiece(loopRow, loopCol);
                    if (p != null && p.getColor() == playerForHighlight) {
                        boolean canThisPieceJump = false;
                        List<Move> allPlayerMoves = gameBoard.getAllValidMovesForPlayer(playerForHighlight);
                        if (allPlayerMoves != null) {
                            for(Move currentMove : allPlayerMoves) {
                                if (currentMove.fromRow == loopRow && currentMove.fromCol == loopCol && currentMove.isJump) {
                                    canThisPieceJump = true;
                                    break;
                                }
                            }
                        }
                        if (canThisPieceJump) {
                            g2d.setColor(new Color(255, 165, 0, 120));
                            g2d.fillOval(loopCol * SQUARE_SIZE + 3, loopRow * SQUARE_SIZE + 3, SQUARE_SIZE - 6, SQUARE_SIZE - 6);
                        }
                    }
                }

                if (loopRow == selectedRow && loopCol == selectedCol) {
                    if (!isOnlineGameMode || (isOnlineGameMode && gameBoard.getCurrentPlayer() == myOnlineColor)) {
                        g2d.setColor(Color.CYAN);
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRect(loopCol * SQUARE_SIZE + 2, loopRow * SQUARE_SIZE + 2, SQUARE_SIZE - 4, SQUARE_SIZE - 4);
                    }
                }

                if (possibleMovesForSelectedPiece != null) {
                    if (!isOnlineGameMode || (isOnlineGameMode && gameBoard.getCurrentPlayer() == myOnlineColor)) {
                        for (Move move : possibleMovesForSelectedPiece) {
                            if (move.toRow == loopRow && move.toCol == loopCol) {
                                g2d.setColor(new Color(0, 255, 0, 150));
                                g2d.fillOval(loopCol * SQUARE_SIZE + SQUARE_SIZE / 2 - 8, loopRow * SQUARE_SIZE + SQUARE_SIZE / 2 - 8, 16, 16);
                                if (move.isJump) {
                                    g2d.setColor(new Color(255, 0, 0, 180));
                                    g2d.setStroke(new BasicStroke(2));
                                    g2d.drawOval(loopCol * SQUARE_SIZE + SQUARE_SIZE / 4, loopRow * SQUARE_SIZE + SQUARE_SIZE / 4, SQUARE_SIZE / 2, SQUARE_SIZE / 2);
                                }
                            }
                        }
                    }
                }

                Piece piece = gameBoard.getPiece(loopRow, loopCol);
                if (piece != null) {
                    int pieceDiameter = (int) (SQUARE_SIZE * 0.80);
                    int margin = (SQUARE_SIZE - pieceDiameter) / 2;
                    int pieceX = loopCol * SQUARE_SIZE + margin;
                    int pieceY = loopRow * SQUARE_SIZE + margin;

                    if (piece.getColor() == PlayerColor.WHITE) {
                        g2d.setColor(Color.WHITE);
                    } else {
                        g2d.setColor(Color.DARK_GRAY);
                    }
                    g2d.fillOval(pieceX, pieceY, pieceDiameter, pieceDiameter);

                    g2d.setColor(piece.getColor() == PlayerColor.WHITE ? Color.GRAY : Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(pieceX, pieceY, pieceDiameter, pieceDiameter);

                    if (piece.getType() == PieceType.KING) {
                        g2d.setColor(piece.getColor() == PlayerColor.WHITE ? Color.DARK_GRAY : Color.WHITE);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.setFont(new Font("Arial", Font.BOLD, (int)(pieceDiameter * 0.6)));
                        String kingMark = "K";
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(kingMark);
                        int textX = pieceX + (pieceDiameter - textWidth) / 2;
                        int textY = pieceY + ((pieceDiameter - fm.getHeight()) / 2) + fm.getAscent();
                        g2d.drawString(kingMark, textX, textY);
                    }
                }
            }
        }
    }
}