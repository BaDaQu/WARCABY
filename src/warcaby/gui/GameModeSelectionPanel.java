package warcaby.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GameModeSelectionPanel extends JPanel {

    private RoundedButton localVsComputerButton;
    private RoundedButton localMultiplayerButton;
    private RoundedButton onlineMultiplayerButton;
    private RoundedButton backToMainMenuButton;

    private static final int MENU_BG_SQUARE_SIZE = 60;
    private final Color lightSquareColor = new Color(230, 200, 160);
    private final Color darkSquareColor = new Color(160, 100, 40);
    private final Color panelOuterBackgroundColor = new Color(45, 45, 45);

    public GameModeSelectionPanel() {
        setOpaque(true);
        setBackground(panelOuterBackgroundColor);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 40, 10, 40);

        Font buttonFont = new Font("Arial", Font.BOLD, 16);
        Dimension buttonSize = new Dimension(280, 45);
        int buttonCornerRadius = 25;

        gbc.weighty = 0.4;
        add(Box.createGlue(), gbc);
        gbc.weighty = 0;

        JLabel titleLabel = new JLabel("Wybierz Tryb Gry", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(10, 40, 20, 40);
        add(titleLabel, gbc);

        gbc.insets = new Insets(10, 40, 10, 40);

        localVsComputerButton = createMenuButton("Lokalnie z Komputerem", buttonFont, buttonSize, buttonCornerRadius);
        add(localVsComputerButton, gbc);

        localMultiplayerButton = createMenuButton("Lokalny Multiplayer", buttonFont, buttonSize, buttonCornerRadius);
        add(localMultiplayerButton, gbc);

        onlineMultiplayerButton = createMenuButton("Multiplayer Online", buttonFont, buttonSize, buttonCornerRadius);
        add(onlineMultiplayerButton, gbc);

        gbc.insets = new Insets(20, 40, 10, 40);
        backToMainMenuButton = createMenuButton("Powrót do Menu Głównego", buttonFont, buttonSize, buttonCornerRadius);
        backToMainMenuButton.setBackgroundColor(new Color(120, 60, 60));
        backToMainMenuButton.setHoverBackgroundColor(new Color(140, 80, 80));
        backToMainMenuButton.setPressedBackgroundColor(new Color(100, 40, 40));
        add(backToMainMenuButton, gbc);

        gbc.weighty = 0.6;
        add(Box.createGlue(), gbc);
    }

    private RoundedButton createMenuButton(String text, Font font, Dimension size, int cornerRadius) {
        RoundedButton button = new RoundedButton(text, cornerRadius);
        button.setFont(font);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int numColsToFit = panelWidth / MENU_BG_SQUARE_SIZE;
        int numRowsToFit = panelHeight / MENU_BG_SQUARE_SIZE;
        int gridDrawingWidth = numColsToFit * MENU_BG_SQUARE_SIZE;
        int gridDrawingHeight = numRowsToFit * MENU_BG_SQUARE_SIZE;
        int offsetX = (panelWidth - gridDrawingWidth) / 2;
        int offsetY = (panelHeight - gridDrawingHeight) / 2;
        for (int row = 0; row < numRowsToFit; row++) {
            for (int col = 0; col < numColsToFit; col++) {
                if ((row + col) % 2 == 0) {
                    g2d.setColor(lightSquareColor);
                } else {
                    g2d.setColor(darkSquareColor);
                }
                g2d.fillRect(offsetX + col * MENU_BG_SQUARE_SIZE,
                        offsetY + row * MENU_BG_SQUARE_SIZE,
                        MENU_BG_SQUARE_SIZE, MENU_BG_SQUARE_SIZE);
            }
        }
        g2d.dispose();
    }

    public void addLocalMultiplayerListener(ActionListener listener) {
        localMultiplayerButton.addActionListener(listener);
    }

    public void addLocalVsComputerListener(ActionListener listener) {
        localVsComputerButton.addActionListener(listener);
    }

    public void addOnlineMultiplayerListener(ActionListener listener) {
        onlineMultiplayerButton.addActionListener(listener);
    }

    public void addBackToMainMenuListener(ActionListener listener) {
        backToMainMenuButton.addActionListener(listener);
    }
}