package mlga.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.swing.*;

import mlga.Boot;
import mlga.SteamUser;
import mlga.io.DbdSteamLogMonitor;
import mlga.io.Settings;
import mlga.io.peer.IOPeer;
import mlga.io.peer.PeerTracker;

public class Overlay extends JPanel implements Observer {
    private static final long serialVersionUID = -470849574354121503L;

    private DbdSteamLogMonitor logMonitor;
    private PeerTracker peerTracker;

    private JFrame frame;
    private JLabel defaultStatus;
    private PeerStatus.PeerStatusListener peerStatusListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private boolean frameMove = false;

    private final Map<Inet4Address, PeerStatus> peerStatuses = new ConcurrentHashMap<>();
    private Long timeSinceWeHaveOnlyOnePeer;
    private static final int MIN_HOST_DETECTION_MS = 10000;

    private static final int PEER_TIMEOUT_MS = 5000;
    private static final int CLEANER_POLL_MS = 2500;


    public Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException, IOException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        defineListeners();
        createDefaultStatus();

        peerTracker = new PeerTracker();

        setOpaque(false);
        frame = new JFrame();
        // since this frame will be always on top, having the focus state in true, will make other windows unclickable
        frame.setFocusableWindowState(false);
        frame.setUndecorated(true);
        frame.setBackground(Color.BLACK);
        frame.setLayout(new GridLayout(0, 1));
        frame.add(defaultStatus);
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

    private void createDefaultStatus() {
        defaultStatus = new JLabel("    No players - Join a lobby?    ");
        defaultStatus.setFont(ResourceFactory.getRobotoFont());
        defaultStatus.setBackground(new Color(0, 0, 0, 255));
        defaultStatus.setForeground(Color.RED);
        defaultStatus.setOpaque(true);
        defaultStatus.addMouseListener(mouseListener);
        defaultStatus.addMouseMotionListener(mouseMotionListener);
    }


    /**
     * Sets a peer's ping, or creates their object.
     */
    public void setPing(Inet4Address id, long ping) {
        frame.remove(defaultStatus);
        PeerStatus peerStatus = peerStatuses.get(id);

        if (peerStatus == null) {
            System.out.print(System.currentTimeMillis() / 1000 + ": ");
            System.out.println("New ping address: " + id);
            addPeerStatus(id, ping);
        } else {
            Peer peerDto = peerStatus.getPeerDto();
            peerDto.setPing(ping);
            peerStatus.update();
        }
    }

