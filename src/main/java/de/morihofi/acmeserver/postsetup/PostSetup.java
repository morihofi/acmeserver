package de.morihofi.acmeserver.postsetup;

import com.google.gson.Gson;
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
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS11KeyStoreParams;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.postsetup.inputcheck.FQDNInputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.InputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.PortInputChecker;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.password.SecurePasswordGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Check;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
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
    public static final Logger log = LogManager.getLogger(PostSetup.class);

    public static void run(CryptoStoreManager cryptoStoreManager, Config appConfig, Path filesDir, String[] args) throws IOException, InterruptedException {
        new PostSetup(cryptoStoreManager, appConfig, filesDir).run(args);
    }

    public PostSetup(CryptoStoreManager cryptoStoreManager, Config appConfig, Path filesDir) {
        this.cryptoStoreManager = cryptoStoreManager;
        this.appConfig = appConfig;
        this.filesDir = filesDir;
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
        topPanel.addComponent(httpPortTextBox);

        Panel bottomPanel = new Panel();
        bottomPanel.addComponent(new Label("HTTPS Port"));
        bottomPanel.addComponent(httpsPortTextBox);

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
        fqdnTextBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)); // Setzt die TextBox so, dass sie den verfügbaren Platz ausfüllt
        topPanel.addComponent(fqdnTextBox);

        fqdnBoxPanel.addComponent(topPanel);
        return fqdnBoxPanel.withBorder(Borders.doubleLine("FQDN/DNS Name Setup"));
    }

    TextBox fqdnTextBox = new TextBox();
    TextBox httpPortTextBox = new TextBox();
    TextBox httpsPortTextBox = new TextBox();


    @Override
    public void afterGUIThreadStarted(WindowBasedTextGUI textGUI) throws InterruptedException, IOException {
        super.afterGUIThreadStarted(textGUI);

        fqdnTextBox.setText(fqdn);
        httpPortTextBox.setText(String.valueOf(portHttp));
        httpsPortTextBox.setText(String.valueOf(portHttps));


        final BasicWindow dialogWindow = new BasicWindow();
        dialogWindow.setHints(List.of(Window.Hint.CENTERED));
        dialogWindow.setTitle("ACME Server Setup");

        Panel dialogMain = new Panel(new GridLayout(1));
        {
            Panel dialogPanel = new Panel(new LinearLayout(Direction.VERTICAL));

            {
                Panel linePanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
                linePanel.addComponent(getHttpHttpsBoxPanel());
                linePanel.addComponent(getFqdnBoxPanel());
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

            InputChecker fqdnChecker = new FQDNInputChecker();
            InputChecker portChecker = new PortInputChecker();

            if (!fqdnChecker.isValid(fqdnTextBox.getText())) {
                //Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid FQDN", "The FQDN you have entered is invalid.");
                return;
            }
            if (!portChecker.isValid(httpPortTextBox.getText())) {
                //Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid HTTP Port", "The insecure HTTP port you have entered is invalid.");
                return;
            }
            if (!portChecker.isValid(httpsPortTextBox.getText())) {
                //Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid HTTPS Port", "The secure HTTPS port you have entered is invalid.");
                return;
            }

            MessageDialog.showMessageDialog(textGUI, "[ i ] Configuration complete", "Basic configuration completed successfully.\nIf you want (recommended) you can edit \"settings.json\" in the \"serverdata\" folder");

            dialogWindow.close();
        }));


        buttonPanel.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false));
        dialogMain.addComponent(buttonPanel);

        dialogWindow.setComponent(dialogMain);
        textGUI.addWindowAndWait(dialogWindow);

        //User has set/changed configuration, now applying variables

        //FQDN
        appConfig.getServer().setDnsName(fqdnTextBox.getText());

        //Network Ports
        appConfig.getServer().getPorts().setHttp(Integer.parseInt(httpPortTextBox.getText()));
        appConfig.getServer().getPorts().setHttps(Integer.parseInt(httpsPortTextBox.getText()));

        //KeyStore, use PKCS12 with random password as default. But only if user hasn't changed default
        if(appConfig.getKeyStore().getPassword().equals("test") && appConfig.getKeyStore() instanceof PKCS12KeyStoreParams){
            PKCS12KeyStoreParams keyStoreParams = new PKCS12KeyStoreParams();
            keyStoreParams.setLocation(filesDir.resolve("keystore.p12").toAbsolutePath().toString());
            keyStoreParams.setPassword(SecurePasswordGenerator.generateSecurePassword());
            appConfig.setKeyStore(keyStoreParams);
        }

        //Set database, use H2 as default. But only if using default config
        appConfig.getDatabase().setEngine("h2");
        appConfig.getDatabase().setHost("");
        appConfig.getDatabase().setPassword(SecurePasswordGenerator.generateSecurePassword());
        appConfig.getDatabase().setName(filesDir.resolve("acmedatabase").toAbsolutePath().toString());
        appConfig.getDatabase().setUser("acmeuser");


        textGUI.getScreen().close();

        log.info("Saving new configuration");
        Gson gson = new Gson();
        JSONObject jso = new JSONObject(gson.toJson(appConfig));
        String formattedJson = jso.toString(4);

        Files.writeString(filesDir.resolve("settings.json"), formattedJson);
        log.info("Configuration has been saved successfully");

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
