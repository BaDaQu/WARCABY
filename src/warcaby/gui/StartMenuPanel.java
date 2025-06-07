package warcaby.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class StartMenuPanel extends JPanel {

    private RoundedButton startGameButton;
    private RoundedButton instructionsButton;
    private RoundedButton exitButton;

    private static final int MENU_BG_SQUARE_SIZE = 60;
    private final Color lightSquareColor = new Color(230, 200, 160);
    private final Color darkSquareColor = new Color(160, 100, 40);
    private final Color panelOuterBackgroundColor = new Color(40, 40, 40);

    public StartMenuPanel() {
        setOpaque(true);
        setBackground(panelOuterBackgroundColor);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 50, 10, 50);

        Font buttonFont = new Font("Arial", Font.BOLD, 18);
        Dimension buttonSize = new Dimension(300, 50);
        int buttonCornerRadius = 30;

        gbc.weighty = 0.5;
        add(Box.createGlue(), gbc);
        gbc.weighty = 0;

        JLabel titleLabel = new JLabel("WARCABY", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(10, 50, 30, 50);
        add(titleLabel, gbc);

        gbc.insets = new Insets(10, 50, 10, 50);

        startGameButton = createMenuButton("Rozpocznij Grę", buttonFont, buttonSize, buttonCornerRadius);
        add(startGameButton, gbc);

        instructionsButton = createMenuButton("Instrukcja Gry", buttonFont, buttonSize, buttonCornerRadius);
        add(instructionsButton, gbc);

        exitButton = createMenuButton("Wyłącz Grę", buttonFont, buttonSize, buttonCornerRadius);
        add(exitButton, gbc);

        gbc.weighty = 0.5;
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

    public void addStartGameListener(ActionListener listener) {
        startGameButton.addActionListener(listener);
    }

    public void addInstructionsListener(ActionListener listener) {
        instructionsButton.addActionListener(listener);
    }

    public void addExitListener(ActionListener listener) {
        exitButton.addActionListener(listener);
    }
}