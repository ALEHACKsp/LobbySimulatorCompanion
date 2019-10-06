package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.SteamUser;

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
import java.util.Observable;
import java.util.Observer;

public class KillerPanel extends JPanel {

    private static final class DocumentSizeFilter extends DocumentFilter {
        private int maxChars;


        public DocumentSizeFilter(int maxChars) {
            this.maxChars = maxChars;
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {

            if (fb.getDocument().getLength() + text.length() <= maxChars) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }


    private static final Font font = ResourceFactory.getRobotoFont();

    private Window window;
    private JPanel summaryBar;
    private JLabel summaryValueLabel;
    private JLabel detailCollapseButton;
    private JPanel detailsPanel;
    private JLabel characterValueLabel;
    private JLabel matchCountValueLabel;
    private JPanel userDescriptionPanel;
    private JTextArea userNotesArea;
    private SteamUser killerUser;


    public KillerPanel(Window window) {
        this.window = window;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBackground(new Color(0, 0, 200));
        summaryBar = createSummaryBar();
        detailsPanel = createDetailsPanel();

        add(summaryBar);
        add(detailsPanel);
    }

    private JPanel createSummaryBar() {
        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel summaryLabel = new JLabel("Killer:");
        summaryLabel.setBorder(border);
        summaryLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        summaryLabel.setFont(font);
        summaryValueLabel = new JLabel("N/A");
        summaryValueLabel.setBorder(border);
        summaryValueLabel.setFont(font);

        detailCollapseButton = new JLabel();
        detailCollapseButton.setIcon(ResourceFactory.getCollapseIcon());

        detailCollapseButton.setBorder(border);
        detailCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailsPanel.setVisible(!detailsPanel.isVisible());
            }
        });

        container.add(summaryLabel);
        container.add(summaryValueLabel);
        container.add(Box.createHorizontalGlue());
        container.add(detailCollapseButton);

        return container;
    }

    private JPanel createDetailsPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel statsContainer = new JPanel();
        statsContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        statsContainer.setLayout(new GridLayout(3, 2, 10, 10));

        JLabel characterLabel = new JLabel("Character:", JLabel.RIGHT);
        characterLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        characterLabel.setFont(font);
        characterValueLabel = new JLabel("Hillbilly");
        characterValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        characterValueLabel.setFont(font);

        JLabel matchCountLabel = new JLabel("Matches played against:", JLabel.RIGHT);
        matchCountLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        matchCountLabel.setFont(font);
        matchCountValueLabel = new JLabel("5");
        matchCountValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        matchCountValueLabel.setFont(font);

        JLabel notesLabel = new JLabel("Your notes (below):", JLabel.RIGHT);
        notesLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        notesLabel.setFont(font);

        statsContainer.add(characterLabel);
        statsContainer.add(characterValueLabel);
        statsContainer.add(matchCountLabel);
        statsContainer.add(matchCountValueLabel);
        statsContainer.add(notesLabel);

        container.add(statsContainer);
        container.add(Box.createVerticalStrut(10));
        container.add(createUserDescriptionPanel());

        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                detailCollapseButton.setIcon(ResourceFactory.getCollapseIcon());
                window.pack();
                super.componentShown(e);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                detailCollapseButton.setIcon(ResourceFactory.getExpandIcon());
                window.pack();
                super.componentHidden(e);
            }
        });


        return container;
    }

    private JPanel createUserDescriptionPanel() {
        String description = "" +
                "nurse (1): noed, 7 blinks\n" +
                "billy (5): high curve\n" +
                "Tryhard. Sucks ass. Moonwalks and talks shit.\n" +
                "Gave me hatch.";

        JPanel container = new JPanel();
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));

        userNotesArea = new JTextArea(description, 10, 30);
        ((AbstractDocument) userNotesArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(1256));
        userNotesArea.setMargin(new Insets(5, 5, 5, 5));
        userNotesArea.setForeground(Color.WHITE);
        userNotesArea.setLineWrap(true);
        userNotesArea.setWrapStyleWord(true);
        userNotesArea.setFont(font);
        userNotesArea.setBackground(new Color(0x85, 0x74, 0xbf));
        userNotesArea.setEditable(true);

        JScrollPane descAreacontainer = new JScrollPane(userNotesArea);

        container.add(Box.createHorizontalGlue());
        container.add(descAreacontainer);

        return container;
    }


    public void updateKillerUser(SteamUser killerUser) {
        this.killerUser = killerUser;
        summaryValueLabel.setText(killerUser.getName());
    }

    public void clearKillerInfo() {
        killerUser = null;
        characterValueLabel.setText("");
        summaryValueLabel.setText("");
        matchCountValueLabel.setText("");
        userNotesArea.setText("");
    }

    private void collapseDetails() {
        if (detailsPanel.isVisible()) {
            detailsPanel.setVisible(false);
        }
    }

    public void updateKillerCharacter(String killerCharacter) {
        characterValueLabel.setText(killerCharacter);
    }

}
