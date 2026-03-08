package evidence.view;

import evidence.controller.EvidenceController;
import evidence.model.AbsenceData;
import evidence.model.DailySchedule;
import evidence.model.EvidenceRequest;
import evidence.model.SchedulePeriod;
import evidence.util.PolishHolidays;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Panel do generowania nowego dokumentu ewidencji czasu pracy.
 * Jest to główny formularz aplikacji, w którym użytkownik wprowadza wszystkie
 * niezbędne dane: dane pracownika, rok, wymiar urlopu, harmonogramy pracy,
 * etaty oraz nieobecności.
 */
class GeneratorPanel extends JPanel {

    private final ViewNavigator navigator;
    private final EvidenceController excelController;
    private static final String TEMPLATE_PATH = "Szablon_evidencja.xlsx";

    // Główne pola formularza
    private JTextField fieldImie, fieldRok;
    private JCheckBox checkDisabled;
    private JRadioButton radio20, radio26;
    private JLabel labelTotalVacation;
    private JLabel labelPlik;
    private File selectedFile;
    private JButton btnGeneruj;
    private JTextField fieldZaleglyUrlop;
    private JTextField fieldNadgodzinyStart;

    private JLabel labelOutputPath;
    private File outputDirectory = new File(".");

    // Kontenery na dynamiczne komponenty
    private JPanel schedulesContainer;
    private List<SchedulePeriodPanel> schedulePanels = new ArrayList<>();
    private JButton btnAddSchedule;

    private JPanel absencesContainer;
    private List<AbsenceRowPanel> absenceRows = new ArrayList<>();

    // Dostawca roku, aby dynamiczne komponenty miały dostęp do aktualnej wartości
    private java.util.function.Supplier<Integer> yearSupplier = () -> {
        try {
            return Integer.parseInt(fieldRok.getText());
        } catch (Exception e) {
            return LocalDate.now().getYear();
        }
    };

    /**
     * Tworzy panel generatora.
     * @param navigator Obiekt nawigatora do obsługi powrotu do menu.
     * @param service   Kontroler odpowiedzialny za logikę generowania pliku Excel.
     */
    public GeneratorPanel(ViewNavigator navigator, EvidenceController service) {
        this.navigator = navigator;
        this.excelController = service;

        setLayout(new BorderLayout());

        JButton btnBack = new JButton("<< Powrot do Menu");
        btnBack.addActionListener(e -> navigator.showMenu());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnBack);
        add(topPanel, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Budowanie poszczególnych sekcji formularza
        content.add(createDataSection());
        content.add(Box.createVerticalStrut(10));
        content.add(createSchedulesManagerSection());
        content.add(Box.createVerticalStrut(10));
        content.add(createAbsenceSection());
        content.add(Box.createVerticalStrut(10));
        content.add(createOutputSection());
        content.add(Box.createVerticalStrut(20));

        btnGeneruj = new JButton("GENERUJ DOKUMENT");
        btnGeneruj.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnGeneruj.setBackground(new Color(46, 139, 87));
        btnGeneruj.setForeground(Color.BLACK);
        btnGeneruj.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGeneruj.setEnabled(false);
        btnGeneruj.addActionListener(e -> performGeneration());
        content.add(btnGeneruj);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);

