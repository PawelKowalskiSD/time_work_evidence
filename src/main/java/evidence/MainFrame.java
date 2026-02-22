package evidence;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame implements ViewNavigator {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    // Serwisy (Logika biznesowa)
    private final ExcelService excelService = new ApachePoiExcelService();

    // Stałe nazw widoków
    private static final String VIEW_MENU = "MENU";
    private static final String VIEW_GENERATOR = "GENERATOR";
    private static final String VIEW_EDITOR = "EDITOR";
    private static final String VIEW_VACATION = "VACATION";

    public MainFrame() {
        super("SYSTEM EWIDENCJI CZASU PRACY - PRO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. Ustawienie odpowiedniego rozmiaru (żeby okno nie było małe)
        setSize(950, 800);
        setLocationRelativeTo(null); // Wyśrodkowanie na ekranie

        // 2. Inicjalizacja Paneli
        // Przekazujemy 'this' jako nawigatora, żeby panele mogły zmieniać widoki
        MenuPanel menuPanel = new MenuPanel(this);
        GeneratorPanel generatorPanel = new GeneratorPanel(this, excelService);

        EditorPanel editorPanel = new EditorPanel(this);

        // 3. Dodanie paneli do CardLayout
        mainPanel.add(menuPanel, VIEW_MENU);
        mainPanel.add(generatorPanel, VIEW_GENERATOR);
        mainPanel.add(editorPanel, VIEW_EDITOR);

        // 4. Dodanie głównego panelu do okna
        add(mainPanel);

        // 5. KLUCZOWE: Pokazanie menu na start!
        cardLayout.show(mainPanel, VIEW_MENU);
    }

    // --- Implementacja nawigacji ---
    @Override public void showMenu() { cardLayout.show(mainPanel, VIEW_MENU); }
    @Override public void showGenerator() { cardLayout.show(mainPanel, VIEW_GENERATOR); }
    @Override public void showEditor() { cardLayout.show(mainPanel, VIEW_EDITOR); }
    @Override public void showVacation() { cardLayout.show(mainPanel, VIEW_VACATION); }
    @Override public void exitApp() { System.exit(0); }

    // Pomocniczy panel dla brakujących funkcji
    private JPanel createPlaceholderPanel(String text) {
        JPanel p = new JPanel(new BorderLayout());
        JButton btnBack = new JButton("<< Powrót");
        btnBack.addActionListener(e -> showMenu());
        p.add(btnBack, BorderLayout.NORTH);
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 20));
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }
}
