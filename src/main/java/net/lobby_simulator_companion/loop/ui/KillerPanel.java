package net.lobby_simulator_companion.loop.ui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.service.GameEvent;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.FontUtil;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static javax.swing.SwingUtilities.invokeLater;
import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.WIDTH__INFO_PANEL__NAME_COLUMN;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.WIDTH__INFO_PANEL__VALUE_COLUMN;
import static net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator.UiEvent;

/**
 * @author NickyRamone
 */
@Slf4j
public class KillerPanel extends JPanel {

    /**
     * When the user updates the killer player description, the change is not applied immediately
     * (we don't want to do an update for every single character), so we defer the write with
     * a specific delay.
     */
    private static final int DESCRIPTION_UPDATE_DELAY_MS = 5000;
    private static final int MAX_KILLER_DESCRIPTION_SIZE = 1256;
    private static final Font font = ResourceFactory.getRobotoFont();

    @RequiredArgsConstructor
    private enum InfoType {
        KILLER_PLAYER_NAMES("Previous names seen:"),
        TIMES_ENCOUNTERED("Times encountered:"),
        MATCHES_AGAINST_PLAYER("Matches played against:"),
        TIME_PLAYED_AGAINST("Total time played against:"),
        ESCAPES_AGAINST("Total escapes against:"),
        DEATHS_BY("Times died against:"),
        NOTES("Your notes:");

        private final String description;

        @Override
        public String toString() {
            return description;
        }
    }

    private final LoopDataService dataService;
    private final GameStateManager gameStateManager;
    private final UiEventOrchestrator uiEventOrchestrator;

    private JLabel playerNameLabel;
    private JLabel playerSteamButton;
    private JLabel titleBarPlayerExtraInfoLabel;
    private JLabel playerRateLabel;
    private NameValueInfoPanel statsContainer;
    private JScrollPane userNotesPane;
    private JLabel userNotesEditButton;
    private JTextArea userNotesArea;

    private Timer userNotesUpdateTimer;


    public KillerPanel(Settings settings, LoopDataService dataService, GameStateManager gameStateManager,
                       UiEventOrchestrator uiEventOrchestrator) {
        this.dataService = dataService;
        this.gameStateManager = gameStateManager;
        this.uiEventOrchestrator = uiEventOrchestrator;

        draw(settings);
        initEventListeners();
    }

    private void initEventListeners() {
        gameStateManager.registerListener(GameEvent.CONNECTED_TO_LOBBY,
                evt -> refreshClear());
        gameStateManager.registerListener(GameEvent.NEW_KILLER_PLAYER,
                evt -> refreshKillerPlayerOnScreen());
        gameStateManager.registerListener(GameEvent.UPDATED_STATS,
                evt -> refreshKillerPlayerOnScreen());
        uiEventOrchestrator.registerListener(UiEvent.UPDATE_KILLER_PLAYER,
                evt -> refreshKillerPlayerOnScreen());
    }

    private void draw(Settings settings) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel collapsablePanel = new CollapsablePanel(
                createTitleBar(),
                createDetailsPanel(),
                settings,
                "ui.panel.killer.collapsed");
        collapsablePanel.addPropertyChangeListener(evt ->
                uiEventOrchestrator.fireEvent(UiEvent.STRUCTURE_RESIZED));

