package evidence;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class App extends JFrame {

//    // --- KOMPONENTY UI ---
//    private JTextField fieldImie;
//    private JTextField fieldRok;
//    private JLabel labelPlik;
//    private File wybranySzablon;
//    private JButton btnGeneruj;
//
//    // Harmonogram
//    private JRadioButton radioTrybProsty;
//    private JRadioButton radioTrybZaawansowany;
//    private JPanel panelGodzinContainer;
//    private CardLayout cardLayout;
//    private WierszCzasu wierszWspolny; // Dla trybu prostego
//    private Map<DayOfWeek, WierszCzasu> wierszeDni = new LinkedHashMap<>(); // Dla trybu zaawansowanego
//
//    // Precyzyjny zakres dat
//    private JComboBox<String> comboStartDzien, comboStartMiesiac;
//    private JComboBox<String> comboKoniecDzien, comboKoniecMiesiac;
//    private JCheckBox checkDoKoncaRoku;
//
//    // Dane statyczne
//    public static final String[] GODZINY = IntStream.range(0, 24).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);
//    public static final String[] MINUTY = IntStream.iterate(0, n -> n + 5).limit(12).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);
//    public static final String[] DNI_MIESIACA = IntStream.rangeClosed(1, 31).mapToObj(String::valueOf).toArray(String[]::new);
//    public static final String[] MIESIACE = IntStream.rangeClosed(1, 12).mapToObj(String::valueOf).toArray(String[]::new);
//
//    public App() {
//        super("Generator Ewidencji PRECYZYJNY");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setSize(600, 750);
//        setLocationRelativeTo(null);
//        setLayout(new BorderLayout(10, 10));
//
//        JPanel mainPanel = new JPanel();
//        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
//        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        // --- 1. DANE PODSTAWOWE ---
//        JPanel panelDane = createPanel("1. Dane podstawowe");
//        panelDane.setLayout(new GridLayout(4, 2, 5, 5));
//
//        JButton btnWybierz = new JButton("Wybierz Szablon (.xlsx)");
//        labelPlik = new JLabel("Brak pliku...");
//        btnWybierz.addActionListener(e -> wybierzPlik());
//
//        panelDane.add(btnWybierz);
//        panelDane.add(labelPlik);
//        panelDane.add(new JLabel("Imię i Nazwisko:"));
//        fieldImie = new JTextField("Jan Kowalski");
//        panelDane.add(fieldImie);
//        panelDane.add(new JLabel("Rok ewidencji:"));
//        fieldRok = new JTextField(String.valueOf(LocalDate.now().getYear()));
//        panelDane.add(fieldRok);
//
//        mainPanel.add(panelDane);
//        mainPanel.add(Box.createVerticalStrut(10));
//
//        // --- 2. HARMONOGRAM PRACY ---
//        JPanel panelGodzinyWrapper = createPanel("2. Harmonogram i Dni Pracujące");
//        panelGodzinyWrapper.setLayout(new BorderLayout());
//
//        JPanel panelTryb = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        ButtonGroup grpTryb = new ButtonGroup();
//        radioTrybProsty = new JRadioButton("Tryb Prosty (Pn-Pt jednakowo)", true);
//        radioTrybZaawansowany = new JRadioButton("Tryb Zaawansowany (Wybór dni)");
//        grpTryb.add(radioTrybProsty);
//        grpTryb.add(radioTrybZaawansowany);
//        panelTryb.add(radioTrybProsty);
//        panelTryb.add(radioTrybZaawansowany);
//        panelGodzinyWrapper.add(panelTryb, BorderLayout.NORTH);
//
//        cardLayout = new CardLayout();
//        panelGodzinContainer = new JPanel(cardLayout);
//
//        // KARTA PROSTA
//        JPanel kartaProsta = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        wierszWspolny = new WierszCzasu("Wszystkie dni robocze:", "08", "00", "16", "00", true, false);
//        kartaProsta.add(wierszWspolny);
//
//        // KARTA ZAAWANSOWANA
//        JPanel kartaZaawansowana = new JPanel();
//        kartaZaawansowana.setLayout(new BoxLayout(kartaZaawansowana, BoxLayout.Y_AXIS));
//
//        DayOfWeek[] dniTygodnia = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
//        for (DayOfWeek dzien : dniTygodnia) {
//            String nazwa = dzien.getDisplayName(TextStyle.FULL, new Locale("pl", "PL"));
//            nazwa = nazwa.substring(0, 1).toUpperCase() + nazwa.substring(1);
//
//            // Domyślnie zaznaczamy Pn-Pt, weekendy odznaczone
//            boolean domyslnieAktywny = (dzien != DayOfWeek.SATURDAY && dzien != DayOfWeek.SUNDAY);
//
//            WierszCzasu wiersz = new WierszCzasu(nazwa, "08", "00", "16", "00", domyslnieAktywny, true);
//            wierszeDni.put(dzien, wiersz);
//            kartaZaawansowana.add(wiersz);
//        }
//
//        panelGodzinContainer.add(kartaProsta, "PROSTY");
//        panelGodzinContainer.add(new JScrollPane(kartaZaawansowana), "ZAAWANSOWANY"); // Scrollbar dla wygody
//        panelGodzinyWrapper.add(panelGodzinContainer, BorderLayout.CENTER);
//
//        radioTrybProsty.addActionListener(e -> cardLayout.show(panelGodzinContainer, "PROSTY"));
//        radioTrybZaawansowany.addActionListener(e -> cardLayout.show(panelGodzinContainer, "ZAAWANSOWANY"));
//
//        mainPanel.add(panelGodzinyWrapper);
//        mainPanel.add(Box.createVerticalStrut(10));
//
//        // --- 3. PRECYZYJNY ZAKRES DAT ---
//        JPanel panelZakres = createPanel("3. Okres zatrudnienia / generowania");
//        panelZakres.setLayout(new GridLayout(2, 1, 5, 5));
//
//        // Wiersz START
//        JPanel rowStart = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        rowStart.add(new JLabel("Rozpoczęcie pracy (Dzień / Miesiąc): "));
//        comboStartDzien = new JComboBox<>(DNI_MIESIACA);
//        comboStartMiesiac = new JComboBox<>(MIESIACE);
//        rowStart.add(comboStartDzien);
//        rowStart.add(new JLabel("/"));
//        rowStart.add(comboStartMiesiac);
//        panelZakres.add(rowStart);
//
//        // Wiersz KONIEC
//        JPanel rowKoniec = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        rowKoniec.add(new JLabel("Zakończenie pracy (Dzień / Miesiąc): "));
//
//        checkDoKoncaRoku = new JCheckBox("Do końca roku", true);
//        comboKoniecDzien = new JComboBox<>(DNI_MIESIACA);
//        comboKoniecMiesiac = new JComboBox<>(MIESIACE);
//
//        // Domyślny stan końcowy (wyłączony bo checkbox zaznaczony)
//        comboKoniecDzien.setEnabled(false);
//        comboKoniecMiesiac.setEnabled(false);
//        comboKoniecDzien.setSelectedItem("31");
//        comboKoniecMiesiac.setSelectedItem("12");
//
//        checkDoKoncaRoku.addActionListener(e -> {
//            boolean active = !checkDoKoncaRoku.isSelected();
//            comboKoniecDzien.setEnabled(active);
//            comboKoniecMiesiac.setEnabled(active);
//        });
//
//        rowKoniec.add(checkDoKoncaRoku);
//        rowKoniec.add(Box.createHorizontalStrut(15));
//        rowKoniec.add(comboKoniecDzien);
//        rowKoniec.add(new JLabel("/"));
//        rowKoniec.add(comboKoniecMiesiac);
//        panelZakres.add(rowKoniec);
//
//        mainPanel.add(panelZakres);
//        mainPanel.add(Box.createVerticalStrut(15));
//
//        // --- PRZYCISK ---
//        btnGeneruj = new JButton("GENERUJ EWIDENCJĘ");
//        btnGeneruj.setFont(new Font("Segoe UI", Font.BOLD, 18));
//        btnGeneruj.setBackground(new Color(46, 139, 87));
//        btnGeneruj.setForeground(Color.WHITE);
//        btnGeneruj.setFocusPainted(false);
//        btnGeneruj.setEnabled(false);
//        btnGeneruj.addActionListener(e -> uruchomProces());
//
//        mainPanel.add(btnGeneruj);
//
//        add(new JScrollPane(mainPanel), BorderLayout.CENTER); // Scroll na całość
//        setVisible(true);
//    }
//
//    // --- LOGIKA ---
//
//    private void wybierzPlik() {
//        JFileChooser chooser = new JFileChooser();
//        chooser.setFileFilter(new FileNameExtensionFilter("Pliki Excel", "xlsx"));
//        chooser.setCurrentDirectory(new File("."));
//        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//            wybranySzablon = chooser.getSelectedFile();
//            labelPlik.setText(wybranySzablon.getName());
//            btnGeneruj.setEnabled(true);
//        }
//    }
//
//    private void uruchomProces() {
//        String imie = fieldImie.getText();
//        int rok;
//        LocalDate dataStart, dataKoniec;
//
//        // 1. Walidacja Roku i Dat
//        try {
//            rok = Integer.parseInt(fieldRok.getText());
//
//            int sD = Integer.parseInt((String) comboStartDzien.getSelectedItem());
//            int sM = Integer.parseInt((String) comboStartMiesiac.getSelectedItem());
//            dataStart = LocalDate.of(rok, sM, sD);
//
//            if (checkDoKoncaRoku.isSelected()) {
//                dataKoniec = LocalDate.of(rok, 12, 31);
//            } else {
//                int eD = Integer.parseInt((String) comboKoniecDzien.getSelectedItem());
//                int eM = Integer.parseInt((String) comboKoniecMiesiac.getSelectedItem());
//                dataKoniec = LocalDate.of(rok, eM, eD);
//            }
//
//            if (dataKoniec.isBefore(dataStart)) {
//                JOptionPane.showMessageDialog(this, "Data zakończenia nie może być przed datą startu!", "Błąd daty", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, "Niepoprawna data (np. 30 luty) lub format roku!", "Błąd", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        // 2. Budowanie mapy harmonogramu
//        Map<DayOfWeek, Pair<LocalTime, LocalTime>> harmonogram = new HashMap<>();
//
//        if (radioTrybProsty.isSelected()) {
//            // W trybie prostym zakładamy Pn-Pt
//            LocalTime s = wierszWspolny.getStart();
//            LocalTime e = wierszWspolny.getEnd();
//            DayOfWeek[] robocze = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
//            for (DayOfWeek d : robocze) harmonogram.put(d, new Pair<>(s, e));
//        } else {
//            // W trybie zaawansowanym sprawdzamy checkboxy
//            for (Map.Entry<DayOfWeek, WierszCzasu> entry : wierszeDni.entrySet()) {
//                WierszCzasu w = entry.getValue();
//                if (w.isAktywny()) {
//                    harmonogram.put(entry.getKey(), new Pair<>(w.getStart(), w.getEnd()));
//                }
//            }
//        }
//
//        // 3. Uruchomienie generowania
//        final int finalRok = rok;
//        final LocalDate fStart = dataStart;
//        final LocalDate fKoniec = dataKoniec;
//
//        new Thread(() -> generujExcel(wybranySzablon, imie, finalRok, fStart, fKoniec, harmonogram)).start();
//    }
//
//    private void generujExcel(File plik, String imie, int rok, LocalDate startData, LocalDate koniecData,
//                              Map<DayOfWeek, Pair<LocalTime, LocalTime>> harmonogram) {
//        try (FileInputStream fis = new FileInputStream(plik);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//
//            CellStyle stylCzasu = workbook.createCellStyle();
//            stylCzasu.setDataFormat(workbook.createDataFormat().getFormat("HH:mm"));
//
//            // Iterujemy przez 12 miesięcy (zakładki)
//            for (int i = 0; i < 12; i++) {
//                if (i >= workbook.getNumberOfSheets()) break;
//                Sheet sheet = workbook.getSheetAt(i);
//                int miesiac = i + 1;
//                LocalDate firstDayOfMonth = LocalDate.of(rok, miesiac, 1);
//
//                // --- Nagłówek ---
//                Row hRow = sheet.getRow(5);
//                if (hRow != null) {
//                    Cell c = hRow.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//                    String mName = firstDayOfMonth.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("pl", "PL"));
//                    c.setCellValue(imie + " - " + mName + " " + rok);
//                }
//
//                // --- Dni ---
//                int dniMax = firstDayOfMonth.lengthOfMonth();
//                for (int d = 1; d <= 31; d++) {
//                    Row row = sheet.getRow(7 + d); // Index 8 w excelu (wiersz 9)
//                    if (row == null) row = sheet.createRow(7 + d);
//
//                    // Czyszczenie dni spoza kalendarza (np. 30 luty)
//                    if (d > dniMax) {
//                        wyczyscWiersz(row);
//                        continue;
//                    }
//
//                    LocalDate currentDate = LocalDate.of(rok, miesiac, d);
//                    DayOfWeek dzienTyg = currentDate.getDayOfWeek();
//
//                    // Wpisujemy datę i dzień tygodnia (ZAWSZE, żeby kalendarz był kompletny)
//                    row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(d + ".");
//                    row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
//                            .setCellValue(dzienTyg.getDisplayName(TextStyle.SHORT, new Locale("pl", "PL")));
//
//                    // Pobieramy komórki godzinowe
//                    Cell cTyp = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//                    Cell cOd = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//                    Cell cDo = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//                    Cell cGodz = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//
//                    // Resetujemy wartości
//                    cTyp.setBlank(); cOd.setBlank(); cDo.setBlank(); cGodz.setBlank();
//
//                    // 1. Sprawdzamy czy data mieści się w zadanym zakresie zatrudnienia
//                    boolean wZakresie = !currentDate.isBefore(startData) && !currentDate.isAfter(koniecData);
//
//                    // 2. Logika typów dni
//                    if (dzienTyg == DayOfWeek.SUNDAY) {
//                        cTyp.setCellValue("WN");
//                    }
//                    else if (dzienTyg == DayOfWeek.SATURDAY) {
//                        cTyp.setCellValue("W5"); // lub WN zależnie od firmy
//                    }
//                    else {
//                        // Dzień powszedni (Pn-Pt)
//                        // Sprawdzamy czy w ogóle pracujemy w ten dzień (checkboxy)
//                        if (harmonogram.containsKey(dzienTyg)) {
//                            // Jeśli data jest w zakresie zatrudnienia -> wpisujemy godziny
//                            if (wZakresie) {
//                                cTyp.setCellValue("R");
//                                Pair<LocalTime, LocalTime> godziny = harmonogram.get(dzienTyg);
//                                cOd.setCellValue(godziny.key.toString());
//                                cDo.setCellValue(godziny.value.toString());
//
//                                long minuty = ChronoUnit.MINUTES.between(godziny.key, godziny.value);
//                                cGodz.setCellValue(minuty / (24.0 * 60.0));
//                                cGodz.setCellStyle(stylCzasu);
//                            } else {
//                                // Dzień roboczy poza okresem zatrudnienia (pusty, bez "R")
//                                // Można tu wpisać np. "-" albo zostawić puste
//                            }
//                        } else {
//                            // Dzień odznaczony w harmonogramie (np. Piątek wolny)
//                            // Traktujemy jak dzień wolny (WN/W5 lub puste)
//                            cTyp.setCellValue("WN"); // Wolne
//                        }
//                    }
//                }
//                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
//            }
//
//            String plikWynik = "Ewidencja_" + imie.replace(" ", "_") + "_" + rok + ".xlsx";
//            try (FileOutputStream fos = new FileOutputStream(plikWynik)) {
//                workbook.write(fos);
//            }
//            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Gotowe! Wygenerowano: " + plikWynik));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Błąd: " + e.getMessage()));
//        }
//    }
//
//    private void wyczyscWiersz(Row row) {
//        for(int k=0; k<=5; k++) { Cell c=row.getCell(k); if(c!=null) c.setBlank(); }
//    }
//
//    private JPanel createPanel(String title) {
//        JPanel p = new JPanel();
//        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
//        return p;
//    }
//
//    // --- HELPERY UI ---
//    static class WierszCzasu extends JPanel {
//        private JCheckBox checkAktywny;
//        private JComboBox<String> startH, startM, endH, endM;
//
//        public WierszCzasu(String etykieta, String sh, String sm, String eh, String em, boolean domyslnieOn, boolean pokazacCheckbox) {
//            setLayout(new FlowLayout(FlowLayout.LEFT));
//
//            checkAktywny = new JCheckBox(etykieta, domyslnieOn);
//            checkAktywny.setPreferredSize(new Dimension(140, 20));
//            add(checkAktywny);
//
//            startH = new JComboBox<>(GODZINY); startH.setSelectedItem(sh);
//            startM = new JComboBox<>(MINUTY);  startM.setSelectedItem(sm);
//            add(startH); add(new JLabel(":")); add(startM);
//
//            add(new JLabel(" — "));
//
//            endH = new JComboBox<>(GODZINY); endH.setSelectedItem(eh);
//            endM = new JComboBox<>(MINUTY);  endM.setSelectedItem(em);
//            add(endH); add(new JLabel(":")); add(endM);
//
//            if (!pokazacCheckbox) {
//                // W trybie prostym checkbox jest ukryty, ale etykieta widoczna
//                checkAktywny.setEnabled(false);
//                // Ewentualnie można użyć JLabel zamiast CheckBoxa, ale tak jest spójnie
//            }
//
//            // Logika wyszarzania
//            checkAktywny.addActionListener(e -> przelaczStan(checkAktywny.isSelected()));
//            przelaczStan(domyslnieOn);
//        }
//
//        private void przelaczStan(boolean on) {
//            startH.setEnabled(on); startM.setEnabled(on);
//            endH.setEnabled(on);   endM.setEnabled(on);
//        }
//
//        public boolean isAktywny() { return checkAktywny.isSelected(); }
//
//        public LocalTime getStart() {
//            return LocalTime.of(Integer.parseInt((String) startH.getSelectedItem()),
//                    Integer.parseInt((String) startM.getSelectedItem()));
//        }
//        public LocalTime getEnd() {
//            return LocalTime.of(Integer.parseInt((String) endH.getSelectedItem()),
//                    Integer.parseInt((String) endM.getSelectedItem()));
//        }
//    }
//
//    static class Pair<K, V> {
//        K key; V value;
//        public Pair(K key, V value) { this.key = key; this.value = value; }
//    }
//
//    public static void main(String[] args) {
//        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
//        SwingUtilities.invokeLater(App::new);
//    }
public static void main(String[] args) {
    // Ustawienie LookAndFeel systemu operacyjnego dla lepszego wyglądu
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
    catch (Exception ignored) {}

    // Uruchomienie w wątku Swing (EDT)
    SwingUtilities.invokeLater(() -> {
        new MainFrame().setVisible(true);
    });
}
}
