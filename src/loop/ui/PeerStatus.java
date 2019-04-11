package loop.ui;

import loop.Constants;
import loop.io.peer.IOPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static loop.io.peer.IOPeer.Rating;

/**
 * Represents a status bar component for a single connection.
 *
 * @author NickyRamone
 */
public class PeerStatus extends JPanel {

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
    private IOPeer hostUser;


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
        hostUser = new IOPeer();
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
                    separator2Label.setVisible(true);
                    descriptionLabel.setVisible(false);
                    editField.setVisible(true);
                    editField.requestFocusInWindow();
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
                        String profileUrl = Constants.STEAM_PROFILE_URL_PREFIX + hostUser.getUID();
                        Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                    } catch (IOException e1) {
                        System.err.println("Failed to open browser at Steam profile.");
                        e1.printStackTrace();
                    } catch (URISyntaxException e1) {
                        System.err.println("Failed to open browser at Steam profile.");
                        e1.printStackTrace();
                    }
                }
            }
        });
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
        editField = new JTextField(hostUser.getDescription());
        editField.setCaretColor(Color.WHITE);
        editField.setCaretPosition(hostUser.getDescription().length());
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
                    editField.setVisible(false);
                    descriptionLabel.setVisible(true);
                    separator2Label.setVisible(!lastDescription.isEmpty());
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
            editField.setVisible(false);
            descriptionLabel.setVisible(true);
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
        boolean userDetected = !hostUser.getUID().isEmpty();
        boolean hasDescription = !hostUser.getDescription().isEmpty();

        separator1Label.setVisible(userDetected);
        steamLabel.setVisible(userDetected);
        userNameLabel.setVisible(userDetected);

        if (!editingDescription) {
            separator2Label.setVisible(hasDescription);
            descriptionLabel.setVisible(hasDescription);
            descriptionLabel.setText(hostUser.getDescription());
        }

        if (userDetected) {
            userNameLabel.setText(hostUser.getMostRecentName());

            Rating peerRating = hostUser.getRating();

            if (peerRating == Rating.THUMBS_DOWN) {
                if (!ratingLabel.isVisible()) {
                    ratingLabel.setVisible(true);
                }
                ratingLabel.setIcon(ResourceFactory.getThumbsDownIcon());
            } else if (peerRating == Rating.THUMBS_UP) {
                if (!ratingLabel.isVisible()) {
                    ratingLabel.setVisible(true);
                }
                ratingLabel.setIcon(ResourceFactory.getThumbsUpIcon());
            } else {
                ratingLabel.setIcon(null);

                if (ratingLabel.isVisible()) {
                    ratingLabel.setVisible(false);
                }
            }
        }
    }

    public void rateHostUser() {
        Rating rating = hostUser.getRating();

        if (Rating.UNRATED.equals(rating)) {
            hostUser.setRating(Rating.THUMBS_UP);
        } else if (Rating.THUMBS_DOWN.equals(rating)) {
            hostUser.setRating(Rating.THUMBS_DOWN);
        } else {
            hostUser.setRating(Rating.UNRATED);
        }
    }

    public void setHostUser(IOPeer hostUser) {
        this.hostUser = hostUser;
        editField.setText(hostUser.getDescription());
        lastDescription = hostUser.getDescription();
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

}
