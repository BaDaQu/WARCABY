package warcaby.gui;

import warcaby.gamelogic.PlayerColor; // Tylko dla PlayerColor

import javax.swing.*;
import java.awt.*;

public class InfoPanel extends JPanel {
    private JLabel currentPlayerLabel;
    private JLabel whiteTimeLabel;
    private JLabel blackTimeLabel;

    public InfoPanel() {
        setLayout(new GridLayout(1, 3, 10, 0));
        setBackground(Color.LIGHT_GRAY);
        setPreferredSize(new Dimension(1, 40)); // Szerokość zostanie dopasowana

        currentPlayerLabel = new JLabel("Ruch: ", SwingConstants.CENTER);
        whiteTimeLabel = new JLabel("Białe: 00:00", SwingConstants.CENTER);
        blackTimeLabel = new JLabel("Czarne: 00:00", SwingConstants.CENTER);

        Font labelFont = new Font("Arial", Font.BOLD, 14);
        currentPlayerLabel.setFont(labelFont);
        whiteTimeLabel.setFont(labelFont);
        blackTimeLabel.setFont(labelFont);

        add(currentPlayerLabel);
        add(whiteTimeLabel);
        add(blackTimeLabel);
    }

    public void updatePlayerInfo(PlayerColor currentPlayer) {
        if (currentPlayer != null) {
            currentPlayerLabel.setText("Ruch: " + (currentPlayer == PlayerColor.WHITE ? "BIAŁE" : "CZARNE"));
        } else {
            currentPlayerLabel.setText("Ruch: -");
        }
    }

    public void updateWhiteTime(long totalSeconds) {
        whiteTimeLabel.setText("Białe: " + formatTime(totalSeconds));
    }

    public void updateBlackTime(long totalSeconds) {
        blackTimeLabel.setText("Czarne: " + formatTime(totalSeconds));
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void resetInfo(PlayerColor startingPlayer) {
        updatePlayerInfo(startingPlayer);
        updateWhiteTime(0);
        updateBlackTime(0);
    }
}