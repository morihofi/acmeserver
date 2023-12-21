package de.morihofi.acmeserver.postsetup;

import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;

import java.io.IOException;

public abstract class WindowBase {
    void run(String[] args) throws IOException, InterruptedException {
        Screen screen = new TerminalFactory(args).createScreen();
        screen.startScreen();
        MultiWindowTextGUI textGUI = createTextGUI(screen);
        String theme = extractTheme(args);
        if(theme != null) {
            textGUI.setTheme(LanternaThemes.getRegisteredTheme(theme));
        }
        textGUI.setBlockingIO(false);
        textGUI.setEOFWhenNoWindows(true);
        //noinspection ResultOfMethodCallIgnored
        textGUI.isEOFWhenNoWindows();   //No meaning, just to silence IntelliJ:s "is never used" alert

        try {
            init(textGUI);
            AsynchronousTextGUIThread guiThread = (AsynchronousTextGUIThread)textGUI.getGUIThread();
            guiThread.start();
            afterGUIThreadStarted(textGUI);
            guiThread.waitForStop();
        }
        finally {
            screen.stopScreen();
        }
    }

    private String extractTheme(String[] args) {
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("--theme") && i + 1 < args.length) {
                return args[i+1];
            }
        }
        return null;
    }

    protected MultiWindowTextGUI createTextGUI(Screen screen) {
        return new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
    }

    public abstract void init(WindowBasedTextGUI textGUI);
    public void afterGUIThreadStarted(WindowBasedTextGUI textGUI) throws InterruptedException, IOException {
        // By default, do nothing
    }
}
