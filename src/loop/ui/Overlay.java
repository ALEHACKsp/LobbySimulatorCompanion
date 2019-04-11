package loop.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.*;
import java.util.Timer;
import java.util.stream.Collectors;

import javax.swing.*;

import loop.Boot;
import loop.SteamUser;
import loop.io.DbdSteamLogMonitor;
import loop.io.Settings;
import loop.io.peer.IOPeer;
import loop.io.peer.PeerTracker;

public class Overlay extends JPanel implements Observer {
    private static final long serialVersionUID = -470849574354121503L;

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

//        revalidate();
//        repaint();
//        if (peerStatus == null) {
////            frame.remove(emptyStatus);
//            empty
//            addPeerStatus(ping);
//        } else {
//            ping = connections.size() == 1? ping: -1;
//            peerStatus.setPing(ping);
//            peerStatus.update();
//        }
    }

    private void addPeerStatus(int ping) {
//        Peer peer = new Peer(addr, rtt);
//        peerStatus = new PeerStatus(peerStatusListener);
//        peerStatus.addMouseListener(mouseListener);
//        peerStatus.addMouseMotionListener(mouseMotionListener);
//        peerStatus.setPing(ping);
//        peerStatuses.put(addr, peerStatus);
//        updateSteamUserIfNecessary();
//        frame.add(peerStatus);
//        frame.pack();
//        frame.revalidate();
//        frame.repaint();
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


    private void updateSteamUserIfNecessary() {

        if (connected && logMonitor.getLastSteamUserFound() != null) {
            SteamUser steamUser = logMonitor.getLastSteamUserFound();
            String steamId = steamUser.getId();
            IOPeer ioPeer = peerStatus.getHostUser();
            ioPeer.setUID(steamId);

            IOPeer storedPeer = peerTracker.getPeerBySteamId(steamId);
            if (storedPeer == null) {
                System.out.println("storedPeer not found: creating new one");

                if (!ioPeer.getMostRecentName().isEmpty()) {
                    // we are replacing the current host
                    ioPeer.setStatus(IOPeer.Status.UNRATED);
                    ioPeer.setDescription("");
                    ioPeer.clearNames();
                }

                ioPeer.addName(steamUser.getName());
                peerTracker.addPeer(steamId, ioPeer);
            } else {
                System.out.println("storedPeer found: " + storedPeer);
                storedPeer.addName(steamUser.getName());
                peerStatus.setHostUser(storedPeer);
            }

            peerStatus.update();
        }
    }

}
