package evidence.view;

import evidence.controller.EditorService;
import evidence.controller.VacationReportGenerator;
import evidence.model.VacationEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EditorPanel extends JPanel {

    private final ViewNavigator navigator;
    private final EditorService editorService = new EditorService();
    private final VacationReportGenerator reportGenerator = new VacationReportGenerator();

    private DefaultListModel<String> listModel;
    private List<File> selectedFiles = new ArrayList<>();
    private JProgressBar progressBar;
    private JLabel lblStatus;

    private JTextField fieldStartOverdueDays;
    private JTextField fieldBaseYearlyDays;
    private DefaultTableModel eventsTableModel;
    private JTable eventsTable;

    public EditorPanel(ViewNavigator navigator) {
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBack = new JButton("<< Wroc");
        btnBack.addActionListener(e -> navigator.showMenu());
        top.add(btnBack);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("1. Pliki Excel (Lata)"));
        listModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(listModel);
        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        JButton btnSelect = new JButton("Dodaj pliki");
        btnSelect.addActionListener(e -> selectFiles());
        leftPanel.add(btnSelect, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("2. Zdarzenia (Zmiana wymiaru)"));

        JPanel startConfig = new JPanel(new GridLayout(2, 2, 5, 5));
        startConfig.add(new JLabel("Zalegly na start (dni):"));
        fieldStartOverdueDays = new JTextField("0");
        startConfig.add(fieldStartOverdueDays);

        startConfig.add(new JLabel("Wymiar startowy (1 sty):"));
        fieldBaseYearlyDays = new JTextField("26");
        startConfig.add(fieldBaseYearlyDays);

        rightPanel.add(startConfig, BorderLayout.NORTH);

        String[] columns = {"Data (RRRR-MM-DD)", "Nowy Wymiar", "Opis"};
        eventsTableModel = new DefaultTableModel(columns, 0);
        eventsTable = new JTable(eventsTableModel);
        rightPanel.add(new JScrollPane(eventsTable), BorderLayout.CENTER);

        JPanel eventsButtons = new JPanel(new FlowLayout());
        JButton btnAddEvent = new JButton("Dodaj Zmiane");
        btnAddEvent.addActionListener(e -> addEventDialog());
        JButton btnDelEvent = new JButton("Usun");
        btnDelEvent.addActionListener(e -> {
            int row = eventsTable.getSelectedRow();
            if (row != -1) eventsTableModel.removeRow(row);
        });
        eventsButtons.add(btnAddEvent);
        eventsButtons.add(btnDelEvent);
        rightPanel.add(eventsButtons, BorderLayout.SOUTH);

        center.add(leftPanel);
        center.add(rightPanel);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton btnUpdate = new JButton("KROK 1: PRZELICZ PLIKI EXCEL");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setBackground(new Color(70, 130, 180));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setPreferredSize(new Dimension(220, 40));
        btnUpdate.addActionListener(e -> runUpdate());

        JButton btnReport = new JButton("KROK 2: GENERUJ KARTA ROCZNA");
        btnReport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReport.setBackground(new Color(0, 100, 0));
        btnReport.setForeground(Color.WHITE);
        btnReport.setPreferredSize(new Dimension(220, 40));
        btnReport.addActionListener(e -> runReportGeneration());

        actionButtons.add(btnUpdate);
        actionButtons.add(btnReport);

        progressBar = new JProgressBar();
        lblStatus = new JLabel("Gotowy");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        bottom.add(actionButtons, BorderLayout.NORTH);
        bottom.add(progressBar, BorderLayout.CENTER);
        bottom.add(lblStatus, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addEventDialog() {
        JTextField dField = new JTextField("2025-03-24");
        JTextField wField = new JTextField("36");
        JTextField oField = new JTextField("Orzeczenie");

        Object[] msg = {"Data:", dField, "Nowy Wymiar:", wField, "Opis:", oField};
        if (JOptionPane.showConfirmDialog(this, msg, "Dodaj", JOptionPane.OK_CANCEL_OPTION) == 0) {
            eventsTableModel.addRow(new Object[]{dField.getText(), wField.getText(), oField.getText()});
        }
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                if (!selectedFiles.contains(f)) {
                    selectedFiles.add(f);
                    listModel.addElement(f.getName());
                }
            }
        }
    }

    private List<VacationEvent> getEventsFromTable() {
        List<VacationEvent> events = new ArrayList<>();
        for (int i = 0; i < eventsTableModel.getRowCount(); i++) {
            try {
                String d = (String) eventsTableModel.getValueAt(i, 0);
                int dim = Integer.parseInt((String) eventsTableModel.getValueAt(i, 1));
                String desc = (String) eventsTableModel.getValueAt(i, 2);
                events.add(new VacationEvent(LocalDate.parse(d), dim, desc));
            } catch (Exception e) {
            }
        }
        return events;
    }

    private void runUpdate() {
        if (selectedFiles.isEmpty()) return;
        new Thread(() -> {
            try {
                int overdue = Integer.parseInt(fieldStartOverdueDays.getText());
                int base = Integer.parseInt(fieldBaseYearlyDays.getText());
                List<VacationEvent> events = getEventsFromTable();

                editorService.processFilesChain(selectedFiles, overdue, base, events, (s, c, t) -> {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText(s);
                        progressBar.setMaximum(t);
                        progressBar.setValue(c);
                    });
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Blad danych!"));
            }
        }).start();
    }

    private void runReportGeneration() {
        if (selectedFiles.isEmpty()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outDir = chooser.getSelectedFile();
            new Thread(() -> {
                try {
                    int base = Integer.parseInt(fieldBaseYearlyDays.getText());
                    List<VacationEvent> events = getEventsFromTable();

                    reportGenerator.generateReport(selectedFiles, outDir, base, events, (s, c, t) -> {
                        SwingUtilities.invokeLater(() -> {
                            lblStatus.setText(s);
                            progressBar.setMaximum(t);
                            progressBar.setValue(c);
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
