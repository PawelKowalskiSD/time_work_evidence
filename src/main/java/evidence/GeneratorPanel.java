package evidence;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

class GeneratorPanel extends JPanel {

    private final ViewNavigator navigator;
    private final ExcelService excelService;
    private static final String TEMPLATE_PATH = "Szablon_evidencja.xlsx";

    // Pola UI
    private JTextField fieldImie, fieldRok;
    private JCheckBox checkDisabled;
    private JRadioButton radio20, radio26;
    private JLabel labelTotalVacation;
    private JLabel labelPlik;
    private File selectedFile;
    private JButton btnGeneruj;
    private JTextField fieldZaleglyUrlop;
    private JTextField fieldNadgodzinyStart;

    // --- NOWE POLA DLA ŚCIEŻKI ZAPISU ---
    private JLabel labelOutputPath;
    private File outputDirectory = new File("."); // Domyślnie obecny folder

    // Sekcja Harmonogramów
    private JPanel schedulesContainer;
    private List<SchedulePeriodPanel> schedulePanels = new ArrayList<>();
    private JButton btnAddSchedule;

    // Sekcja nieobecności
    private JPanel absencesContainer;
    private List<AbsenceRowPanel> absenceRows = new ArrayList<>();

    private java.util.function.Supplier<Integer> yearSupplier = () -> {
        try { return Integer.parseInt(fieldRok.getText()); } catch (Exception e) { return LocalDate.now().getYear(); }
    };

