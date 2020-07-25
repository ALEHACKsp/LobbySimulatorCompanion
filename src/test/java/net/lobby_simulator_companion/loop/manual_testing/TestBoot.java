package net.lobby_simulator_companion.loop.manual_testing;

import net.lobby_simulator_companion.loop.Boot;
import net.lobby_simulator_companion.loop.DevModeConfigurer;
import net.lobby_simulator_companion.loop.Factory;

import static javax.swing.SwingUtilities.invokeLater;
import static net.lobby_simulator_companion.loop.util.LangUtil.unchecked;

/**
 * @author NickyRamone
 */
public class TestBoot {


    public static void main(String[] args) throws Exception {
        DevModeConfigurer.init();
        Boot.main(new String[]{});

        invokeLater(() -> unchecked(() ->
                new DebugPanel(Factory.dbdLogMonitor(), Factory.loopDataService())).get());
    }

}
