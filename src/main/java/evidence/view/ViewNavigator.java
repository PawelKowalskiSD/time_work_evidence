package evidence.view;

/**
 * Interfejs nawigacji widoków.
 * Definiuje zestaw metod do przełączania się między różnymi panelami (widokami) w aplikacji.
 * Implementowany przez {@link MainFrame}, aby umożliwić panelom potomnym zmianę bieżącego widoku.
 */
public interface ViewNavigator {
    /** Wyświetla panel menu głównego. */
    void showMenu();

    /** Wyświetla panel generatora nowej ewidencji. */
    void showGenerator();

    /** Wyświetla panel edytora istniejących plików. */
    void showEditor();

    /** Wyświetla panel generowania karty urlopowej (może być tożsamy z edytorem). */
    void showVacation();

    /** Zamyka aplikację. */
    void exitApp();
}
