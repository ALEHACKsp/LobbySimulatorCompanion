package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.Player;
import net.lobby_simulator_companion.loop.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Represents a status bar component for a single connection.
 *
 * @author NickyRamone
 */
public class PeerStatus extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PeerStatus.class);

    private static final int MAX_DESCRIPTION_LEN = 50;
    private static final Font font = ResourceFactory.getRobotoFont();

    private JLabel pingLabel;
    private JLabel separator1Label;
    private JLabel steamLabel;
    private JLabel userNameLabel;
    private JLabel ratingLabel;
    private JLabel separator2Label;
    private JLabel descriptionLabel;
    private JTextField editField;
    private PeerStatusListener listener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;

    private int ping;
    private Player hostUser;


    /**
     * We store the previous description in case the user cancels the input by pressing Esc.
     */
    private String lastDescription;
    private boolean editingDescription;


    /**
     * Ugly approach for adjusting UI parent containers.
     */
    public interface PeerStatusListener {

        void startEdit();

        void finishEdit();

        void updated();

        void peerDataChanged();

    }


    public PeerStatus(PeerStatusListener listener) {
        this.listener = listener;

        setBackground(new Color(0, 0, 0, 255));
        defineListeners();
        createComponents();
        addComponents();
    }

    private void createComponents() {
        createSeparatorLabels();
        createPingLabel();
        createSteamLabel();
        createUserNameLabel();
        createRatingLabel();
        createDescriptionLabel();
        createDescriptionEditField();
    }

    private void addComponents() {
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gridConstrs = new GridBagConstraints();
        gridConstrs.gridy = 0;

        gridConstrs.gridx = 0;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 10);
        layout.setConstraints(pingLabel, gridConstrs);

        gridConstrs.gridx = 1;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(separator1Label, gridConstrs);

        gridConstrs.gridx = 2;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(steamLabel, gridConstrs);

        gridConstrs.gridx = 3;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(userNameLabel, gridConstrs);

        gridConstrs.gridx = 4;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(ratingLabel, gridConstrs);

        gridConstrs.gridx = 5;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(separator2Label, gridConstrs);

        gridConstrs.gridx = 6;
        gridConstrs.weightx = 0;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 5, 0, 5);
        layout.setConstraints(descriptionLabel, gridConstrs);

        gridConstrs.gridx = 7;
        gridConstrs.weightx = 1;
        gridConstrs.anchor = GridBagConstraints.WEST;
        gridConstrs.insets = new Insets(0, 0, 0, 0);
        layout.setConstraints(editField, gridConstrs);

        add(pingLabel);
        add(separator1Label);
        add(steamLabel);
        add(userNameLabel);
        add(ratingLabel);
        add(separator2Label);
        add(descriptionLabel);
        add(editField);
    }

    private void defineListeners() {
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean peerIsIdentified = !hostUser.getUID().isEmpty();

                if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown() && peerIsIdentified) {
                    rateHostUser();
                    update();
                    listener.peerDataChanged();
                } else if (SwingUtilities.isRightMouseButton(e) && peerIsIdentified) {
                    // user wants to edit the description
                    editingDescription = true;
                    setVisible(separator2Label, true);
                    setVisible(descriptionLabel, false);
                    setVisible(editField, true);
                    listener.startEdit();
                } else {
                    for (MouseListener listener : getMouseListeners()) {
                        if (listener != this) {
                            listener.mouseClicked(e);
                        }
                    }
                }
            }
        };

        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                for (MouseMotionListener listener : getMouseMotionListeners()) {
                    if (listener != this) {
                        listener.mouseDragged(e);
                    }
                }
            }
        };
    }


    private void createSeparatorLabels() {
        separator1Label = new JLabel("|");
        separator1Label.setFont(font);
        separator1Label.setBackground(Color.BLACK);
        separator1Label.setForeground(Color.WHITE);
        separator1Label.setOpaque(true);
        separator1Label.addMouseListener(mouseListener);
        separator1Label.addMouseMotionListener(mouseMotionListener);

        separator2Label = new JLabel("|");
        separator2Label.setFont(font);
        separator2Label.setBackground(Color.BLACK);
        separator2Label.setForeground(Color.WHITE);
        separator2Label.setOpaque(true);
        separator2Label.addMouseListener(mouseListener);
        separator2Label.addMouseMotionListener(mouseMotionListener);
    }

    private void createPingLabel() {
        pingLabel = new JLabel();
        pingLabel.setFont(font);
        pingLabel.setBackground(Color.BLACK);
        pingLabel.setOpaque(true);
        pingLabel.addMouseListener(mouseListener);
        pingLabel.addMouseMotionListener(mouseMotionListener);
    }

    private void createSteamLabel() {
        steamLabel = new JLabel();
        steamLabel.setIcon(ResourceFactory.getSteamIcon());
        steamLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) {
                    try {
                        String profileUrl = Factory.getAppProperties().get("steam.profile_url_prefix") + hostUser.getUID();
                        Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                    } catch (IOException e1) {
                        logger.error("Failed to open browser at Steam profile.");
                    } catch (URISyntaxException e1) {
                        logger.error("Attempted to use an invalid URL for the Steam profile.");
                    }
                }
            }
        });
        steamLabel.addMouseListener(mouseListener);
        steamLabel.addMouseMotionListener(mouseMotionListener);
    }

    private void createUserNameLabel() {
        userNameLabel = new JLabel();
        userNameLabel.setFont(font);
        userNameLabel.setBackground(Color.BLACK);
        userNameLabel.setForeground(new Color(0, 100, 200));
        userNameLabel.setOpaque(true);
        userNameLabel.addMouseListener(mouseListener);
        userNameLabel.addMouseMotionListener(mouseMotionListener);
        userNameLabel.setVisible(false);
    }

    private void createRatingLabel() {
        ratingLabel = new JLabel();
        ratingLabel.addMouseListener(mouseListener);
        ratingLabel.addMouseMotionListener(mouseMotionListener);
    }

    private void createDescriptionLabel() {
        descriptionLabel = new JLabel();
        descriptionLabel.setFont(font);
        descriptionLabel.setBackground(Color.BLACK);
        descriptionLabel.setForeground(new Color(0, 100, 200));
        descriptionLabel.setOpaque(true);
        descriptionLabel.setVisible(true);
        descriptionLabel.addMouseListener(mouseListener);
        descriptionLabel.addMouseMotionListener(mouseMotionListener);
        descriptionLabel.setVisible(false);
    }

    private void createDescriptionEditField() {
        String description = hostUser != null ? hostUser.getDescription() : "";

        editField = new JTextField(description);
        editField.setCaretColor(Color.WHITE);
        editField.setCaretPosition(description.length());
        editField.setBackground(Color.BLACK);
        editField.setForeground(Color.WHITE);
        editField.setColumns(30);
        editField.setFont(font);
        editField.setVisible(false);
        editField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    editField.setText(lastDescription);
                    setVisible(editField, false);
                    setVisible(descriptionLabel, true);
                    setVisible(separator2Label, !lastDescription.isEmpty());
                    listener.finishEdit();
                    editingDescription = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (editField.getText().length() >= MAX_DESCRIPTION_LEN) {
                    e.consume();
                }
            }
        });
        editField.addActionListener(e -> {
            lastDescription = editField.getText();
            hostUser.setDescription(lastDescription);
            descriptionLabel.setText(lastDescription);
            setVisible(editField, false);
            setVisible(descriptionLabel, true);
            editingDescription = false;
            update();
            listener.finishEdit();
            listener.peerDataChanged();
        });
    }

    public void update() {
        updatePing();
        updateUserInfo();
        revalidate();
        repaint();
        listener.updated();
    }

    private void updatePing() {
        int ping = this.ping;
        Color color;

        if (ping < 0) {
            color = Color.WHITE;
        } else if (ping <= 140) {
            color = Color.GREEN;
        } else if (ping > 140 && ping <= 190) {
            color = Color.YELLOW;
        } else {
            color = Color.RED;
        }
        pingLabel.setForeground(color);
        pingLabel.setText((ping >= 0 ? ping : " ? ") + " ms");
    }


    private void updateUserInfo() {
        boolean userDetected = hostUser != null;
        boolean hasDescription = userDetected && !hostUser.getDescription().isEmpty();

        if (userDetected && !editingDescription) {
            descriptionLabel.setText(hostUser.getDescription());
        }

        if (userDetected) {
            userNameLabel.setText(hostUser.getMostRecentName());
            Player.Rating peerRating = hostUser.getRating();

            if (peerRating == Player.Rating.THUMBS_DOWN) {
                ratingLabel.setIcon(ResourceFactory.getThumbsDownIcon());
            } else if (peerRating == Player.Rating.THUMBS_UP) {
                ratingLabel.setIcon(ResourceFactory.getThumbsUpIcon());
            } else {
                ratingLabel.setIcon(null);
            }
        }

        setVisible(separator1Label, userDetected);
        setVisible(steamLabel, userDetected);
        setVisible(userNameLabel, userDetected);
        setVisible(ratingLabel, userDetected && Player.Rating.UNRATED != hostUser.getRating());
        setVisible(separator2Label, hasDescription);
        setVisible(descriptionLabel, hasDescription);
    }

    public void rateHostUser() {
        Player.Rating rating = hostUser.getRating();

        if (Player.Rating.UNRATED.equals(rating)) {
            hostUser.setRating(Player.Rating.THUMBS_UP);
        } else if (Player.Rating.THUMBS_UP.equals(rating)) {
            hostUser.setRating(Player.Rating.THUMBS_DOWN);
        } else {
            hostUser.setRating(Player.Rating.UNRATED);
        }
    }

    public void setHostUser(Player hostUser) {
        this.hostUser = hostUser;
        String description = hostUser != null ? hostUser.getDescription() : "";
        editField.setText(description);
        lastDescription = description;
    }

    public boolean hasHostUser() {
        return hostUser != null;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    private void setVisible(Component component, boolean visible) {
        if (component.isVisible() != visible) {
            component.setVisible(visible);
        }
    }

    public void focusOnEditField() {
        editField.requestFocusInWindow();
    }

}
