package evidence;

/**
 * Interfejs zarządzający nawigacją w aplikacji.
 * Pozwala komponentom zmieniać widoki bez wiedzy o szczegółach implementacji okna.
 */
public interface ViewNavigator {
    void showMenu();
    void showGenerator();
    void showEditor();
    void exitApp();
    void showVacation();
}