    private void addPeerStatus(Inet4Address addr, long rtt) {
        Peer peer = new Peer(addr, rtt);
        PeerStatus peerStatus = new PeerStatus(peer, peerStatusListener);
        peerStatus.addMouseListener(mouseListener);
        peerStatus.addMouseMotionListener(mouseMotionListener);
        peerStatuses.put(addr, peerStatus);
        timeSinceWeHaveOnlyOnePeer = peerStatuses.size() == 1 ? System.currentTimeMillis() : null;
        System.out.println("timeSinceWeHaveOnlyOnePeer: " + ( (timeSinceWeHaveOnlyOnePeer != null)? (timeSinceWeHaveOnlyOnePeer / 1000): "null") );
        updateSteamUserIfNecessary();
        frame.add(peerStatus);
        frame.pack();
        frame.revalidate();
        frame.repaint();
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
                Collection<Map.Entry<Inet4Address, PeerStatus>> entriesToRemove =
                        peerStatuses.entrySet().stream()
                                .filter(e -> e.getValue().getPeerDto().age() >= PEER_TIMEOUT_MS)
                                .collect(Collectors.toList());

                for (Map.Entry<Inet4Address, PeerStatus> entryToRemove : entriesToRemove) {
                    Boot.active.remove(entryToRemove.getKey());
                    PeerStatus peerStatus = peerStatuses.remove(entryToRemove.getKey());
                    frame.remove(peerStatus);

                    if (peerStatuses.isEmpty()) {
                        frame.add(defaultStatus);
                    } else if (peerStatuses.size() == 1) {
                        timeSinceWeHaveOnlyOnePeer = System.currentTimeMillis();
                    }

                    updateSteamUserIfNecessary();
                    frame.pack();
                    frame.revalidate();
                    frame.repaint();
                }
            }
        }, 0, CLEANER_POLL_MS);

    }


    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DbdSteamLogMonitor) {
            updateSteamUserIfNecessary();
        }
    }


    private void updateSteamUserIfNecessary() {
        if (timeSinceWeHaveOnlyOnePeer == null) {
            return;
        }

        long timeWithOnlyOnePeer = System.currentTimeMillis() - timeSinceWeHaveOnlyOnePeer;
        System.out.println("time with one peer: " + timeWithOnlyOnePeer / 1000);

        if (timeWithOnlyOnePeer >= MIN_HOST_DETECTION_MS && logMonitor.getLastSteamUserFound() != null) {
            /**
             * Important: At this point, we have to be careful of the following situation:
             * It can happen that even when we have a single connection, that connection is not with the lobby host
             * (because there are other communication packets being sent between other peers).
             * As more seconds pass, we have a higher probability of knowing that the single connection is with
             * the lobby host.
             *
             * So, if we decided to clear the username right after assigning it, we might end up losing it if it was
             * assigned to a random peer that was not the lobby host.
             */
            PeerStatus peerStatus = peerStatuses.entrySet().iterator().next().getValue();
            SteamUser steamUser = logMonitor.getLastSteamUserFound();

            System.out.println("Attaching steam name {" + logMonitor.getLastSteamUserFound().getName()
                    + "} to peer status: " + peerStatus.getPeerDto().getID() + " / " + peerStatus.getPeerDto().getSteamName());

            String steamId = steamUser.getId();
            IOPeer ioPeer = peerStatus.getPeerDto().getPeerData();
            ioPeer.setUID(steamId);

            IOPeer storedPeer = peerTracker.getPeerBySteamId(steamId);
            if (storedPeer == null) {
                System.out.println("storedPeer not found: creating new one");
                ioPeer.addName(steamUser.getName());
                peerTracker.addPeer(steamId, ioPeer);
            } else {
                System.out.println("storedPeer found");
                storedPeer.addName(steamUser.getName());
                peerStatus.getPeerDto().setPeerData(storedPeer);
                peerStatus.notifyDataUpdated();
            }

            peerStatus.update();
        }
    }

//    private synchronized void updateSteamUserIfNecessary() {
//
//        if (peerStatuses.size() == 1 && logMonitor.getLastSteamUserFound() != null) {
//            /**
//             * Important: At this point, we have to be careful of the following situation:
//             * It can happen that even when we have a single connection, that connection is not with the lobby host
//             * (because there are other commmunication packets being sent between other peers).
//             * As more seconds pass, we have a higher probability of knowing that the single connection is with
//             * the lobby host.
//             *
//             * So, if we decided to clear the username right after assigning it, we might end up losing it if it was
//             * assigned to a random peer that was not the lobby host.
//             */
//            PeerStatus peerStatus = peerStatuses.entrySet().iterator().next().getValue();
//            SteamUser steamUser = logMonitor.getLastSteamUserFound();
//
//            System.out.println("Attaching steam name {" + logMonitor.getLastSteamUserFound().getName()
//                    + "} to peer status: " + peerStatus.getPeerDto().getID() + " / " + peerStatus.getPeerDto().getSteamName());
//
//            String steamId = steamUser.getId();
//            IOPeer ioPeer = peerStatus.getPeerDto().getPeerData();
//            ioPeer.setUID(steamId);
//
//            IOPeer storedPeer = peerTracker.getPeerBySteamId(steamId);
//            if (storedPeer == null) {
//                System.out.println("storedPeer not found: creating new one");
//                ioPeer.addName(steamUser.getName());
//                peerTracker.addPeer(steamId, ioPeer);
//            } else {
//                System.out.println("storedPeer found");
//                storedPeer.addName(steamUser.getName());
//                peerStatus.getPeerDto().setPeerData(storedPeer);
//                peerStatus.notifyDataUpdated();
//            }
//
//            peerStatus.update();
//        }
//    }


}