        loadDefaultTemplate();
    }

    /**
     * Tworzy sekcję wyboru folderu docelowego.
     */
    private JPanel createOutputSection() {
        JPanel p = createTitledPanel("5. Miejsce zapisu dokumentu");
        p.setLayout(new BorderLayout(10, 5));

        labelOutputPath = new JLabel("Folder: " + outputDirectory.getAbsolutePath());
        labelOutputPath.setForeground(Color.DARK_GRAY);
        p.add(labelOutputPath, BorderLayout.CENTER);

        JButton btnChooseDir = new JButton("Zmien folder...");
        btnChooseDir.addActionListener(e -> selectOutputDirectory());
        p.add(btnChooseDir, BorderLayout.EAST);

        return p;
    }

    /**
     * Otwiera okno dialogowe do wyboru folderu zapisu.
     */
    private void selectOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(outputDirectory);
        chooser.setDialogTitle("Wybierz folder do zapisu");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = chooser.getSelectedFile();
            labelOutputPath.setText("Folder: " + outputDirectory.getAbsolutePath());
        }
    }

    /**
     * Tworzy sekcję z podstawowymi danymi pracownika i ustawieniami urlopu.
     */
    private JPanel createDataSection() {
        JPanel p = createTitledPanel("1. Dane Pracownika i Urlopy");
        p.setLayout(new GridLayout(7, 2, 5, 5));

        p.add(new JLabel("Status szablonu:"));
        labelPlik = new JLabel("...");
        p.add(labelPlik);

        p.add(new JLabel("Imie i Nazwisko:"));
        fieldImie = new JTextField("Jan Kowalski");
        p.add(fieldImie);

        p.add(new JLabel("Rok:"));
        fieldRok = new JTextField(String.valueOf(LocalDate.now().getYear()));
        p.add(fieldRok);

        p.add(new JLabel("Zalegly urlop (dni):"));
        fieldZaleglyUrlop = new JTextField("0");
        p.add(fieldZaleglyUrlop);

        p.add(new JLabel("Nadgodziny z zeszlego roku (godz):"));
        fieldNadgodzinyStart = new JTextField("0");
        p.add(fieldNadgodzinyStart);

        // Opcje wymiaru urlopu
        JPanel pVacation = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radio20 = new JRadioButton("20 dni");
        radio26 = new JRadioButton("26 dni", true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(radio20);
        bg.add(radio26);
        pVacation.add(radio20);
        pVacation.add(radio26);

        checkDisabled = new JCheckBox("Niepelnosprawny (+10 dni)");
        labelTotalVacation = new JLabel(" (Razem: 26 dni)");
        labelTotalVacation.setForeground(Color.BLUE);

        Runnable updateVacLabel = () -> {
            int base = radio20.isSelected() ? 20 : 26;
            if (checkDisabled.isSelected()) base += 10;
            labelTotalVacation.setText(" (Razem: " + base + " dni)");
        };
        radio20.addActionListener(e -> updateVacLabel.run());
        radio26.addActionListener(e -> updateVacLabel.run());
        checkDisabled.addActionListener(e -> updateVacLabel.run());

        JPanel pOptions = new JPanel(new GridLayout(2, 1));
        pOptions.add(pVacation);
        JPanel pDis = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pDis.add(checkDisabled);
        pDis.add(labelTotalVacation);
        pOptions.add(pDis);

        p.add(new JLabel("Wymiar urlopu:"));
        p.add(pOptions);

        return p;
    }

    /**
     * Tworzy sekcję zarządzania harmonogramami i wymiarami etatu.
     */
    private JPanel createSchedulesManagerSection() {
        JPanel p = createTitledPanel("2. Harmonogramy i Etaty");
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        schedulesContainer = new JPanel();
        schedulesContainer.setLayout(new BoxLayout(schedulesContainer, BoxLayout.Y_AXIS));
        p.add(schedulesContainer);

        btnAddSchedule = new JButton("+ Dodaj kolejny okres harmonogramu");
        btnAddSchedule.setEnabled(false);
        btnAddSchedule.addActionListener(e -> addSchedulePanel());
        p.add(btnAddSchedule);

        addSchedulePanel(); // Dodaj pierwszy, domyślny panel harmonogramu
        return p;
    }

    /**
     * Dodaje nowy panel do definiowania okresu harmonogramu.
     */
    private void addSchedulePanel() {
        LocalDate defaultStart;
        int year = yearSupplier.get();

        if (schedulePanels.isEmpty()) {
            defaultStart = LocalDate.of(year, 1, 1);
        } else {
            SchedulePeriodPanel last = schedulePanels.get(schedulePanels.size() - 1);
            LocalDate lastEnd = last.getEndDate(year);
            if (lastEnd.getMonthValue() == 12 && lastEnd.getDayOfMonth() == 31) {
                return; // Nie można dodać więcej, jeśli ostatni okres kończy się z końcem roku
            }
            defaultStart = lastEnd.plusDays(1);
        }

        SchedulePeriodPanel panel = new SchedulePeriodPanel(yearSupplier, defaultStart, this::removeSchedulePanel, this::updateAddButtonState);
        schedulePanels.add(panel);
        schedulesContainer.add(panel);
        updateAddButtonState();
        schedulesContainer.revalidate();
        schedulesContainer.repaint();
    }

    /**
     * Usuwa wskazany panel harmonogramu.
     * @param p Panel do usunięcia.
     */
    private void removeSchedulePanel(SchedulePeriodPanel p) {
        if (schedulePanels.size() > 1) {
            schedulePanels.remove(p);
            schedulesContainer.remove(p);
            updateAddButtonState();
            schedulesContainer.revalidate();
            schedulesContainer.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Musi byc co najmniej jeden harmonogram!");
        }
    }

    /**
     * Aktualizuje stan przycisku "Dodaj kolejny okres harmonogramu" w zależności od tego,
     * czy ostatni zdefiniowany okres kończy się wraz z końcem roku.
     */
    private void updateAddButtonState() {
        if (schedulePanels.isEmpty()) {
            btnAddSchedule.setEnabled(true);
            return;
        }
        SchedulePeriodPanel last = schedulePanels.get(schedulePanels.size() - 1);
        int year = yearSupplier.get();
        LocalDate end = last.getEndDate(year);
        boolean isEndOfYear = (end.getMonthValue() == 12 && end.getDayOfMonth() == 31);
        btnAddSchedule.setEnabled(!isEndOfYear);
    }

    /**
     * Tworzy sekcję do wprowadzania nieobecności.
     */
    private JPanel createAbsenceSection() {
        JPanel p = createTitledPanel("4. Nieobecnosci");
        p.setLayout(new BorderLayout());

        absencesContainer = new JPanel();
        absencesContainer.setLayout(new BoxLayout(absencesContainer, BoxLayout.Y_AXIS));

        JButton btnAddAbsence = new JButton("+ Dodaj nieobecnosc");
        btnAddAbsence.addActionListener(e -> addAbsenceRow());

        p.add(absencesContainer, BorderLayout.CENTER);
        p.add(btnAddAbsence, BorderLayout.SOUTH);

        return p;
    }

    /**
     * Dodaje nowy wiersz do wprowadzania nieobecności.
     */
    private void addAbsenceRow() {
        AbsenceRowPanel row = new AbsenceRowPanel(this::removeAbsenceRow, yearSupplier);
        absenceRows.add(row);
        absencesContainer.add(row);
        absencesContainer.revalidate();
        absencesContainer.repaint();
    }

    /**
     * Usuwa wskazany wiersz nieobecności.
     * @param row Wiersz do usunięcia.
     */
    private void removeAbsenceRow(AbsenceRowPanel row) {
        absenceRows.remove(row);
        absencesContainer.remove(row);
        absencesContainer.revalidate();
        absencesContainer.repaint();
    }

    /**
     * Wewnętrzna klasa reprezentująca panel do definiowania jednego okresu harmonogramu.
     * Zawiera pola do ustawienia daty początku i końca, wymiaru etatu oraz godzin pracy
     * w poszczególnych dniach tygodnia.
     */
    class SchedulePeriodPanel extends JPanel {
        JComboBox<String> sD, sM, eD, eM;
        JCheckBox checkEndOfYearLocal;
        JComboBox<String> comboEtat;
        Map<DayOfWeek, ScheduleRow> rows = new LinkedHashMap<>();

        String[] ETATY = {
                "1/1", "1/2", "1/3", "2/3", "1/4", "2/4", "3/4",
                "1/5", "2/5", "3/5", "4/5", "1/8", "3/8", "5/8", "7/8",
                "1/10", "2/10", "3/10", "4/10", "5/10", "6/10", "7/10", "8/10", "9/10"
        };

        Runnable stateChangedCallback;
        java.util.function.Supplier<Integer> yearProvider;

        public SchedulePeriodPanel(java.util.function.Supplier<Integer> yearProv,
                                   LocalDate defaultStart,
                                   java.util.function.Consumer<SchedulePeriodPanel> removeAction,
                                   Runnable stateChangedCallback) {
            this.yearProvider = yearProv;
            this.stateChangedCallback = stateChangedCallback;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            // ... reszta implementacji GUI ...
        }

        public LocalDate getEndDate(int year) {
            // ...
        }

        public SchedulePeriod toModel(int year) {
            // ...
        }
    }

    /**
     * Wewnętrzna klasa reprezentująca wiersz do ustawiania godzin pracy dla jednego dnia tygodnia.
     */
    static class ScheduleRow extends JPanel {
        // ... implementacja ...
    }

    /**
     * Wewnętrzna klasa reprezentująca wiersz do definiowania nieobecności.
     */
    class AbsenceRowPanel extends JPanel {
        // ... implementacja ...
    }

    /**
     * Zbiera wszystkie dane z formularza, tworzy obiekt {@link EvidenceRequest}
     * i uruchamia proces generowania pliku w osobnym wątku.
     */
    private void performGeneration() {
        try {
            if (selectedFile == null || !selectedFile.exists()) return;
            int year = Integer.parseInt(fieldRok.getText());
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);

            int zalegly = Integer.parseInt(fieldZaleglyUrlop.getText());
            int nadgodziny = Integer.parseInt(fieldNadgodzinyStart.getText());

            List<SchedulePeriod> schedules = new ArrayList<>();
            for (SchedulePeriodPanel p : schedulePanels) {
                SchedulePeriod sp = p.toModel(year);
                if (sp != null) schedules.add(sp);
            }

            List<AbsenceData> abs = new ArrayList<>();
            for (AbsenceRowPanel p : absenceRows) {
                List<AbsenceData> range = p.toModelList(year);
                if (range != null) abs.addAll(range);
            }

            int baseVacation = radio20.isSelected() ? 20 : 26;
            EvidenceRequest req = new EvidenceRequest(selectedFile, fieldImie.getText(), year, start, end, schedules, abs, checkDisabled.isSelected(), baseVacation, outputDirectory, zalegly, nadgodziny);

            new Thread(() -> {
                try {
                    excelController.generateEvidence(req);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Gotowe! Plik zapisano w: " + outputDirectory.getAbsolutePath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Blad: " + e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Blad danych: " + e.getMessage());
        }
    }

    /**
     * Ładuje domyślny plik szablonu i aktualizuje stan UI.
     */
    private void loadDefaultTemplate() {
        File f = new File(TEMPLATE_PATH);
        if (f.exists()) {
            selectedFile = f;
            labelPlik.setText(f.getName());
            labelPlik.setForeground(new Color(0, 100, 0));
            btnGeneruj.setEnabled(true);
        } else {
            labelPlik.setText("BRAK PLIKU SZABLONU!");
            labelPlik.setForeground(Color.RED);
            btnGeneruj.setEnabled(false);
        }
    }

    /**
     * Pomocnicza metoda do tworzenia panelu z tytułową ramką.
     */
    private JPanel createTitledPanel(String t) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(t));
        return p;
    }
}
