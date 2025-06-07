package warcaby.gui.frame;

import javax.swing.*;
import java.awt.*;

/**
 * Klasa FrameViewManager jest odpowiedzialna za zarządzanie widokami (panelami)
 * w głównej ramce aplikacji, używając mechanizmu CardLayout.
 * Umożliwia dynamiczne przełączanie między różnymi ekranami, takimi jak
 * menu startowe, panel wyboru trybu gry, czy główny panel rozgrywki.
 */
public class FrameViewManager {
    private final CardLayout cardLayout; // Obiekt CardLayout używany do przełączania widoków
    private final JPanel mainPanel;      // Główny kontener (JPanel), na którym stosowany jest CardLayout

    // Publiczne stałe identyfikujące poszczególne "karty" (panele) w CardLayout.
    // Używane przez CheckersFrame do określania, który widok ma być pokazany.
    public static final String MENU_PANEL_ID = "MenuPanel";
    public static final String MODE_SELECTION_PANEL_ID = "ModeSelectionPanel";
    public static final String GAME_PANEL_CONTAINER_ID = "GamePanelContainer";

    /**
     * Konstruktor FrameViewManager.
     * @param mainPanel Panel (zazwyczaj główny kontener JFrame), który używa CardLayout.
     * @param cardLayout Instancja CardLayout skojarzona z mainPanel.
     */
    public FrameViewManager(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
    }

    /**
     * Dodaje nowy panel (widok) do kontenera zarządzanego przez CardLayout.
     * Każdy dodany panel jest identyfikowany przez unikalny panelId.
     * @param panel Komponent (zazwyczaj JPanel) do dodania jako nowa "karta".
     * @param panelId Unikalny identyfikator tekstowy dla dodawanego panelu.
     */
    public void addView(Component panel, String panelId) {
        mainPanel.add(panel, panelId);
    }

    /**
     * Wyświetla panel menu startowego.
     * Przełącza widok w CardLayout na kartę zidentyfikowaną przez MENU_PANEL_ID.
     */
    public void showStartMenu() {
        cardLayout.show(mainPanel, MENU_PANEL_ID);
    }

    /**
     * Wyświetla panel wyboru trybu gry.
     * Przełącza widok w CardLayout na kartę zidentyfikowaną przez MODE_SELECTION_PANEL_ID.
     */
    public void showGameModeSelectionPanel() {
        cardLayout.show(mainPanel, MODE_SELECTION_PANEL_ID);
    }

    /**
     * Wyświetla główny kontener panelu gry (który zazwyczaj zawiera planszę i panel informacji).
     * Przełącza widok w CardLayout na kartę zidentyfikowaną przez GAME_PANEL_CONTAINER_ID.
     */
    public void showGamePanelContainer() {
        cardLayout.show(mainPanel, GAME_PANEL_CONTAINER_ID);
    }
}