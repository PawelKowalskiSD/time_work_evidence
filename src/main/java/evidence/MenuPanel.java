package evidence;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Widok Menu Głównego.
 */
class MenuPanel extends JPanel {
    public MenuPanel(ViewNavigator navigator) {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel buttonContainer = new JPanel();
        // GridLayout 0,1 oznacza: dowolna ilość wierszy, 1 kolumna (pionowo)
        buttonContainer.setLayout(new GridLayout(0, 1, 10, 15));
        buttonContainer.setPreferredSize(new Dimension(350, 300));

        buttonContainer.add(createStyledButton("NOWY DOKUMENT", navigator::showGenerator, new Color(46, 139, 87)));
        buttonContainer.add(createStyledButton("EDYTUJ ISTNIEJĄCY", navigator::showEditor, new Color(70, 130, 180)));
        buttonContainer.add(createStyledButton("KARTA URLOPOWA", navigator::showVacation, new Color(218, 165, 32))); // Złoty
        buttonContainer.add(createStyledButton("WYJŚCIE", navigator::exitApp, new Color(205, 92, 92)));

        JLabel titleLabel = new JLabel("SYSTEM EWIDENCJI CZASU PRACY", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 30, 0);
        add(titleLabel, gbc);

        gbc.gridy = 1;
        add(buttonContainer, gbc);
    }

    private JButton createStyledButton(String text, Runnable action, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(0, 50)); // Wysokość przycisku
        btn.addActionListener(e -> action.run());
        return btn;
    }
}