    public GeneratorPanel(ViewNavigator navigator, ExcelService service) {
        this.navigator = navigator;
        this.excelService = service;

        setLayout(new BorderLayout());

        JButton btnBack = new JButton("<< Powrót do Menu");
        btnBack.addActionListener(e -> navigator.showMenu());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnBack);
        add(topPanel, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(createDataSection());
        content.add(Box.createVerticalStrut(10));

        content.add(createSchedulesManagerSection());
        content.add(Box.createVerticalStrut(10));

        content.add(createAbsenceSection());
        content.add(Box.createVerticalStrut(10));

        // --- DODANA SEKCJA WYBORU FOLDERU ---
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

    // --- NOWA METODA: SEKCJA WYBORU FOLDERU ---
    private JPanel createOutputSection() {
        JPanel p = createTitledPanel("5. Miejsce zapisu dokumentu");
        p.setLayout(new BorderLayout(10, 5));

        labelOutputPath = new JLabel("Folder: " + outputDirectory.getAbsolutePath());
        labelOutputPath.setForeground(Color.DARK_GRAY);
        p.add(labelOutputPath, BorderLayout.CENTER);

        JButton btnChooseDir = new JButton("Zmień folder...");
        btnChooseDir.addActionListener(e -> selectOutputDirectory());
        p.add(btnChooseDir, BorderLayout.EAST);

        return p;
    }

    // --- LOGIKA WYBORU FOLDERU ---
    private void selectOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(outputDirectory);
        chooser.setDialogTitle("Wybierz folder do zapisu (lub stwórz nowy)");
        // Tryb wybierania tylko katalogów
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Akceptuje "Wszystkie pliki" żeby widzieć foldery
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = chooser.getSelectedFile();
            labelOutputPath.setText("Folder: " + outputDirectory.getAbsolutePath());
        }
    }

    // --- DANE PRACOWNIKA ---
    private JPanel createDataSection() {
        JPanel p = createTitledPanel("1. Dane Pracownika i Urlopy");
        // Zwiększamy liczbę wierszy w GridLayout do 7, żeby wszystko się zmieściło
        p.setLayout(new GridLayout(7, 2, 5, 5));

        // 1. PRZYWRÓCONA INICJALIZACJA labelPlik (To naprawia błąd NullPointerException)
        p.add(new JLabel("Status szablonu:"));
        labelPlik = new JLabel("...");
        p.add(labelPlik);

        // 2. Imię i Nazwisko
        p.add(new JLabel("Imię i Nazwisko:"));
        fieldImie = new JTextField("Jan Kowalski");
        p.add(fieldImie);

        // 3. Rok
        p.add(new JLabel("Rok:"));
        fieldRok = new JTextField(String.valueOf(LocalDate.now().getYear()));
        p.add(fieldRok);

        // 4. Zaległy urlop (Nowe pole)
        p.add(new JLabel("Zaległy urlop (dni):"));
        fieldZaleglyUrlop = new JTextField("0");
        p.add(fieldZaleglyUrlop);

        // 5. Nadgodziny startowe (Nowe pole)
        p.add(new JLabel("Nadgodziny z zeszłego roku (godz):"));
        fieldNadgodzinyStart = new JTextField("0");
        p.add(fieldNadgodzinyStart);

        // 6. Wymiar urlopu (Radio buttons)
        JPanel pVacation = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radio20 = new JRadioButton("20 dni");
        radio26 = new JRadioButton("26 dni", true);
        ButtonGroup bg = new ButtonGroup(); bg.add(radio20); bg.add(radio26);
        pVacation.add(radio20); pVacation.add(radio26);

        checkDisabled = new JCheckBox("Niepełnosprawny (+10 dni)");
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
        pDis.add(checkDisabled); pDis.add(labelTotalVacation);
        pOptions.add(pDis);

        p.add(new JLabel("Wymiar urlopu:"));
        p.add(pOptions);

        return p;
    }

    // --- HARMONOGRAMY ---
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

        addSchedulePanel();
        return p;
    }

    private void addSchedulePanel() {
        LocalDate defaultStart;
        int year = yearSupplier.get();

        if (schedulePanels.isEmpty()) {
            defaultStart = LocalDate.of(year, 1, 1);
        } else {
            SchedulePeriodPanel last = schedulePanels.get(schedulePanels.size() - 1);
            LocalDate lastEnd = last.getEndDate(year);
            if (lastEnd.getMonthValue() == 12 && lastEnd.getDayOfMonth() == 31) {
                return;
            }
            defaultStart = lastEnd.plusDays(1);
        }

        SchedulePeriodPanel panel = new SchedulePeriodPanel(yearSupplier, defaultStart, this::removeSchedulePanel, this::updateAddButtonState);
        schedulePanels.add(panel);
        schedulesContainer.add(panel);
        updateAddButtonState();
        schedulesContainer.revalidate(); schedulesContainer.repaint();
    }

    private void removeSchedulePanel(SchedulePeriodPanel p) {
        if (schedulePanels.size() > 1) {
            schedulePanels.remove(p);
            schedulesContainer.remove(p);
            updateAddButtonState();
            schedulesContainer.revalidate(); schedulesContainer.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Musi być co najmniej jeden harmonogram!");
        }
    }

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

    // --- NIEOBECNOŚCI ---
    private JPanel createAbsenceSection() {
        JPanel p = createTitledPanel("4. Nieobecności");
        p.setLayout(new BorderLayout());

        absencesContainer = new JPanel();
        absencesContainer.setLayout(new BoxLayout(absencesContainer, BoxLayout.Y_AXIS));

        JButton btnAddAbsence = new JButton("+ Dodaj nieobecność");
        btnAddAbsence.addActionListener(e -> addAbsenceRow());

        p.add(absencesContainer, BorderLayout.CENTER);
        p.add(btnAddAbsence, BorderLayout.SOUTH);

        return p;
    }

    private void addAbsenceRow() {
        AbsenceRowPanel row = new AbsenceRowPanel(this::removeAbsenceRow, yearSupplier);
        absenceRows.add(row);
        absencesContainer.add(row);
        absencesContainer.revalidate();
        absencesContainer.repaint();
    }

    private void removeAbsenceRow(AbsenceRowPanel row) {
        absenceRows.remove(row);
        absencesContainer.remove(row);
        absencesContainer.revalidate();
        absencesContainer.repaint();
    }

    // --- KLASY WEWNĘTRZNE ---
    class SchedulePeriodPanel extends JPanel {
        JComboBox<String> sD, sM, eD, eM;
        JCheckBox checkEndOfYearLocal;
        JComboBox<String> comboEtat;
        Map<DayOfWeek, ScheduleRow> rows = new LinkedHashMap<>();

        // NOWA, PEŁNA LISTA WG TWOICH ZDJĘĆ
        String[] ETATY = {
                "1/1",
                "1/2",
                "1/3", "2/3",
                "1/4", "2/4", "3/4",
                "1/5", "2/5", "3/5", "4/5",
                "1/8", "3/8", "5/8", "7/8",
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
                    BorderFactory.createMatteBorder(0,0,1,0, Color.GRAY),
                    BorderFactory.createEmptyBorder(5,5,5,5)));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

            // Sztywne wymiary (zachowujemy naprawę skalowania)
            Dimension dateDim = new Dimension(55, 25);

            top.add(new JLabel("Obowiązuje OD:"));
            sD = new JComboBox<>(getIntArray(1,31)); sD.setPreferredSize(dateDim);
            sM = new JComboBox<>(getIntArray(1,12)); sM.setPreferredSize(dateDim);

            sD.setSelectedItem(String.valueOf(defaultStart.getDayOfMonth()));
            sM.setSelectedItem(String.valueOf(defaultStart.getMonthValue()));

            top.add(sD); top.add(new JLabel("/")); top.add(sM);

            top.add(new JLabel("  DO:"));
            checkEndOfYearLocal = new JCheckBox("końca roku", true);

            eD = new JComboBox<>(getIntArray(1,31)); eD.setPreferredSize(dateDim);
            eM = new JComboBox<>(getIntArray(1,12)); eM.setPreferredSize(dateDim);

            eD.setEnabled(false); eM.setEnabled(false); eD.setSelectedItem("31"); eM.setSelectedItem("12");

            java.awt.event.ActionListener dateChangeListener = e -> {
                if (this.stateChangedCallback != null) this.stateChangedCallback.run();
            };
            sD.addActionListener(dateChangeListener); sM.addActionListener(dateChangeListener);
            eD.addActionListener(dateChangeListener); eM.addActionListener(dateChangeListener);

            checkEndOfYearLocal.addActionListener(e -> {
                boolean active = !checkEndOfYearLocal.isSelected();
                eD.setEnabled(active); eM.setEnabled(active);
                if (this.stateChangedCallback != null) this.stateChangedCallback.run();
            });
            top.add(checkEndOfYearLocal);
            top.add(eD); top.add(new JLabel("/")); top.add(eM);

            top.add(Box.createHorizontalStrut(15));
            top.add(new JLabel("Wymiar Etatu:"));

            // Tuta podstawiamy nową listę ETATY
            comboEtat = new JComboBox<>(ETATY);
            comboEtat.setPreferredSize(new Dimension(70, 25)); // Nieco szersze dla 10/10
            top.add(comboEtat);

            JButton btnDel = new JButton("Usuń okres");
            btnDel.setForeground(Color.RED);
            btnDel.addActionListener(e -> removeAction.accept(this));
            top.add(btnDel);
            add(top);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
            for (DayOfWeek day : days) {
                String plName = day.getDisplayName(TextStyle.FULL, new Locale("pl", "PL"));
                plName = plName.substring(0, 1).toUpperCase() + plName.substring(1);
                boolean defActive = (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);
                ScheduleRow row = new ScheduleRow(plName, "08", "00", "16", "00", defActive);
                rows.put(day, row);
                add(row);
            }
        }

        public LocalDate getEndDate(int year) {
            if (checkEndOfYearLocal.isSelected()) return LocalDate.of(year, 12, 31);
            try {
                return LocalDate.of(year, Integer.parseInt((String)eM.getSelectedItem()), Integer.parseInt((String)eD.getSelectedItem()));
            } catch (Exception e) { return LocalDate.of(year, 12, 31); }
        }

        public SchedulePeriod toModel(int year) {
            try {
                LocalDate start = LocalDate.of(year, Integer.parseInt((String)sM.getSelectedItem()), Integer.parseInt((String)sD.getSelectedItem()));
                LocalDate end = getEndDate(year);
                Map<DayOfWeek, DailySchedule> weekly = new HashMap<>();
                rows.forEach((d, r) -> weekly.put(d, r.toModel()));
                return new SchedulePeriod(start, end, (String)comboEtat.getSelectedItem(), weekly);
            } catch (Exception e) { return null; }
        }
        private String[] getIntArray(int start, int end) { return IntStream.rangeClosed(start, end).mapToObj(String::valueOf).toArray(String[]::new); }
    }

    static class ScheduleRow extends JPanel {
        private JCheckBox checkActive;
        private JComboBox<String> hStart, mStart, hEnd, mEnd;
        private static final String[] HOURS = IntStream.range(0, 24).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);
        private static final String[] MINS = IntStream.iterate(0, n -> n + 5).limit(12).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);

        public ScheduleRow(String label, String hs, String ms, String he, String me, boolean active) {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            checkActive = new JCheckBox(label, active); checkActive.setPreferredSize(new Dimension(110, 20)); add(checkActive);
            hStart = new JComboBox<>(HOURS); hStart.setSelectedItem(hs);
            mStart = new JComboBox<>(MINS); mStart.setSelectedItem(ms);
            add(hStart); add(new JLabel(":")); add(mStart);
            add(new JLabel(" — "));
            hEnd = new JComboBox<>(HOURS); hEnd.setSelectedItem(he);
            mEnd = new JComboBox<>(MINS); mEnd.setSelectedItem(me);
            add(hEnd); add(new JLabel(":")); add(mEnd);
            checkActive.addActionListener(e -> toggle()); toggle();
        }
        private void toggle() { boolean on=checkActive.isSelected() && checkActive.isEnabled(); hStart.setEnabled(on); mStart.setEnabled(on); hEnd.setEnabled(on); mEnd.setEnabled(on); }
        public void disableRow() { checkActive.setEnabled(false); checkActive.setSelected(false); toggle(); }
        public void enableRow() { checkActive.setEnabled(true); toggle(); }
        public DailySchedule toModel() {
            return new DailySchedule(checkActive.isSelected(),
                    LocalTime.of(Integer.parseInt((String)hStart.getSelectedItem()), Integer.parseInt((String)mStart.getSelectedItem())),
                    LocalTime.of(Integer.parseInt((String)hEnd.getSelectedItem()), Integer.parseInt((String)mEnd.getSelectedItem())));
        }
    }

    class AbsenceRowPanel extends JPanel {
        JComboBox<String> typeCombo;
        JComboBox<String> sD, sM, eD, eM;
        JPanel panelEnd, panelOdbior;
        JComboBox<String> odbiorD;
        JButton btnRemove;
        java.util.function.Supplier<Integer> yearP;
        JCheckBox checkOvertimePickup;

        public AbsenceRowPanel(java.util.function.Consumer<AbsenceRowPanel> rem, java.util.function.Supplier<Integer> yp) {
            this.yearP = yp;
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
            add(new JLabel("Typ:"));
            typeCombo = new JComboBox<>(new String[]{"UW", "UM", "UO", "UB", "CH", "OP", "NU", "DEL", "NN"});
            add(typeCombo);
            checkOvertimePickup = new JCheckBox("Z nadgodzin");
            checkOvertimePickup.setToolTipText("Zaznacz, jeśli to odbiór nadgodzin (nie pomniejsza urlopu)");
            add(checkOvertimePickup);
            add(Box.createHorizontalStrut(10));
            add(new JLabel("Od:")); add(new JLabel("Dz:")); sD = new JComboBox<>(); add(sD);
            add(new JLabel("Msc:")); sM = new JComboBox<>(getIntArray(1,12)); sM.setSelectedItem(String.valueOf(LocalDate.now().getMonthValue())); add(sM);

            panelEnd = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panelEnd.add(new JLabel(" Do:")); panelEnd.add(new JLabel("Dz:")); eD = new JComboBox<>(); panelEnd.add(eD);
            panelEnd.add(new JLabel("Msc:")); eM = new JComboBox<>(getIntArray(1,12)); eM.setSelectedItem(String.valueOf(LocalDate.now().getMonthValue())); panelEnd.add(eM);
            add(panelEnd);

            updateDays(sM, sD); updateDays(eM, eD);
            sM.addActionListener(e -> updateDays(sM, sD)); eM.addActionListener(e -> updateDays(eM, eD));

            panelOdbior = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panelOdbior.add(new JLabel("  Odbiór za (święto w sob.):"));
            odbiorD = new JComboBox<>(); odbiorD.setPreferredSize(new Dimension(130, 22));
            panelOdbior.add(odbiorD);
            panelOdbior.setVisible(false);
            add(panelOdbior);

            typeCombo.addActionListener(e -> {
                boolean isUO = "UO".equals(typeCombo.getSelectedItem());
                panelEnd.setVisible(!isUO); panelOdbior.setVisible(isUO);
                if (isUO) loadHolidaySaturdays();
                revalidate(); repaint();
            });

            btnRemove = new JButton("X"); btnRemove.setForeground(Color.RED);
            btnRemove.addActionListener(e -> rem.accept(this));
            add(btnRemove);
            setPreferredSize(new Dimension(920, 40));
        }

        private void loadHolidaySaturdays() {
            int currentYear = yearP.get();
            int prevYear = currentYear - 1;
            List<LocalDate> satsCurrent = PolishHolidays.getHolidaysOnSaturday(currentYear);
            List<LocalDate> satsPrev = PolishHolidays.getHolidaysOnSaturday(prevYear);
            List<LocalDate> allSaturdays = new ArrayList<>(satsPrev);
            allSaturdays.addAll(satsCurrent);
            odbiorD.removeAllItems();
            if (allSaturdays.isEmpty()) { odbiorD.addItem("Brak świąt"); odbiorD.setEnabled(false); }
            else { odbiorD.setEnabled(true); for (LocalDate d : allSaturdays) odbiorD.addItem(d.getDayOfMonth() + "." + d.getMonthValue() + "." + d.getYear()); }
        }

        private void updateDays(JComboBox<String> mC, JComboBox<String> dC) {
            int year = yearP.get();
            int month = Integer.parseInt((String) mC.getSelectedItem());
            int max = 31; try { max = LocalDate.of(year, month, 1).lengthOfMonth(); } catch(Exception e){}
            dC.removeAllItems();
            for(int i=1; i<=max; i++) {
                try {
                    LocalDate d = LocalDate.of(year, month, i);
                    if (!PolishHolidays.isHoliday(d) && d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) { dC.addItem(String.valueOf(i)); }
                } catch (Exception e) {}
            }
        }
        private String[] getIntArray(int start, int end) { return IntStream.rangeClosed(start, end).mapToObj(String::valueOf).toArray(String[]::new); }

        public List<AbsenceData> toModelList(int year) {
            List<AbsenceData> list = new ArrayList<>();
            try {
                String type = (String) typeCombo.getSelectedItem();
                LocalDate start = LocalDate.of(year, Integer.parseInt((String)sM.getSelectedItem()), Integer.parseInt((String)sD.getSelectedItem()));
                LocalDate end = "UO".equals(type) ? start : LocalDate.of(year, Integer.parseInt((String)eM.getSelectedItem()), Integer.parseInt((String)eD.getSelectedItem()));
                String note = "";
                if ("UO".equals(type) && odbiorD.getSelectedItem() != null) note = "odbiór za " + odbiorD.getSelectedItem().toString();
                if (!end.isBefore(start)) {
                    LocalDate curr = start;
                    while (!curr.isAfter(end)) {
                        if (!PolishHolidays.isHoliday(curr) && curr.getDayOfWeek() != DayOfWeek.SATURDAY && curr.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            AbsenceData data = new AbsenceData(type, curr, note);
                            // USTAWIAMY FLAGĘ
                            if (checkOvertimePickup.isSelected()) data.setOvertimePickup(true);
                            list.add(data);
                        }
                        curr = curr.plusDays(1);
                    }
                }
            } catch (Exception e) { return null; }
            return list;
        }
    }

    private void performGeneration() {
        try {
            if (selectedFile == null || !selectedFile.exists()) return;
            int year = Integer.parseInt(fieldRok.getText());
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);

            int zalegly = 0; try { zalegly = Integer.parseInt(fieldZaleglyUrlop.getText()); } catch(Exception e){}
            int nadgodziny = 0; try { nadgodziny = Integer.parseInt(fieldNadgodzinyStart.getText()); } catch(Exception e){}

            // 1. Pobieramy harmonogramy z UI
            List<SchedulePeriod> schedules = new ArrayList<>();
            for (SchedulePeriodPanel p : schedulePanels) {
                SchedulePeriod sp = p.toModel(year);
                if (sp != null) schedules.add(sp);
            }

            // --- NOWOŚĆ: WALIDACJA CZASU PRACY ---
            TimeValidator.ValidationResult validation = TimeValidator.validate(schedules);

            if (validation.hasOvertime()) {
                String message = "WYKRYTO NIEZGODNOŚĆ Z ETATEM!\n\n" +
                        "Zaplanowany grafik przekracza wymiar etatu (37h 55min).\n" +
                        "System wygeneruje plik, ale godziny będą się różnić od umowy.\n\n" +
                        "RAPORT NADGODZIN:\n" +
                        validation.getReport() +
                        "\n\nCzy chcesz kontynuować mimo to?";

                int choice = JOptionPane.showConfirmDialog(this, message, "Ostrzeżenie o nadgodzinach", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == JOptionPane.NO_OPTION) {
                    return; // Przerwij generowanie, pozwól użytkownikowi poprawić
                }
            }
            // -------------------------------------

            List<AbsenceData> abs = new ArrayList<>();
            for (AbsenceRowPanel p : absenceRows) {
                List<AbsenceData> range = p.toModelList(year);
                if (range != null) abs.addAll(range);
            }

            int baseVacation = radio20.isSelected() ? 20 : 26;
            EvidenceRequest req = new EvidenceRequest(selectedFile, fieldImie.getText(), year, start, end, schedules, abs, checkDisabled.isSelected(), baseVacation, outputDirectory, zalegly, nadgodziny);

            new Thread(() -> {
                try {
                    excelService.generateEvidence(req);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Gotowe! Plik zapisano w: " + outputDirectory.getAbsolutePath()));
                } catch(Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd: " + e.getMessage()));
                }
            }).start();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Błąd danych: " + e.getMessage()); }
    }

    private void loadDefaultTemplate() {
        File f = new File(TEMPLATE_PATH);
        if (f.exists()) { selectedFile = f; labelPlik.setText(f.getName()); labelPlik.setForeground(new Color(0,100,0)); btnGeneruj.setEnabled(true); }
        else { labelPlik.setText("BRAK PLIKU SZABLONU!"); labelPlik.setForeground(Color.RED); btnGeneruj.setEnabled(false); }
    }
    private JPanel createTitledPanel(String t) { JPanel p=new JPanel(); p.setBorder(BorderFactory.createTitledBorder(t)); return p; }
}

