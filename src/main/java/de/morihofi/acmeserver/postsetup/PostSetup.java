package de.morihofi.acmeserver.postsetup;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.postsetup.inputcheck.FQDNInputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.InputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.PortInputChecker;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import org.hibernate.annotations.Check;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class PostSetup extends WindowBase {

    private CryptoStoreManager cryptoStoreManager;
    private Config appConfig;
    private Path filesDir;

    public static void run(CryptoStoreManager cryptoStoreManager, Config appConfig, Path filesDir, String[] args) throws IOException, InterruptedException {
        new PostSetup(cryptoStoreManager, appConfig, filesDir).run(args);
    }

    public PostSetup(CryptoStoreManager cryptoStoreManager, Config appConfig, Path filesDir) {
        this.cryptoStoreManager = cryptoStoreManager;
        this.appConfig = appConfig;
        this.filesDir = filesDir;
    }

    int i = 0;

    private String showDialog(WindowBasedTextGUI textGUI, String title, String initialContent, InputChecker checker) throws IOException {
        final AtomicReference<String> result = new AtomicReference<>(initialContent);
        final BasicWindow dialogWindow = new BasicWindow();
        i++;
        dialogWindow.setHints(List.of(Window.Hint.CENTERED));
        dialogWindow.setTitle(i + "");
        Panel dialogPanel = new Panel(new GridLayout(1));
        TextBox inputBox = new TextBox(result.get());
        dialogPanel.addComponent(inputBox.withBorder(Borders.singleLine(title)));

        Panel buttonPanel = new Panel();
        buttonPanel.addComponent(new Button("OK", () -> {
            if (checker.isValid(inputBox.getText())) {
                result.set(inputBox.getText());
                dialogWindow.close();
            } else {
                //Input is invalid
                MessageDialog.showMessageDialog(textGUI, "Invalid Input", "The input you have entered is invalid.");
            }
        }));
        buttonPanel.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false));
        dialogPanel.addComponent(buttonPanel);
        dialogWindow.setComponent(dialogPanel);

        //textGUI.addWindowAndWait(dialogWindow);
        textGUI.addWindow(dialogWindow);
        textGUI.updateScreen();
        textGUI.waitForWindowToClose(dialogWindow);


        return result.get();
    }

    private String fqdn = "";
    private int portHttp;
    private int portHttps;

    @Override
    public void init(WindowBasedTextGUI textGUI) {


        try {
            fqdn = NetworkHelper.getLocalFqdn();
            if (fqdn == null) {
                fqdn = appConfig.getServer().getDnsName();
            }
        } catch (SocketException e) {
        }
        portHttp = appConfig.getServer().getPorts().getHttp();
        portHttps = appConfig.getServer().getPorts().getHttps();

        //Set background
        prepareBackground(textGUI);

    }

    private Component getHttpHttpsBoxPanel() {
        Panel httpHttpsPanel = new Panel();
        httpHttpsPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        Panel topPanel = new Panel();
        topPanel.addComponent(new Label("HTTP Port"));
        topPanel.addComponent(new TextBox());

        Panel bottomPanel = new Panel();
        bottomPanel.addComponent(new Label("HTTPS Port"));
        bottomPanel.addComponent(new TextBox());

        httpHttpsPanel.addComponent(topPanel.withBorder(Borders.singleLine()));
        httpHttpsPanel.addComponent(bottomPanel.withBorder(Borders.singleLine()));

        return httpHttpsPanel.withBorder(Borders.doubleLine("Port configuration"));
    }

    private Component getKeyStorePanel() {
        Panel keyStorePanel = new Panel();
        keyStorePanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        Panel topPanel = new Panel();
        topPanel.addComponent(new Label("Set how to store certificates and keys"));
        topPanel.addComponent(new ComboBox<String>()
                .addItem("PKCS#12 KeyStore (.p12 File)")
                .addItem("PKCS#11 Hardware Security Module")
                .withBorder(Borders.singleLine())
        );


        keyStorePanel.addComponent(topPanel);

        return keyStorePanel.withBorder(Borders.doubleLine("KeyStore"));
    }

    private Component getFqdnBoxPanel() {
        Panel fqdnBoxPanel = new Panel();
        fqdnBoxPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        Panel topPanel = new Panel();
        topPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL)); // Setzt das Layout für topPanel

        Label label = new Label("Enter the FQDN or DNS Name of this server.\nThis is important, as this is written into certificates. \nFor example \"acme.example.com\" or \"acmeserver\"");
        topPanel.addComponent(label);

        // Erstellen eines TextBox-Elements, das die gesamte verfügbare Breite einnimmt
        TextBox textBox = new TextBox();
        textBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)); // Setzt die TextBox so, dass sie den verfügbaren Platz ausfüllt
        topPanel.addComponent(textBox);

        fqdnBoxPanel.addComponent(topPanel);
        return fqdnBoxPanel.withBorder(Borders.doubleLine("FQDN/DNS Name Setup"));
    }


    @Override
    public void afterGUIThreadStarted(WindowBasedTextGUI textGUI) throws InterruptedException, IOException {
        super.afterGUIThreadStarted(textGUI);


        final BasicWindow dialogWindow = new BasicWindow();
        dialogWindow.setHints(List.of(Window.Hint.CENTERED));
        dialogWindow.setTitle("ACME Server Setup");

        Panel dialogMain = new Panel(new GridLayout(1));
        {
            Panel dialogPanel = new Panel(new LinearLayout(Direction.VERTICAL));

            {
                Panel linePanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
                linePanel.addComponent(getHttpHttpsBoxPanel());
                linePanel.addComponent(getFqdnBoxPanel()                );
                //linePanel.addComponent(getKeyStorePanel().setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
                dialogPanel.addComponent(linePanel);
            }
            /*{
                Panel linePanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
                linePanel.addComponent(getFqdnBoxPanel()
                        .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center))
                );
                dialogPanel.addComponent(linePanel);
            }*/

            dialogMain.addComponent(dialogPanel);
        }

        Panel buttonPanel = new Panel();
        buttonPanel.addComponent(new Button("Save", () -> {
            dialogWindow.close();
        }));


        buttonPanel.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false));
        dialogMain.addComponent(buttonPanel);


        /*
        TextBox fqdnInputBox = new TextBox(fqdn);
        dialogPanel.addComponent(fqdnInputBox);

        Panel buttonPanel = new Panel();
        buttonPanel.addComponent(new Button("Save", () -> {
           *//* if(checker.isValid(fqdnInputBox.getText())){
                result.set(fqdnInputBox.getText());
                dialogWindow.close();
            }else{
                //Input is invalid
                MessageDialog.showMessageDialog(textGUI, "Invalid Input", "The input you have entered is invalid.");
            }*//*
        }));
        buttonPanel.addComponent(new Button("I'll do it later", () -> {
           dialogWindow.close();
        }));
        buttonPanel.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false));
        dialogPanel.addComponent(buttonPanel);
        */

        dialogWindow.setComponent(dialogMain);

        //textGUI.addWindowAndWait(dialogWindow);
        textGUI.addWindow(dialogWindow);
        textGUI.updateScreen();
        textGUI.waitForWindowToClose(dialogWindow);
    }

    private void prepareBackground(WindowBasedTextGUI textGUI) {
        textGUI.getBackgroundPane().setComponent(new EmptySpace(TextColor.ANSI.BLUE) {
            @Override
            protected ComponentRenderer<EmptySpace> createDefaultRenderer() {
                return new ComponentRenderer<EmptySpace>() {
                    @Override
                    public TerminalSize getPreferredSize(EmptySpace component) {
                        return TerminalSize.ONE;
                    }

                    @Override
                    public void drawComponent(TextGUIGraphics graphics, EmptySpace component) {
                        graphics.setForegroundColor(TextColor.ANSI.CYAN);
                        graphics.setBackgroundColor(TextColor.ANSI.BLUE);
                        graphics.setModifiers(EnumSet.of(SGR.BOLD));
                        graphics.fill(' ');
                        graphics.putString(1, 0, "ACME Server (v." + Main.buildMetadataVersion + ") Post-Setup Assistant");
                    }
                };
            }
        });
    }
}
