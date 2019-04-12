package net.nickyramone.deadbydaylight.loop.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.*;
import java.util.Timer;
import java.util.stream.Collectors;

import javax.swing.*;

import net.nickyramone.deadbydaylight.loop.Boot;
import net.nickyramone.deadbydaylight.loop.SteamUser;
import net.nickyramone.deadbydaylight.loop.io.DbdSteamLogMonitor;
import net.nickyramone.deadbydaylight.loop.io.Settings;
import net.nickyramone.deadbydaylight.loop.io.peer.IOPeer;
import net.nickyramone.deadbydaylight.loop.io.peer.PeerTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Overlay extends JPanel implements Observer {
    private static final long serialVersionUID = -470849574354121503L;

    private static final Logger logger = LoggerFactory.getLogger(Overlay.class);

    private DbdSteamLogMonitor logMonitor;
    private PeerTracker peerTracker;

    private JFrame frame;
    private JLabel emptyStatus;
    private PeerStatus.PeerStatusListener peerStatusListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private boolean frameMove = false;

    private Map<Inet4Address, Long> connections = new HashMap<>(); // keeps track of the last time we received ping for each IP
    private PeerStatus peerStatus;
    private boolean connected; // true if we are connected to any peer; otherwise, false

    private static final int PEER_TIMEOUT_MS = 5000;
    private static final int CLEANER_POLL_MS = 2500;


    public Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException, IOException {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        defineListeners();
        createEmptyStatus();

        peerTracker = new PeerTracker();

        setOpaque(false);
        frame = new JFrame();

        /** Since this frame will be always on top, having the focus state in true
         * will make other windows unclickable so we need to set it to false. */
        frame.setFocusableWindowState(false);
        frame.setUndecorated(true);
        frame.setBackground(Color.BLACK);
        frame.setLayout(new GridLayout(0, 1));
        frame.add(emptyStatus);
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocation((int) Settings.getDouble("frame_x", 5), (int) Settings.getDouble("frame_y", 400));
        frame.setVisible(true);

        startCleanerTask();
        startLogMonitor();
    }

    private void defineListeners() {
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2) {
                    frameMove = !frameMove;
                    Settings.set("frame_x", frame.getLocationOnScreen().x);
                    Settings.set("frame_y", frame.getLocationOnScreen().y);
                }
            }
        };

        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (frameMove) {
                    frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
                }
            }
        };

        peerStatusListener = new PeerStatus.PeerStatusListener() {
            @Override
            public void startEdit() {
                frame.setMinimumSize(new Dimension(500, 0));
                frame.setFocusableWindowState(true);
                frame.pack();
            }

            @Override
            public void finishEdit() {
                frame.setFocusableWindowState(false);
                frame.setMinimumSize(new Dimension(0, 0));
                frame.pack();
            }

            @Override
            public void updated() {
                frame.pack();
                revalidate();
                repaint();
            }

            @Override
            public void peerDataChanged() {
                peerTracker.savePeers();
            }
        };
    }

    private void createEmptyStatus() {
        emptyStatus = new JLabel("    No players - Join a lobby?    ");
        emptyStatus.setFont(ResourceFactory.getRobotoFont());
        emptyStatus.setBackground(new Color(0, 0, 0, 255));
        emptyStatus.setForeground(Color.RED);
        emptyStatus.setOpaque(true);
        emptyStatus.addMouseListener(mouseListener);
        emptyStatus.addMouseMotionListener(mouseMotionListener);

        peerStatus = new PeerStatus(peerStatusListener);
        peerStatus.addMouseListener(mouseListener);
        peerStatus.addMouseMotionListener(mouseMotionListener);
    }


    /**
     * Sets a peer's ping, or creates their object.
     */
    public void setPing(Inet4Address ip, int ping) {
        if (!connected) {
            frame.remove(emptyStatus);
            frame.add(peerStatus);
            connected = true;
            updateSteamUserIfNecessary();
        }

        connections.put(ip, System.currentTimeMillis());
        ping = connections.size() == 1 ? ping : -1;
        peerStatus.setPing(ping);
        peerStatus.update();
    }


    /**
     * Dispose this Overlay's Window.
     */
    public void close() {
        this.frame.dispose();
    }


    private void startLogMonitor() throws IOException {
        logMonitor = new DbdSteamLogMonitor(this);
        Thread thread = new Thread(logMonitor);
        thread.setDaemon(true);
        thread.start();
    }

    private void startCleanerTask() {
        Timer cleanTime = new Timer();
        cleanTime.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                Set<Inet4Address> connectionsToRemove = connections.entrySet().stream()
                        .filter(e -> currentTime - e.getValue() >= PEER_TIMEOUT_MS)
                        .map(e -> e.getKey()).collect(Collectors.toSet());

                for (Inet4Address connectionToRemove : connectionsToRemove) {
                    Boot.active.remove(connectionToRemove);
                    connections.remove(connectionToRemove);
                }

                if (connected && connections.isEmpty()) {
                    clearUserHostStatus();
                }
                frame.pack();
                frame.revalidate();
                frame.repaint();
            }
        }, 0, CLEANER_POLL_MS);
    }

    private void clearUserHostStatus() {
        frame.remove(peerStatus);
        peerStatus.setHostUser(new IOPeer());
        peerStatus.update();
        logMonitor.clearUser();
        frame.add(emptyStatus);
        connected = false;
    }


    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DbdSteamLogMonitor) {
            updateSteamUserIfNecessary();
        }
    }


    private synchronized void updateSteamUserIfNecessary() {

        if (connected && logMonitor.getLastSteamUserFound() != null) {
            SteamUser steamUser = logMonitor.getLastSteamUserFound();
            String steamId = steamUser.getId();
            String steamName = steamUser.getName();
            IOPeer storedHost = peerTracker.getPeerBySteamId(steamId);

            if (storedHost == null) {
                logger.debug("User of id {} not found in the storage. Creating new entry...", steamId);
                storedHost = new IOPeer();
                storedHost.setUID(steamId);
                storedHost.setRating(IOPeer.Rating.UNRATED);
                storedHost.setDescription("");
                storedHost.addName(steamUser.getName());
                peerTracker.addPeer(steamId, storedHost);
            } else {
                logger.debug("User of id {} found in the storage. Adding name '{}' to the existing entry.", steamId, steamName);
                storedHost.addName(steamName);
            }
            peerStatus.setHostUser(storedHost);
            peerStatus.update();
        }
    }

}
