package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.repository.SteamProfileDao;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.service.PlayerDto;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.Colors;
import net.lobby_simulator_companion.loop.ui.common.FontUtil;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
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

import static net.lobby_simulator_companion.loop.ui.common.ComponentUtils.NO_BORDER;
import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;


/**
 * @author NickyRamone
 */
public class KillerPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";
    public static final String EVENT_KILLER_UPDATE = "killer.update";

    /**
     * When the user updates the killer player description, the change is not applied immediately
     * (we don't want to do an update for every single character), so we defer the write with
     * a specific delay.
     */
    private static final int DESCRIPTION_UPDATE_DELAY_MS = 5000;
    private static final int MAX_KILLER_DESCRIPTION_SIZE = 1256;
    private static final char DEFAULT_NON_DISPLAYABLE_CHAR = '\u0387';
    private static final String MSG_KILLER_CHARACTER = "(as %s)";
    private static final String BLACKED_OUT_KILLER_CHAR = "************";
    private static final Logger logger = LoggerFactory.getLogger(KillerPanel.class);
    private static final Font font = ResourceFactory.getRobotoFont();

    private enum InfoType {
        KILLER_CHARACTER("Character"),
        KILLER_PLAYER_NAMES("Previous names seen"),
        TIMES_ENCOUNTERED("Times encountered"),
        MATCHES_AGAINST_PLAYER("Matches played against"),
        TIME_PLAYED_AGAINST("Total time played against"),
        ESCAPES_AGAINST("Total escapes against"),
        DEATHS_BY("Times died against"),
        NOTES("Your notes");

        String description;

        InfoType(String description) {
            this.description = description;
        }
    }

    private Settings settings;
    private LoopDataService dataService;
    private SteamProfileDao steamProfileDao;

    private JPanel titleBar;
    private JLabel playerNameLabel;
    private JLabel playerSteamButton;
    private JLabel titleBarCharacterLabel;
    private JLabel playerRateLabel;
    private JPanel detailsPanel;
    private NameValueInfoPanel<InfoType> statsContainer;
    private JPanel charValuePanel;
    private JLabel characterValueLabel;
    private JScrollPane userNotesPane;
    private JLabel userNotesEditButton;
    private JTextArea userNotesArea;

    private Player killerPlayer;
    private Player killerPlayerBackup;
    private Killer killerCharacter = Killer.UNIDENTIFIED;
    private Timer userNotesUpdateTimer;
    private boolean showCharacter;


    public KillerPanel(Settings settings, LoopDataService dataService, SteamProfileDao steamProfileDao) {
        this.settings = settings;
        this.dataService = dataService;
        this.steamProfileDao = steamProfileDao;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        titleBar = createTitleBar();
        detailsPanel = createDetailsPanel();

        JPanel collapsablePanel = new CollapsablePanel(titleBar, detailsPanel,
                settings, "ui.panel.killer.collapsed");
        collapsablePanel.addPropertyChangeListener(evt -> firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null));
        add(collapsablePanel);
    }

    private JPanel createTitleBar() {

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel summaryLabel = new JLabel("Killer:");
        summaryLabel.setBorder(border);
        summaryLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
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
                if (killerPlayer != null) {
                    try {
                        String profileUrl = Factory.getAppProperties().get("steam.profile_url_prefix") + killerPlayer.getSteamId64();
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

        return container;
    }

    private JPanel createDetailsPanel() {

        JLabel characterLabel = new JLabel("Character:", JLabel.RIGHT);
        characterLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        characterLabel.setFont(font);

        characterValueLabel = new JLabel();
        characterValueLabel.setBorder(NO_BORDER);
        characterValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        characterValueLabel.setFont(font);

        showCharacter = settings.getBoolean("ui.panel.killer.character.show", false);

        JLabel hideCharButton = new JLabel();
        hideCharButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hideCharButton.setBorder(new EmptyBorder(0, 10, 0, 0));
        ImageIcon icon = showCharacter ? ResourceFactory.getIcon(Icon.HIDE) : ResourceFactory.getIcon(Icon.SHOW);
        hideCharButton.setIcon(icon);
        hideCharButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCharacter = !showCharacter;
                settings.set("ui.panel.killer.character.show", showCharacter);
                ImageIcon icon = showCharacter ? ResourceFactory.getIcon(Icon.HIDE) : ResourceFactory.getIcon(Icon.SHOW);
                hideCharButton.setIcon(icon);
                titleBarCharacterLabel.setVisible(showCharacter);

                if (showCharacter) {
                    if (killerCharacter.isIdentified()) {
                        characterValueLabel.setText(killerCharacter.alias());
                    }
                } else {
                    characterValueLabel.setText(BLACKED_OUT_KILLER_CHAR);
                }

                firePropertyChange(EVENT_KILLER_UPDATE, null, null);
            }
        });

        charValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        charValuePanel.setBackground(Colors.INFO_PANEL_BACKGROUND);
        charValuePanel.add(characterValueLabel);
        charValuePanel.add(hideCharButton);
        charValuePanel.setVisible(false);

        JLabel notesLabel = new JLabel("Your notes:", JLabel.RIGHT);
        notesLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
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

        NameValueInfoPanel.Builder<InfoType> builder = new NameValueInfoPanel.Builder<>();
        if (settings.getExperimentalSwitch(2)) {
            builder.addField(InfoType.KILLER_CHARACTER,
                    InfoType.KILLER_CHARACTER.description + ":", charValuePanel);
        }
        if (settings.getExperimentalSwitch(1)) {
            builder.addField(InfoType.KILLER_PLAYER_NAMES,
                    InfoType.KILLER_PLAYER_NAMES.description + ":");
        }
        builder.addField(InfoType.TIMES_ENCOUNTERED, InfoType.TIMES_ENCOUNTERED.description + ":");
        builder.addField(InfoType.MATCHES_AGAINST_PLAYER, InfoType.MATCHES_AGAINST_PLAYER.description + ":");
        builder.addField(InfoType.ESCAPES_AGAINST, InfoType.ESCAPES_AGAINST.description + ":");
        builder.addField(InfoType.DEATHS_BY, InfoType.DEATHS_BY.description + ":");
        builder.addField(InfoType.TIME_PLAYED_AGAINST, InfoType.TIME_PLAYED_AGAINST.description + ":");
        builder.addField(InfoType.NOTES, InfoType.NOTES.description + ":", userNotesEditButton);
        statsContainer = builder.build();

        JPanel container = new JPanel();
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
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
                    if (killerPlayer != null) {
                        String notes = userNotesArea.getText().trim();
                        notes = notes.isEmpty() ? null : notes;
                        killerPlayer.setDescription(notes);
                        userNotesUpdateTimer = null;
                        dataService.notifyChange();
                        firePropertyChange(EVENT_KILLER_UPDATE, null, null);
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
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(Box.createHorizontalGlue());
        container.add(userNotesPane);

        return container;
    }


    public void receiveNewKillerPlayer(PlayerDto playerDto) {
        String playerName;
        try {
            playerName = steamProfileDao.getPlayerName(playerDto.getSteamId());
            playerName = FontUtil.replaceNonDisplayableChars(font, playerName, DEFAULT_NON_DISPLAYABLE_CHAR);
        } catch (IOException e) {
            logger.error("Failed to retrieve player's name for steam id#{}.", playerDto.getSteamId());
            playerName = "";
        }
        String steamId = playerDto.getSteamId();
        Player storedPlayer = dataService.getPlayerBySteamId(steamId);
        final Player player;

        if (storedPlayer == null) {
            logger.debug("User of id {} not found in the storage. Creating new entry...", steamId);
            player = new Player();
            player.setSteamId64(steamId);
            player.setDbdPlayerId(playerDto.getDbdId());
            player.addName(playerName);
            player.incrementTimesEncountered();
            dataService.addPlayer(steamId, player);
        } else {
            logger.debug("User '{}' (id '{}') found in the storage. Updating entry...", playerName, steamId);
            player = storedPlayer;
            player.updateLastSeen();
            player.addName(playerName);
            player.incrementTimesEncountered();
            dataService.notifyChange();
        }

        // the player info is received from an app thread, so we need to push it to the UI thread (EDT)
        SwingUtilities.invokeLater(() -> updateKillerPlayer(player));
    }

    public void clearKillerInfo() {
        killerPlayer = null;
        killerCharacter = Killer.UNIDENTIFIED;
        playerNameLabel.setText(null);
        playerSteamButton.setVisible(false);
        if (settings.getExperimentalSwitch(1)) {
            statsContainer.get(InfoType.KILLER_PLAYER_NAMES).setText(null);
            statsContainer.get(InfoType.KILLER_PLAYER_NAMES).setToolTipText(null);
        }
        titleBarCharacterLabel.setText(null);
        playerRateLabel.setIcon(null);
        statsContainer.get(InfoType.TIMES_ENCOUNTERED).setText(null);
        characterValueLabel.setText(null);
        charValuePanel.setVisible(false);
        statsContainer.get(InfoType.MATCHES_AGAINST_PLAYER).setText(null);
        statsContainer.get(InfoType.ESCAPES_AGAINST).setText(null);
        statsContainer.get(InfoType.DEATHS_BY).setText(null);
        statsContainer.get(InfoType.TIME_PLAYED_AGAINST).setText(null);
        userNotesEditButton.setVisible(false);
        userNotesArea.setText("");
        toggleUserNotesAreaVisibility(false);
    }

    private void updateKillerPlayer(Player player) {
        killerPlayer = player;

        playerNameLabel.setText(player.getMostRecentName() != null ? player.getMostRecentName() : "");
        playerSteamButton.setVisible(true);

        if (settings.getExperimentalSwitch(1)) {
            JLabel otherNamesValueLabel = statsContainer.get(InfoType.KILLER_PLAYER_NAMES);
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
        }

        statsContainer.get(InfoType.TIMES_ENCOUNTERED).setText(String.valueOf(player.getTimesEncountered()));
        statsContainer.get(InfoType.MATCHES_AGAINST_PLAYER).setText(String.valueOf(player.getMatchesPlayed()));
        statsContainer.get(InfoType.ESCAPES_AGAINST).setText(String.valueOf(player.getEscapesAgainst()));
        statsContainer.get(InfoType.DEATHS_BY).setText(String.valueOf(player.getDeathsBy()));
        statsContainer.get(InfoType.TIME_PLAYED_AGAINST).setText(TimeUtil.formatTimeUpToHours(player.getSecondsPlayed()));

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

        firePropertyChange(EVENT_KILLER_UPDATE, null, null);
    }

    private void toggleUserNotesAreaVisibility(boolean visible) {
        userNotesPane.setVisible(visible);
        firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
    }

    private void updateRating() {
        if (killerPlayer == null) {
            return;
        }

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
        firePropertyChange(EVENT_KILLER_UPDATE, null, null);
    }


    public void updateKillerCharacter(Killer killer) {
        if (!this.killerCharacter.isIdentified() || !this.killerCharacter.equals(killer)) {
            this.killerCharacter = killer;

            if (showCharacter) {
                titleBarCharacterLabel.setText(String.format(MSG_KILLER_CHARACTER, killer.alias()));
                characterValueLabel.setText(killer.alias());
            } else {
                characterValueLabel.setText(BLACKED_OUT_KILLER_CHAR);
            }
            charValuePanel.setVisible(true);
            firePropertyChange(EVENT_KILLER_UPDATE, null, null);
        }
    }

    public Player getKillerPlayer() {
        return killerPlayer;
    }

    public Killer getKillerCharacter() {
        return killerCharacter;
    }

    public boolean isShowKillerCharacter() {
        return showCharacter;
    }

    public void notifyEndOfMatch(int matchTime) {
        if (killerPlayer != null) {
            killerPlayer.incrementMatchesPlayed();
            statsContainer.get(InfoType.MATCHES_AGAINST_PLAYER).setText(String.valueOf(killerPlayer.getMatchesPlayed()));
        }

        if (killerPlayer != null) {
            killerPlayer.incrementSecondsPlayed(matchTime);
            statsContainer.get(InfoType.TIME_PLAYED_AGAINST).setText(TimeUtil.formatTimeUpToHours(killerPlayer.getSecondsPlayed()));
        }

        dataService.notifyChange();
        backupKillerPlayer();
    }

    public void backupKillerPlayer() {
        if (killerPlayer != null) {
            killerPlayerBackup = killerPlayer.clone();
        }
    }

    public void restoreKillerPlayer() {
        if (killerPlayer != null && killerPlayerBackup != null) {
            killerPlayer.copyFrom(killerPlayerBackup);
            dataService.notifyChange();
            updateKillerPlayer(killerPlayer);
        }
    }


    public void notifySurvivalAgainstCurrentKiller(Boolean escaped) {
        if (killerPlayer == null || escaped == null) {
            return;
        }

        if (escaped) {
            killerPlayer.incrementEscapes();
        } else {
            killerPlayer.incrementDeaths();
        }
        dataService.notifyChange();
        updateKillerPlayer(killerPlayer);
    }

}
