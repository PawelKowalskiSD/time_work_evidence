package evidence;

import evidence.view.MainFrame;

import javax.swing.*;

/**
 * The main entry point for the evidence worker application.
 * This class is responsible for initializing and launching the graphical user interface (GUI).
 */
public class App {
    /**
     * The main method that serves as the entry point for the application.
     * It sets up the application's look and feel and then creates and displays the main window.
     *
     * @param args Command line arguments passed to the application (not used).
     */
    public static void main(String[] args) {
        // Attempt to set the UI's look and feel to match the native operating system.
        // This provides a more familiar and integrated user experience.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // If setting the system look and feel fails (e.g., on a headless environment),
            // the exception is ignored, and Swing will default to its cross-platform look and feel.
        }

        // All Swing components should be created and manipulated on the Event Dispatch Thread (EDT).
        // SwingUtilities.invokeLater ensures that the code to create and show the MainFrame
        // runs on the EDT, which is the proper way to start a Swing application.
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
