package de.morihofi.acmeserver.postsetup;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.MouseCaptureMode;

public class TerminalFactory extends DefaultTerminalFactory {

    public TerminalFactory() {
    }

    public TerminalFactory(String[] args) {
        parseArgs(args);
    }

    public void parseArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String[] tok = arg.split("=", 2);
            arg = tok[0]; // only the part before "="
            String par = tok.length > 1 ? tok[1] : "";
            if ("--text-terminal".equals(arg) || "--no-swing".equals(arg)) {
                setPreferTerminalEmulator(false);
                setForceTextTerminal(true);
            } else if ("--awt".equals(arg)) {
                setForceTextTerminal(false);
                setPreferTerminalEmulator(true);
                setForceAWTOverSwing(true);
            } else if ("--swing".equals(arg)) {
                setForceTextTerminal(false);
                setPreferTerminalEmulator(true);
                setForceAWTOverSwing(false);
            } else if ("--mouse-click".equals(arg)) {
                setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE);
            } else if ("--mouse-drag".equals(arg)) {
                setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE_DRAG);
            } else if ("--mouse-move".equals(arg)) {
                setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE_DRAG_MOVE);
            } else if ("--telnet-port".equals(arg)) {
                int port = 1024; // default for option w/o param
                try {
                    port = Integer.parseInt(par);
                } catch (NumberFormatException e) {
                }
                setTelnetPort(port);
            } else if ("--with-timeout".equals(arg)) {
                int inputTimeout = 40; // default for option w/o param
                try {
                    inputTimeout = Integer.parseInt(par);
                } catch (NumberFormatException e) {
                }
                setInputTimeout(inputTimeout);
            }
        }
    }

}
