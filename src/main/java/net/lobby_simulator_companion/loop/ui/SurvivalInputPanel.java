package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author NickyRamone
 */
public class SurvivalInputPanel extends JPanel {

    public static final String EVENT_SURVIVAL_INPUT_DONE = "survival_input_done";
    private static final int SURVIVAL_INPUT_WINDOW_DELAY = 3000;


    private enum SelectionState {NONE, ESCAPED, DIED, IGNORE}

    private final LoopDataService dataService;
    private final GameStateManager gameStateManager;
    private final UiEventOrchestrator uiEventOrchestrator;

    private JLabel escapedButton;
    private JLabel diedButton;
    private JLabel ignoreButton;
    private Timer timer;

    private SelectionState selectionState = SelectionState.NONE;


    public SurvivalInputPanel(LoopDataService dataService,
                              GameStateManager gameStateManager, UiEventOrchestrator uiEventOrchestrator) {

        this.dataService = dataService;
        this.gameStateManager = gameStateManager;
        this.uiEventOrchestrator = uiEventOrchestrator;
        initTimer();

        UIManager.put("ToolTip.font", ResourceFactory.getRobotoFont().deriveFont(14f));
        escapedButton = ComponentUtils.createButtonLabel(
                UiConstants.COLOR__INFO_PANEL__NAME__FG,
                "Click to indicate that you survived this match",
                ResourceFactory.Icon.ESCAPED_BUTTON, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSurvivalStatusChange(SelectionState.ESCAPED);
                    }
                });
        diedButton = ComponentUtils.createButtonLabel(
                UiConstants.COLOR__INFO_PANEL__NAME__FG,
                "Click to indicate that you died on this match",
                ResourceFactory.Icon.DIED_BUTTON, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSurvivalStatusChange(SelectionState.DIED);
                    }
                });

        ignoreButton = ComponentUtils.createButtonLabel(
                UiConstants.COLOR__INFO_PANEL__NAME__FG,
                "Click to ignore reporting your survival status for this match.",
                ResourceFactory.Icon.IGNORE_BUTTON, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSurvivalStatusChange(SelectionState.IGNORE);
                    }
                }
        );

        JLabel msg1Label = new JLabel("Did you survive or die this match?");
        msg1Label.setForeground(Color.BLACK);
        msg1Label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        msg1Label.setFont(ResourceFactory.getRobotoFont());

        JPanel msgPanel1 = new JPanel();
        msgPanel1.setLayout(new BoxLayout(msgPanel1, BoxLayout.Y_AXIS));
        msgPanel1.setBackground(Color.YELLOW);
        msgPanel1.add(msg1Label);

        JPanel survivalStatusButtonPanel = new JPanel();
        survivalStatusButtonPanel.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        survivalStatusButtonPanel.add(escapedButton);
        survivalStatusButtonPanel.add(diedButton);
        survivalStatusButtonPanel.add(ignoreButton);
        survivalStatusButtonPanel.setVisible(true);

        JPanel survivalMessagePanel = new JPanel();
        survivalMessagePanel.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        survivalMessagePanel.add(msgPanel1);
        survivalMessagePanel.setVisible(true);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(survivalStatusButtonPanel);
        add(survivalMessagePanel);
        setVisible(false);
    }

    private void handleSurvivalStatusChange(SelectionState newSelectionState) {
        if (newSelectionState == this.selectionState) {
            resetButtons();
            this.selectionState = SelectionState.NONE;
            timer.stop();
            return;
        }

        resetButtons();

        if (newSelectionState == SelectionState.IGNORE) {
            ignoreButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.IGNORE_BUTTON_PRESSED));
        } else if (newSelectionState == SelectionState.ESCAPED) {
            escapedButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.ESCAPED_BUTTON_PRESSED));
        } else {
            diedButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.DIED_BUTTON_PRESSED));
        }

        this.selectionState = newSelectionState;
        timer.restart();
    }

    private void initTimer() {
        timer = new Timer(SURVIVAL_INPUT_WINDOW_DELAY, e -> {
            timer.stop();

            if (!SelectionState.IGNORE.equals(selectionState)) {
                gameStateManager.notifySurvivalUserInput(SelectionState.ESCAPED.equals(selectionState));
            }
            firePropertyChange(EVENT_SURVIVAL_INPUT_DONE, null, null);
        });
    }

    public void reset() {
        resetButtons();
        selectionState = SelectionState.NONE;
        timer.stop();
    }

    private void resetButtons() {
        escapedButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.ESCAPED_BUTTON));
        diedButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.DIED_BUTTON));
        ignoreButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.IGNORE_BUTTON));
    }

}
