package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Boot;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.DbdSteamLogMonitor;
import net.lobby_simulator_companion.loop.service.Player;
import net.lobby_simulator_companion.loop.service.PlayerService;
import net.lobby_simulator_companion.loop.service.SteamUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Inet4Address;
import java.util.Timer;
import java.util.*;
import java.util.stream.Collectors;

public class Overlay extends JPanel implements Observer {
    private static final long serialVersionUID = -470849574354121503L;
    private static final Logger logger = LoggerFactory.getLogger(Overlay.class);

    private DbdSteamLogMonitor logMonitor;
    private final PlayerService playerService;

    private JFrame frame;
    private JLabel emptyStatus;
    private PeerStatus.PeerStatusListener peerStatusListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private boolean frameMove = false;

    private Map<Inet4Address, Long> connections = new HashMap<>(); // keeps track of the last time we received ping for each IP
    private PeerStatus peerStatus;
    private boolean connected; // true if we are connected to any peer; otherwise, false
    private Queue<Player> lobbyHosts = new LinkedList<>();

    private static final int PEER_TIMEOUT_MS = 5000;
    private static final int CLEANER_POLL_MS = 2500;


    public Overlay(PlayerService playerService, DbdSteamLogMonitor logMonitor) throws Exception {
        this.playerService = playerService;
        this.logMonitor = logMonitor;
        logMonitor.addObserver(this);

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        defineListeners();
        createEmptyStatus();

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

        startConnectionCleaner();
        startLogMonitor();
    }

    private void defineListeners() {
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() && e.getClickCount() >= 2) {
                    frameMove = !frameMove;
                    Settings.set("frame_x", frame.getLocationOnScreen().x);
                    Settings.set("frame_y", frame.getLocationOnScreen().y);
                }
            }
        };

        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (frameMove && !e.isShiftDown()) {
                    frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
                }
            }
        };

        peerStatusListener = new PeerStatus.PeerStatusListener() {
            @Override
            public void startEdit() {
                frame.setMinimumSize(new Dimension(500, 0));
                frame.setFocusableWindowState(true);
                peerStatus.update();
                frame.pack();
                peerStatus.focusOnEditField();
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
                playerService.save();
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
            updateLobbyHost();
        }
        connections.put(ip, System.currentTimeMillis());

        if (!peerStatus.hasHostUser() && !lobbyHosts.isEmpty()) {
            peerStatus.setHostUser(lobbyHosts.poll());
        }

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


    private void startLogMonitor() {
        Thread thread = new Thread(logMonitor);
        thread.setDaemon(true);
        thread.start();
    }

    private void startConnectionCleaner() {
        Timer cleanTime = new Timer();
        cleanTime.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanConnections();
            }
        }, 0, CLEANER_POLL_MS);
    }


    private void cleanConnections() {
        synchronized (connections) {
            long currentTime = System.currentTimeMillis();
            Set<Inet4Address> connectionsToRemove = connections.entrySet().stream()
                    .filter(e -> currentTime - e.getValue() >= PEER_TIMEOUT_MS)
                    .map(e -> e.getKey()).collect(Collectors.toSet());

            for (Inet4Address connectionToRemove : connectionsToRemove) {
                Boot.active.remove(connectionToRemove);
                connections.remove(connectionToRemove);
            }

            int connectionCount = connections.size();

            if (connected && connectionCount == 0) {
                clearUserHostStatus();
            }
//            else if (connectionCount == 1 && !connectionsToRemove.isEmpty() && !lobbyHosts.isEmpty()) {
////                updateLobbyHost();
////                updateSteamUserIfNecessary();
//                Player lobbyHost = lobbyHosts.poll();
//                peerStatus.setHostUser(lobbyHost);
//                peerStatus.update();
//
//            }
        }
        frame.pack();
        frame.revalidate();
        frame.repaint();
    }



    private void clearUserHostStatus() {
        logger.debug("Clearing user host status...");
        frame.remove(peerStatus);
        peerStatus.setHostUser(null);
        peerStatus.update();
        frame.add(emptyStatus);
        connected = false;
    }


    @Override
    public void update(Observable o, Object obj) {
        if (o instanceof DbdSteamLogMonitor) {
            SteamUser steamUser = (SteamUser) obj;
            String steamId = steamUser.getId();
            String steamName = steamUser.getName();
            Player player = playerService.getPlayerBySteamId(steamId);

            if (player == null) {
                logger.debug("User of id {} not found in the storage. Creating new entry...", steamId);
                player = new Player();
                player.setUID(steamId);
                player.setRating(Player.Rating.UNRATED);
                player.setDescription("");
                player.addName(steamUser.getName());
                playerService.addPlayer(steamId, player);
            } else {
                logger.debug("User of id {} found in the storage. Adding name '{}' to the existing entry...", steamId, steamName);
                player.addName(steamName);
            }

            lobbyHosts.add(player);
            updateLobbyHost();
        }
    }

    private synchronized void updateLobbyHost() {
        if (connected && !peerStatus.hasHostUser()) {
            Player lobbyHost = lobbyHosts.poll();
            peerStatus.setHostUser(lobbyHost);
            peerStatus.update();
        }
    }

}
