package mlga.ui;

import mlga.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static mlga.io.peer.IOPeer.Status;

/**
 * Represents a status bar component for a single connection.
 *
 * @author NickyRamone
 */
public class PeerStatus extends JPanel {

    private static int MAX_DESCRIPTION_LEN = 50;

    private static Font font = ResourceFactory.getRobotoFont();

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

    private Peer peer;

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


    public PeerStatus(Peer peer, PeerStatusListener listener) {
        this.peer = peer;
        this.lastDescription = peer.getDescription();
        this.listener = listener;

        setBackground(new Color(0, 0, 0, 255));
        defineListeners();
        createComponents();
        addComponents();

        update();
        listener.updated();
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
        gridConstrs.fill = GridBagConstraints.HORIZONTAL;
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
                boolean peerIsIdentified = !peer.getPeerData().getUID().isEmpty();

                if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown() && peerIsIdentified) {
                    peer.rate();
                    update();
                    listener.peerDataChanged();
                } else if (SwingUtilities.isRightMouseButton(e) && peerIsIdentified) {
                    // user wants to edit getDescription
                    editingDescription = true;
                    descriptionLabel.setVisible(false);
                    editField.setVisible(true);
                    listener.startEdit();
                    editField.requestFocusInWindow();
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
                        String profileUrl = Constants.STEAM_PROFILE_URL_PREFIX + getPeerDto().getPeerData().getUID();
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
        editField = new JTextField(peer.getDescription());
        editField.setCaretPosition(peer.getDescription().length());
        editField.setCaretColor(Color.WHITE);
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
                    separator2Label.setVisible(true);
                    listener.finishEdit();
                    editingDescription = false;
                    revalidate();
                    repaint();
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
            peer.setDescription(lastDescription);
            descriptionLabel.setText(lastDescription);
            editField.setVisible(false);
            descriptionLabel.setVisible(true);
            update();
            editingDescription = false;
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
        long ping = peer.getPing();
        Color color;
        if (ping <= 140) {
            color = Color.GREEN;
        } else if (ping > 140 && ping <= 190) {
            color = Color.YELLOW;
        } else {
            color = Color.RED;
        }
        pingLabel.setForeground(color);
//        pingLabel.setText(ping + " ms");
        pingLabel.setText(ping + " (" + peer.getID() + ")");
    }


    private void updateUserInfo() {
        boolean userDetected = !peer.getPeerData().getUID().isEmpty();
        boolean hasDescription = !peer.getDescription().isEmpty();

        separator1Label.setVisible(userDetected);
        steamLabel.setVisible(userDetected);
        userNameLabel.setVisible(userDetected);
        separator2Label.setVisible(hasDescription);

        if (!editingDescription) {
            descriptionLabel.setVisible(hasDescription);
            descriptionLabel.setText(peer.getDescription());
        }

        if (userDetected) {
            userNameLabel.setText(peer.getSteamName());

            Status peerRating = peer.getPeerData().getStatus();

            if (peerRating == Status.BLOCKED) {
                ratingLabel.setIcon(ResourceFactory.getThumbsDownIcon());
            } else if (peerRating == Status.LOVED) {
                ratingLabel.setIcon(ResourceFactory.getThumbsUpIcon());
            } else {
                ratingLabel.setIcon(null);
            }
        }
    }


    public Peer getPeerDto() {
        return peer;
    }

    public void notifyDataUpdated() {
        editField.setText(peer.getDescription());
        lastDescription = peer.getDescription();
    }


}
