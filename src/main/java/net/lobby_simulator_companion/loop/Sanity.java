package net.lobby_simulator_companion.loop;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.ui.startup.GithubPanel;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class Sanity {

    private static boolean headless = false;

    public static boolean check() {
        boolean[] checks = {
                checkGraphics(),
                checkUpdate(),
                checkJava()
        };

        for (boolean check : checks) {
            if (!check)
                return false;
        }
        return true;
    }

    /**
     * Check for a valid graphical environment.
     */
    private static boolean checkGraphics() {
        if (GraphicsEnvironment.isHeadless()) {
            headless = true;
            message("This program requires a graphical environment to run!\nIt's weird that you even got this far.");
            return false;
        }
        return true;
    }

    /**
     * Check the current Java Version.
     */
    private static boolean checkJava() {
        String v = System.getProperty("java.version");
        log.info("Java version: {}", v);
        if (!v.equals("9")) {
            double version = Double.parseDouble(v.substring(0, v.indexOf('.', 2)));
            if (version < 1.8) {
                message("Java version 8 or higher is required!\nYou are currently using " + version + "!\n");
                return false;
            }
        }
        return true;
    }

    private static boolean checkUpdate() {
        GithubPanel mp = new GithubPanel();

        if (!mp.prompt()) {
            message("At least one update located is mandatory!\nSome updates can be very important for functionality and your security.\n"
                    + "Please update LOOP before running!");
            return false;
        } else {
            log.info("Application is up to date!");
        }
        return true;
    }

    private static void message(String out) {
        log.error(out);
        if (!headless)
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
