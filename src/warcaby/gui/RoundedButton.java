package warcaby.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedButton extends JButton {
    private Color backgroundColor;
    private Color hoverBackgroundColor;
    private Color pressedBackgroundColor;
    private int cornerRadius = 15; // Domyślny promień zaokrąglenia

    public RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false); // Ważne: nie rysujemy domyślnego tła
        setFocusPainted(false);      // Nie rysujemy ramki focusa
        setBorderPainted(false);     // Nie rysujemy domyślnej ramki

        // Domyślne kolory
        this.backgroundColor = new Color(100, 100, 100);
        this.hoverBackgroundColor = new Color(120, 120, 120);
        this.pressedBackgroundColor = new Color(80, 80, 80);
        setForeground(Color.WHITE); // Domyślny kolor tekstu

        // Efekt Hover - repaint jest potrzebny, aby paintComponent użył nowego koloru
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                repaint();
            }
        });
    }

    public RoundedButton(String text, int cornerRadius) {
        this(text);
        this.cornerRadius = cornerRadius;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setHoverBackgroundColor(Color color) {
        this.hoverBackgroundColor = color;
    }

    public Color getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    public void setPressedBackgroundColor(Color color) {
        this.pressedBackgroundColor = color;
    }

    public Color getPressedBackgroundColor() {
        return pressedBackgroundColor;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color colorToDraw;
        ButtonModel model = getModel();

        if (model.isPressed()) {
            colorToDraw = pressedBackgroundColor;
        } else if (model.isRollover()) {
            colorToDraw = hoverBackgroundColor;
        } else {
            colorToDraw = backgroundColor;
        }

        g2.setColor(colorToDraw);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        g2.dispose();

        super.paintComponent(g); // Rysuje tekst przycisku
    }
}