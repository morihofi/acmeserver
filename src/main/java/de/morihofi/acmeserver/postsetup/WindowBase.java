package de.morihofi.acmeserver.postsetup;

import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;

import java.io.IOException;

/**
 * An abstract class representing a basic window-based application framework using Lanterna library. Subclasses should extend this class and
 * implement the {@link #init(WindowBasedTextGUI)} method to define the main functionality of the application.
 *
 * @see MultiWindowTextGUI
 * @see Screen
 */
public abstract class WindowBase {

    /**
     * Runs the Lanterna-based application.
     *
     * @param args Command-line arguments passed to the application.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the application is interrupted.
     */
    void run(String[] args) throws IOException, InterruptedException {
        Screen screen = new TerminalFactory(args).createScreen();
        screen.startScreen();
        MultiWindowTextGUI textGUI = createTextGUI(screen);
        String theme = extractTheme(args);
        if (theme != null) {
            textGUI.setTheme(LanternaThemes.getRegisteredTheme(theme));
        }
        textGUI.setBlockingIO(false);
        textGUI.setEOFWhenNoWindows(true);
        // noinspection ResultOfMethodCallIgnored
        textGUI.isEOFWhenNoWindows();   // No meaning, just to silence IntelliJ:s "is never used" alert

        try {
            init(textGUI);
            AsynchronousTextGUIThread guiThread = (AsynchronousTextGUIThread) textGUI.getGUIThread();
            guiThread.start();
            afterGUIThreadStarted(textGUI);
            guiThread.waitForStop();
        } finally {
            screen.stopScreen();
        }
    }

    /**
     * Extracts the theme name from the command-line arguments.
     *
     * @param args Command-line arguments.
     * @return The extracted theme name or null if not found.
     */
    private String extractTheme(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--theme") && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Creates a {@link com.googlecode.lanterna.gui2.MultiWindowTextGUI} using the provided screen.
     *
     * @param screen The Lanterna screen to use.
     * @return A MultiWindowTextGUI instance.
     */
    protected MultiWindowTextGUI createTextGUI(Screen screen) {
        return new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
    }

    /**
     * Initializes the application's main functionality.
     *
     * @param textGUI The WindowBasedTextGUI instance to use for creating windows and widgets.
     */
    public abstract void init(WindowBasedTextGUI textGUI);

    /**
     * Called after the GUI thread has started. Subclasses can override this method to perform additional setup after the GUI thread is
     * running.
     *
     * @param textGUI The WindowBasedTextGUI instance.
     * @throws InterruptedException If the application is interrupted.
     * @throws IOException          If an I/O error occurs.
     */
    public void afterGUIThreadStarted(WindowBasedTextGUI textGUI) throws InterruptedException, IOException {
        // By default, do nothing
    }
}
