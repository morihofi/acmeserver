/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.postsetup;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.databaseConfig.JDBCUrlDatabaseConfig;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.postsetup.inputcheck.FQDNInputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.InputChecker;
import de.morihofi.acmeserver.postsetup.inputcheck.PortInputChecker;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.password.SecurePasswordGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

/**
 * The PostSetup class represents the post-setup configuration assistant for the ACME server. It allows users to configure server settings.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public class PostSetup extends WindowBase {

    private static final Logger LOG = LogManager.getLogger(PostSetup.class);

    /**
     * Runs the PostSetup configuration assistant with the specified parameters.
     *
     * @param serverInstance The ServerInstance.
     * @param filesDir       The directory where configuration files are stored.
     * @param args           The command-line arguments passed to the application.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public static void run(ServerInstance serverInstance, Path filesDir, String[] args) throws IOException,
            InterruptedException {
        new PostSetup(serverInstance, filesDir).run(args);
    }

    private final ServerInstance serverInstance;
    private final Path filesDir;
    private final TextBox fqdnTextBox = new TextBox();
    private final TextBox httpPortTextBox = new TextBox();
    private final TextBox httpsPortTextBox = new TextBox();
    private String fqdn = "";
    private int portHttp;
    private int portHttps;

    /**
     * Creates a new instance of the PostSetup class.
     *
     * @param serverInstance The ServerInstance
     * @param filesDir       The directory where configuration files are stored.
     */
    public PostSetup(ServerInstance serverInstance, Path filesDir) {
        this.serverInstance = serverInstance;
        this.filesDir = filesDir;
    }

    /**
     * Initializes the PostSetup configuration assistant and sets up the initial values for FQDN, HTTP port, HTTPS port, and background
     * appearance.
     *
     * @param textGUI The WindowBasedTextGUI for displaying the user interface.
     */
    @Override
    public void init(WindowBasedTextGUI textGUI) {

        try {
            fqdn = NetworkHelper.getLocalFqdn();
            if (fqdn == null) {
                fqdn = serverInstance.getAppConfig().getServer().getDnsName();
            }
        } catch (SocketException ignored) {
        }
        portHttp = serverInstance.getAppConfig().getServer().getPorts().getHttp();
        portHttps = serverInstance.getAppConfig().getServer().getPorts().getHttps();

        // Set background
        prepareBackground(textGUI);
    }

    /**
     * Creates and returns a panel for configuring HTTP and HTTPS ports, including input fields for HTTP and HTTPS port numbers.
     *
     * @return A panel containing input fields for HTTP and HTTPS port configuration.
     */
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

    /**
     * Creates and returns a panel for selecting the certificate and key storage method using a ComboBox.
     *
     * @return A panel with a ComboBox for selecting the certificate and key storage method.
     */
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

    /**
     * Creates and returns a panel for entering the Fully Qualified Domain Name (FQDN) or DNS name of the server. This is essential for
     * certificate generation.
     *
     * @return A panel with an input field for setting the server's FQDN or DNS name.
     */
    private Component getFqdnBoxPanel() {
        Panel fqdnBoxPanel = new Panel();
        fqdnBoxPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        Panel topPanel = new Panel();
        topPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL)); // Setzt das Layout für topPanel

        Label label = new Label(
                "Enter the FQDN or DNS Name of this server.\nThis is important, as this is written into certificates. \nFor example "
                        + "\"acme.example.com\" or \"acmeserver\"");
        topPanel.addComponent(label);

        // Erstellen eines TextBox-Elements, das die gesamte verfügbare Breite einnimmt
        fqdnTextBox.setLayoutData(LinearLayout.createLayoutData(
                LinearLayout.Alignment.Fill)); // Setzt die TextBox so, dass sie den verfügbaren Platz ausfüllt
        topPanel.addComponent(fqdnTextBox);

        fqdnBoxPanel.addComponent(topPanel);
        return fqdnBoxPanel.withBorder(Borders.doubleLine("FQDN/DNS Name Setup"));
    }

    /**
     * Overrides the method from the parent class to handle the GUI setup after the GUI thread has started.
     *
     * @param textGUI The WindowBasedTextGUI instance used for GUI interactions.
     * @throws InterruptedException If the thread is interrupted while waiting.
     * @throws IOException          If an I/O error occurs.
     */
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
                dialogPanel.addComponent(linePanel);
            }

            dialogMain.addComponent(dialogPanel);
        }

        Panel buttonPanel = new Panel();
        buttonPanel.addComponent(new Button("Save", () -> {

            InputChecker fqdnChecker = new FQDNInputChecker();
            InputChecker portChecker = new PortInputChecker();

            if (!fqdnChecker.isValid(fqdnTextBox.getText())) {
                // Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid FQDN", "The FQDN you have entered is invalid.");
                return;
            }
            if (!portChecker.isValid(httpPortTextBox.getText())) {
                // Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid HTTP Port", "The insecure HTTP port you have entered is invalid.");
                return;
            }
            if (!portChecker.isValid(httpsPortTextBox.getText())) {
                // Input is invalid
                MessageDialog.showMessageDialog(textGUI, "[ ! ] Invalid HTTPS Port", "The secure HTTPS port you have entered is invalid.");
                return;
            }

            MessageDialog.showMessageDialog(textGUI, "[ i ] Configuration complete",
                    "Basic configuration completed successfully.\nIf you want (recommended) you can edit \"settings.json\" in the "
                            + "\"serverdata\" folder");

            dialogWindow.close();
        }));

        buttonPanel.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false));
        dialogMain.addComponent(buttonPanel);

        dialogWindow.setComponent(dialogMain);
        textGUI.addWindowAndWait(dialogWindow);

        // User has set/changed configuration, now applying variables

        // FQDN
        serverInstance.getAppConfig().getServer().setDnsName(fqdnTextBox.getText());

        // Network Ports
        serverInstance.getAppConfig().getServer().getPorts().setHttp(Integer.parseInt(httpPortTextBox.getText()));
        serverInstance.getAppConfig().getServer().getPorts().setHttps(Integer.parseInt(httpsPortTextBox.getText()));

        // KeyStore, use PKCS12 with random password as default. But only if user hasn't changed default
        if (serverInstance.getAppConfig().getKeyStore().getPassword().equals("test") && serverInstance.getAppConfig().getKeyStore() instanceof PKCS12KeyStoreParams) {
            PKCS12KeyStoreParams keyStoreParams = new PKCS12KeyStoreParams();
            keyStoreParams.setLocation(filesDir.resolve("keystore.p12").toAbsolutePath().toString());
            keyStoreParams.setPassword(SecurePasswordGenerator.generateSecurePassword());
            serverInstance.getAppConfig().setKeyStore(keyStoreParams);
        }

        // Set database, use H2 as default. But only if using default config

        JDBCUrlDatabaseConfig jdbcUrlDatabaseConfig = new JDBCUrlDatabaseConfig();
        jdbcUrlDatabaseConfig.setUser("acmeuser");
        jdbcUrlDatabaseConfig.setPassword(SecurePasswordGenerator.generateSecurePassword());
        jdbcUrlDatabaseConfig.setJdbcUrl("jdbc:h2:" + filesDir.resolve("acmedatabase").toAbsolutePath() + ";DB_CLOSE_DELAY=-1");
        serverInstance.getAppConfig().setDatabase(jdbcUrlDatabaseConfig);

        textGUI.getScreen().close();
        serverInstance.saveServerConfiguration();
    }

    /**
     * Prepares and sets the background for the given WindowBasedTextGUI with customized styling.
     *
     * @param textGUI The WindowBasedTextGUI instance for which the background is prepared.
     */
    private void prepareBackground(WindowBasedTextGUI textGUI) {
        textGUI.getBackgroundPane().setComponent(new EmptySpace(TextColor.ANSI.BLUE) {
            @Override
            protected ComponentRenderer<EmptySpace> createDefaultRenderer() {
                return new ComponentRenderer<>() {
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
