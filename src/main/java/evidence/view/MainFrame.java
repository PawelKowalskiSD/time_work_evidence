package evidence.view;

import evidence.controller.ApachePoiExcelService;
import evidence.controller.EvidenceController;

import javax.swing.*;
import java.awt.*;

/**
 * Główne okno aplikacji (JFrame), które zarządza nawigacją między różnymi panelami (widokami).
 * Używa {@link CardLayout} do przełączania się między panelem menu, generatorem, edytorem itp.
 */
public class MainFrame extends JFrame implements ViewNavigator {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final EvidenceController excelController = new ApachePoiExcelService();

    private static final String VIEW_MENU = "MENU";
    private static final String VIEW_GENERATOR = "GENERATOR";
    private static final String VIEW_EDITOR = "EDITOR";
    private static final String VIEW_VACATION = "VACATION";

    /**
     * Konstruktor głównego okna aplikacji.
     * Inicjalizuje wszystkie panele, ustawia ich layout i wyświetla widok menu jako domyślny.
     */
    public MainFrame() {
        super("SYSTEM EWIDENCJI CZASU PRACY - PRO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(950, 800);
        setLocationRelativeTo(null);

        MenuPanel menuPanel = new MenuPanel(this);
        GeneratorPanel generatorPanel = new GeneratorPanel(this, excelController);

        EditorPanel editorPanel = new EditorPanel(this);

        mainPanel.add(menuPanel, VIEW_MENU);
        mainPanel.add(generatorPanel, VIEW_GENERATOR);
        mainPanel.add(editorPanel, VIEW_EDITOR);

        add(mainPanel);

        cardLayout.show(mainPanel, VIEW_MENU);
    }

    /**
     * Wyświetla panel menu głównego.
     */
    @Override
    public void showMenu() { cardLayout.show(mainPanel, VIEW_MENU); }

    /**
     * Wyświetla panel generatora nowej ewidencji.
     */
    @Override
    public void showGenerator() { cardLayout.show(mainPanel, VIEW_GENERATOR); }

    /**
     * Wyświetla panel edytora istniejących plików.
     */
    @Override
    public void showEditor() { cardLayout.show(mainPanel, VIEW_EDITOR); }

    /**
     * Wyświetla panel generowania karty urlopowej (w tej implementacji jest to panel edytora).
     */
    @Override
    public void showVacation() { cardLayout.show(mainPanel, VIEW_VACATION); }
    /**
     * Zamyka aplikację.
     */
    @Override
    public void exitApp() { System.exit(0); }
}