        add(collapsablePanel);
    }

    private JPanel createTitleBar() {

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel summaryLabel = new JLabel("Killer player:");
        summaryLabel.setBorder(border);
        summaryLabel.setForeground(UiConstants.COLOR__TITLE_BAR__FG);
        summaryLabel.setFont(font);

        playerRateLabel = new JLabel();
        playerRateLabel.setBorder(border);
        playerRateLabel.setIcon(ResourceFactory.getIcon(Icon.RATE));
        playerRateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playerRateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playerRateLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                rateKiller();
            }
        });
        playerRateLabel.setVisible(false);

        playerNameLabel = new JLabel();
        playerNameLabel.setBorder(border);
        playerNameLabel.setFont(font);

        playerSteamButton = new JLabel();
        playerSteamButton.setBorder(border);
        playerSteamButton.setIcon(ResourceFactory.getIcon(Icon.STEAM));
        playerSteamButton.setToolTipText("Click to visit this Steam profile on your browser.");
        playerSteamButton.setVisible(false);
        playerSteamButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playerSteamButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                gameStateManager.getKillerPlayer().ifPresent(killerPlayer -> {
                    try {
                        String profileUrl = Factory.appProperties().get("steam.profile_url_prefix") + killerPlayer.getSteamId64();
                        Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                    } catch (IOException e1) {
                        log.error("Failed to open browser at Steam profile.");
                    } catch (URISyntaxException e1) {
                        log.error("Attempted to use an invalid URL for the Steam profile.");
                    }
                });
            }
        });

        titleBarPlayerExtraInfoLabel = new JLabel();
        titleBarPlayerExtraInfoLabel.setBorder(border);
        titleBarPlayerExtraInfoLabel.setFont(font);

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(UiConstants.COLOR__TITLE_BAR__BG);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(summaryLabel);
        container.add(playerRateLabel);
        container.add(playerNameLabel);
        container.add(playerSteamButton);
        container.add(titleBarPlayerExtraInfoLabel);
        container.add(Box.createHorizontalGlue());

        return container;
    }

    private JPanel createDetailsPanel() {
        JLabel notesLabel = new JLabel("Your notes:", JLabel.RIGHT);
        notesLabel.setForeground(UiConstants.COLOR__INFO_PANEL__NAME__FG);
        notesLabel.setFont(font);
        userNotesEditButton = new JLabel();
        userNotesEditButton.setIcon(ResourceFactory.getIcon(Icon.EDIT));
        userNotesEditButton.setToolTipText("Click to show/hide edit panel.");
        userNotesEditButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userNotesEditButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleUserNotesAreaVisibility(!userNotesPane.isVisible());
            }
        });
        userNotesEditButton.setVisible(false);

        statsContainer = new NameValueInfoPanel();
        statsContainer.setSizes(WIDTH__INFO_PANEL__NAME_COLUMN, WIDTH__INFO_PANEL__VALUE_COLUMN, 170);
        statsContainer.addFields(InfoType.class);
        statsContainer.setRight(InfoType.NOTES, userNotesEditButton);

        JPanel container = new JPanel();
        container.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(Box.createVerticalStrut(5));
        container.add(statsContainer);
        container.add(Box.createVerticalStrut(10));
        container.add(createUserNotesPanel());

        return container;
    }


    private void deferDescriptionUpdate() {
        if (userNotesUpdateTimer == null) {
            userNotesUpdateTimer = new Timer();
            userNotesUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    invokeLater(() -> updatePlayerDescription());
                }
            }, DESCRIPTION_UPDATE_DELAY_MS);
        }
    }

    private void updatePlayerDescription() {
        gameStateManager.getKillerPlayer().ifPresent(killerPlayer -> {
            String newNotes = userNotesArea.getText().trim();
            newNotes = newNotes.isEmpty() ? null : newNotes;
            userNotesUpdateTimer = null;

            if (!Objects.equals(newNotes, killerPlayer.getDescription())) {
                killerPlayer.setDescription(newNotes);
                dataService.notifyChange();
            }
        });
    }

    private JPanel createUserNotesPanel() {

        DocumentFilter docFilter = new DocumentFilter() {
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
                deferDescriptionUpdate();
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string, attr);
                deferDescriptionUpdate();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {

                int textLen = text != null ? text.length() : 0;

                if (fb.getDocument().getLength() + textLen <= MAX_KILLER_DESCRIPTION_SIZE) {
                    super.replace(fb, offset, length, text, attrs);
                    deferDescriptionUpdate();
                }
            }
        };

        userNotesArea = new JTextArea(null, 10, 30);
        ((AbstractDocument) userNotesArea.getDocument()).setDocumentFilter(docFilter);
        userNotesArea.setMargin(new Insets(5, 5, 5, 5));
        userNotesArea.setForeground(Color.WHITE);
        userNotesArea.setLineWrap(true);
        userNotesArea.setWrapStyleWord(true);
        userNotesArea.setFont(font);
        userNotesArea.setBackground(new Color(0x85, 0x74, 0xbf));
        userNotesArea.setEditable(true);

        userNotesPane = new JScrollPane(userNotesArea);
        userNotesPane.setBackground(Color.BLACK);
        userNotesPane.setVisible(false);

        JPanel container = new JPanel();
        container.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(Box.createHorizontalGlue());
        container.add(userNotesPane);

        return container;
    }


    private void refreshClear() {
        playerNameLabel.setText(null);
        playerSteamButton.setVisible(false);
        titleBarPlayerExtraInfoLabel.setText(null);
        playerRateLabel.setIcon(null);

        statsContainer.getRight(InfoType.KILLER_PLAYER_NAMES).setText(null);
        statsContainer.getRight(InfoType.KILLER_PLAYER_NAMES).setToolTipText(null);
        statsContainer.getRight(InfoType.TIMES_ENCOUNTERED).setText(null);
        statsContainer.getRight(InfoType.MATCHES_AGAINST_PLAYER).setText(null);
        statsContainer.getRight(InfoType.ESCAPES_AGAINST).setText(null);
        statsContainer.getRight(InfoType.DEATHS_BY).setText(null);
        statsContainer.getRight(InfoType.TIME_PLAYED_AGAINST).setText(null);

        userNotesEditButton.setVisible(false);
        userNotesArea.setText("");
        toggleUserNotesAreaVisibility(false);
    }


    private void refreshKillerPlayerOnScreen() {
        Player killerPlayer = gameStateManager.getKillerPlayer().orElse(null);
        if (killerPlayer == null) {
            return;
        }
        playerNameLabel.setText(killerPlayer.getMostRecentName().map(FontUtil::replaceNonDisplayableChars).orElse(null));
        playerSteamButton.setVisible(true);

        JLabel otherNamesValueLabel = statsContainer.getRight(InfoType.KILLER_PLAYER_NAMES);
        otherNamesValueLabel.setText("--");
        otherNamesValueLabel.setToolTipText(null);

        // show previous killer names
        if (killerPlayer.getNames().size() > 1) {
            List<String> otherNames = new ArrayList<>(killerPlayer.getNames());
            Collections.reverse(otherNames);

            // remove the first element (which corresponds to the current name)
            otherNames.remove(0);
            String previousName = otherNames.remove(0);

            if (!otherNames.isEmpty()) {
                previousName = previousName + " ...";
                String tooltip = String.join(", ", otherNames);
                otherNamesValueLabel.setToolTipText(tooltip);
            }

            otherNamesValueLabel.setText(previousName);
        }

        statsContainer.getRight(InfoType.TIMES_ENCOUNTERED).setText(String.valueOf(killerPlayer.getTimesEncountered()));
        statsContainer.getRight(InfoType.MATCHES_AGAINST_PLAYER).setText(String.valueOf(killerPlayer.getMatchesPlayed()));
        statsContainer.getRight(InfoType.ESCAPES_AGAINST).setText(String.valueOf(killerPlayer.getEscapes()));
        statsContainer.getRight(InfoType.DEATHS_BY).setText(String.valueOf(killerPlayer.getDeaths()));
        statsContainer.getRight(InfoType.TIME_PLAYED_AGAINST).setText(TimeUtil.formatTimeUpToYears(killerPlayer.getSecondsPlayed()));

        refreshKillerPlayerRatingOnScreen();

        userNotesEditButton.setVisible(true);
        if (killerPlayer.getDescription() == null) {
            userNotesArea.setText(null);
            toggleUserNotesAreaVisibility(false);
        } else {
            userNotesArea.setText(killerPlayer.getDescription());
            toggleUserNotesAreaVisibility(true);
        }
    }

    private void toggleUserNotesAreaVisibility(boolean visible) {
        userNotesPane.setVisible(visible);
        uiEventOrchestrator.fireEvent(UiEvent.STRUCTURE_RESIZED);
    }

    private void refreshKillerPlayerRatingOnScreen() {
        gameStateManager.getKillerPlayer().ifPresent(killerPlayer -> {
            Player.Rating peerRating = killerPlayer.getRating();
            if (peerRating == Player.Rating.UNRATED) {
                playerRateLabel.setIcon(ResourceFactory.getIcon(Icon.RATE));
                playerRateLabel.setToolTipText("This player is unrated. Click to rate.");
            } else if (peerRating == Player.Rating.THUMBS_DOWN) {
                playerRateLabel.setIcon(ResourceFactory.getIcon(Icon.THUMBS_DOWN));
                playerRateLabel.setToolTipText("This player is rated negative. Click to rate.");
            } else if (peerRating == Player.Rating.THUMBS_UP) {
                playerRateLabel.setIcon(ResourceFactory.getIcon(Icon.THUMBS_UP));
                playerRateLabel.setToolTipText("This player is rated positive. Click to rate.");
            }
            playerRateLabel.setVisible(true);
        });
    }

    private void rateKiller() {
        if (!gameStateManager.getKillerPlayer().isPresent()) {
            return;
        }

        Player player = gameStateManager.getKillerPlayer().get();
        Player.Rating rating = player.getRating();
        Player.Rating newRating;

        if (Player.Rating.UNRATED.equals(rating)) {
            newRating = Player.Rating.THUMBS_UP;
        } else if (Player.Rating.THUMBS_UP.equals(rating)) {
            newRating = Player.Rating.THUMBS_DOWN;
        } else {
            newRating = Player.Rating.UNRATED;
        }
        player.setRating(newRating);
        dataService.notifyChange();
        refreshKillerPlayerRatingOnScreen();
        uiEventOrchestrator.fireEvent(UiEvent.UPDATE_KILLER_PLAYER_RATING, newRating);
    }

}
