package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.service.PlayerIdWrapper;
import net.lobby_simulator_companion.loop.util.FontUtil;
import net.lobby_simulator_companion.loop.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class KillerPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";
    public static final String EVENT_NEW_KILLER_PLAYER = "new_killer_player";
    public static final String EVENT_NEW_KILLER_CHARACTER = "new_killer_character";

    /**
     * When the user updates the killer player description, the change is not applied immediately
     * (we don't want to do an update for every single character), so we defer the write with
     * a specific delay.
     */
    private static final int DESCRIPTION_UPDATE_DELAY_MS = 5000;
    private static final int MAX_KILLER_DESCRIPTION_SIZE = 1256;
    private static final char DEFAULT_NON_DISPLAYABLE_CHAR = '\u0387';
    private static final Logger logger = LoggerFactory.getLogger(KillerPanel.class);
    private static final Font font = ResourceFactory.getRobotoFont();

    private Settings settings;
    private AppProperties appProperties;
    private LoopDataService dataService;
    private SteamProfileDao steamProfileDao;

    private JPanel titleBar;
    private JLabel playerNameLabel;
    private JLabel playerSteamButton;
    private JLabel titleBarCharacterLabel;
    private JLabel playerRateLabel;
    private JLabel detailCollapseButton;
    private JPanel detailsPanel;
    private JLabel characterValueLabel;
    private JLabel otherNamesValueLabel;
    private JLabel timesEncounteredValueLabel;
    private JLabel matchCountValueLabel;
    private JLabel timePlayedValueLabel;
    private JScrollPane userNotesPane;
    private JLabel userNotesEditButton;
    private JTextArea userNotesArea;

    private Player killerPlayer;
    private String killerCharacter;
    private Timer userNotesUpdateTimer;


    public KillerPanel(Settings settings, AppProperties appProperties, LoopDataService dataService, SteamProfileDao steamProfileDao) {
        this.settings = settings;
        this.appProperties = appProperties;
        this.dataService = dataService;
        this.steamProfileDao = steamProfileDao;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        titleBar = createTitleBar();
        detailsPanel = createDetailsPanel();

        add(titleBar);
        add(detailsPanel);
    }

    private JPanel createTitleBar() {

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel summaryLabel = new JLabel("Killer:");
        summaryLabel.setBorder(border);
        summaryLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        summaryLabel.setFont(font);

        playerRateLabel = new JLabel();
        playerRateLabel.setBorder(border);
        playerRateLabel.setIcon(ResourceFactory.getRateIcon());
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
        playerSteamButton.setIcon(ResourceFactory.getSteamIcon());
        playerSteamButton.setToolTipText("Click to visit this Steam profile on your browser.");
        playerSteamButton.setVisible(false);
        playerSteamButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (killerPlayer != null) {
                    try {
                        String profileUrl = Factory.getAppProperties().get("steam.profile_url_prefix") + killerPlayer.getUID();
                        Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                    } catch (IOException e1) {
                        logger.error("Failed to open browser at Steam profile.");
                    } catch (URISyntaxException e1) {
                        logger.error("Attempted to use an invalid URL for the Steam profile.");
                    }
                }
            }
        });

        titleBarCharacterLabel = new JLabel();
        titleBarCharacterLabel.setBorder(border);
        titleBarCharacterLabel.setFont(font);

        detailCollapseButton = new JLabel();
        detailCollapseButton.setIcon(ResourceFactory.getCollapseIcon());
        detailCollapseButton.setBorder(border);
        detailCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailsPanel.setVisible(!detailsPanel.isVisible());
            }
        });

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(summaryLabel);
        container.add(playerRateLabel);

        if (settings.getExperimentalSwitch(1)) {
            container.add(playerNameLabel);
            container.add(playerSteamButton);
        }

        if (settings.getExperimentalSwitch(2)) {
            container.add(titleBarCharacterLabel);
        }
        container.add(Box.createHorizontalGlue());
        container.add(detailCollapseButton);

        return container;
    }

    private JPanel createDetailsPanel() {

        JLabel characterLabel = new JLabel("Character:", JLabel.RIGHT);
        characterLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        characterLabel.setFont(font);
        characterValueLabel = new JLabel();
        characterValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        characterValueLabel.setFont(font);

        JLabel otherNamesLabel = new JLabel("Previous names seen:", JLabel.RIGHT);
        otherNamesLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        otherNamesLabel.setFont(font);
        otherNamesValueLabel = new JLabel();
        otherNamesValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        otherNamesValueLabel.setFont(font);

        JLabel timesEncounteredLabel = new JLabel("Times encountered:", JLabel.RIGHT);
        timesEncounteredLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        timesEncounteredLabel.setFont(font);
        timesEncounteredValueLabel = new JLabel();
        timesEncounteredValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        timesEncounteredValueLabel.setFont(font);

        JLabel matchCountLabel = new JLabel("Matches played against:", JLabel.RIGHT);
        matchCountLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        matchCountLabel.setFont(font);
        matchCountValueLabel = new JLabel();
        matchCountValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        matchCountValueLabel.setFont(font);

        JLabel timePlayedLabel = new JLabel("Total time played against:", JLabel.RIGHT);
        timePlayedLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        timePlayedLabel.setFont(font);
        timePlayedValueLabel = new JLabel();
        timePlayedValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        timePlayedValueLabel.setFont(font);

        JLabel notesLabel = new JLabel("Your notes:", JLabel.RIGHT);
        notesLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        notesLabel.setFont(font);
        userNotesEditButton = new JLabel();
        userNotesEditButton.setIcon(ResourceFactory.getEditIcon());
        userNotesEditButton.setToolTipText("Click to show/hide edit panel.");
        userNotesEditButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleUserNotesAreaVisibility(!userNotesPane.isVisible());
            }
        });
        userNotesEditButton.setVisible(false);

        JPanel statsContainer = new JPanel();
        statsContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        statsContainer.setLayout(new GridLayout(0, 2, 10, 5));

        if (settings.getExperimentalSwitch(2)) {
            statsContainer.add(characterLabel);
            statsContainer.add(characterValueLabel);
        }

        if (settings.getExperimentalSwitch(1)) {
            statsContainer.add(otherNamesLabel);
            statsContainer.add(otherNamesValueLabel);
        }
        statsContainer.add(timesEncounteredLabel);
        statsContainer.add(timesEncounteredValueLabel);
        statsContainer.add(matchCountLabel);
        statsContainer.add(matchCountValueLabel);
        statsContainer.add(timePlayedLabel);
        statsContainer.add(timePlayedValueLabel);
        statsContainer.add(notesLabel);
        statsContainer.add(userNotesEditButton);

        JPanel container = new JPanel();
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(statsContainer);
        container.add(Box.createVerticalStrut(10));
        container.add(createUserNotesPanel());
        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                detailCollapseButton.setIcon(ResourceFactory.getCollapseIcon());
                super.componentShown(e);
                settings.set("ui.panel.killer.collapsed", false);
                firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                detailCollapseButton.setIcon(ResourceFactory.getExpandIcon());
                super.componentHidden(e);
                settings.set("ui.panel.killer.collapsed", true);
                firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
            }
        });
        container.setVisible(!settings.getBoolean("ui.panel.killer.collapsed"));

        return container;
    }


    private void deferDescriptionUpdate() {
        if (userNotesUpdateTimer == null) {
            userNotesUpdateTimer = new Timer();
            userNotesUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (killerPlayer != null) {
                        String notes = userNotesArea.getText().trim();
                        notes = notes.isEmpty() ? null : notes;
                        killerPlayer.setDescription(notes);
                        userNotesUpdateTimer = null;
                        dataService.notifyChange();
                    }
                }
            }, DESCRIPTION_UPDATE_DELAY_MS);
        }
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
                if (fb.getDocument().getLength() + text.length() <= MAX_KILLER_DESCRIPTION_SIZE) {
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
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(Box.createHorizontalGlue());
        container.add(userNotesPane);

        return container;
    }


    public void receiveNewKillerPlayer(PlayerIdWrapper idWrapper) {
        String playerName;
        try {
            playerName = steamProfileDao.getPlayerName(idWrapper.getSteamId());
            playerName = FontUtil.replaceNonDisplayableChars(font, playerName, DEFAULT_NON_DISPLAYABLE_CHAR);
        } catch (IOException e) {
            logger.error("Failed to retrieve player's name for steam id#{}.", idWrapper.getSteamId());
            playerName = "";
        }
        String steamId = idWrapper.getSteamId();
        Player storedPlayer = dataService.getPlayerBySteamId(steamId);
        final Player player;

        if (storedPlayer == null) {
            logger.debug("User of id {} not found in the storage. Creating new entry...", steamId);
            player = new Player();
            player.setUID(steamId);
            player.addName(playerName);
            player.setTimesEncountered(1);
            dataService.addPlayer(steamId, player);
        } else {
            logger.debug("User '{}' (id '{}') found in the storage. Updating entry...", playerName, steamId);
            player = storedPlayer;
            player.updateLastSeen();
            player.addName(playerName);
            player.setTimesEncountered(storedPlayer.getTimesEncountered() + 1);
            dataService.notifyChange();
        }

        // the player info is received from an app thread, so we need to push it to the UI thread (EDT)
        SwingUtilities.invokeLater(() -> updateKillerInfo(player));
    }

    public void clearKillerInfo() {
        killerPlayer = null;
        killerCharacter = null;
        playerNameLabel.setText(null);
        playerSteamButton.setVisible(false);
        otherNamesValueLabel.setText(null);
        otherNamesValueLabel.setToolTipText(null);
        titleBarCharacterLabel.setText(null);
        playerRateLabel.setIcon(null);
        timesEncounteredValueLabel.setText(null);
        characterValueLabel.setText(null);
        matchCountValueLabel.setText(null);
        timePlayedValueLabel.setText(null);
        userNotesEditButton.setVisible(false);
        userNotesArea.setText("");
        toggleUserNotesAreaVisibility(false);
    }

    private void updateKillerInfo(Player player) {
        // first of all, save, in case there's unsaved data from previous player
        dataService.save();
        killerPlayer = player;

        playerNameLabel.setText(player.getMostRecentName() != null ? player.getMostRecentName() : "");
        playerSteamButton.setVisible(true);
        otherNamesValueLabel.setText(null);
        otherNamesValueLabel.setToolTipText(null);

        // show previous killer names
        if (player.getNames().size() > 1) {
            List<String> otherNames = new ArrayList<>(player.getNames());
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

        timesEncounteredValueLabel.setText(String.valueOf(player.getTimesEncountered()));
        matchCountValueLabel.setText(String.valueOf(player.getMatchesPlayed()));
        timePlayedValueLabel.setText(TimeUtil.formatTimeElapsed(player.getSecondsPlayed()));

        playerRateLabel.setVisible(true);
        updateRating();

        userNotesEditButton.setVisible(true);
        if (player.getDescription() == null) {
            userNotesArea.setText(null);
            toggleUserNotesAreaVisibility(false);
        } else {
            userNotesArea.setText(player.getDescription());
            toggleUserNotesAreaVisibility(true);
        }

        firePropertyChange(EVENT_NEW_KILLER_PLAYER, null, player);
    }

    public void updateKillerMatchTime(int matchSeconds) {
        if (killerPlayer != null) {
            killerPlayer.incrementSecondsPlayed(matchSeconds);
            timePlayedValueLabel.setText(TimeUtil.formatTimeElapsed(killerPlayer.getSecondsPlayed()));
            dataService.notifyChange();
        }
    }

    private void toggleUserNotesAreaVisibility(boolean visible) {
        userNotesPane.setVisible(visible);
        firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
    }

    public void updateMatchCount() {
        if (killerPlayer == null) {
            return;
        }
        killerPlayer.incrementMatchesPlayed();
        matchCountValueLabel.setText(String.valueOf(killerPlayer.getMatchesPlayed()));
        dataService.notifyChange();
    }

    private void updateRating() {
        if (killerPlayer == null) {
            return;
        }

        Player.Rating peerRating = killerPlayer.getRating();
        if (peerRating == Player.Rating.UNRATED) {
            playerRateLabel.setIcon(ResourceFactory.getRateIcon());
            playerRateLabel.setToolTipText("This player is unrated. Click to rate.");
        } else if (peerRating == Player.Rating.THUMBS_DOWN) {
            playerRateLabel.setIcon(ResourceFactory.getThumbsDownIcon());
            playerRateLabel.setToolTipText("This player is rated negative. Click to rate.");
        } else if (peerRating == Player.Rating.THUMBS_UP) {
            playerRateLabel.setIcon(ResourceFactory.getThumbsUpIcon());
            playerRateLabel.setToolTipText("This player is rated positive. Click to rate.");
        }
    }

    private void rateKiller() {
        Player.Rating rating = killerPlayer.getRating();

        if (Player.Rating.UNRATED.equals(rating)) {
            killerPlayer.setRating(Player.Rating.THUMBS_UP);
        } else if (Player.Rating.THUMBS_UP.equals(rating)) {
            killerPlayer.setRating(Player.Rating.THUMBS_DOWN);
        } else {
            killerPlayer.setRating(Player.Rating.UNRATED);
        }
        dataService.notifyChange();
        updateRating();
    }


    public void updateKillerCharacter(String killerCharacter) {
        if (this.killerCharacter == null || !this.killerCharacter.equals(killerCharacter)) {
            this.killerCharacter = killerCharacter;
            titleBarCharacterLabel.setText("(as " + killerCharacter + ")");
            characterValueLabel.setText(killerCharacter);
            firePropertyChange(EVENT_NEW_KILLER_CHARACTER, null, killerCharacter);
        }
    }

}
