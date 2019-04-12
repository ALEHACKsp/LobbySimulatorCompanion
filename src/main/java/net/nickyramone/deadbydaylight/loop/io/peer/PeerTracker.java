package net.nickyramone.deadbydaylight.loop.io.peer;

import net.nickyramone.deadbydaylight.loop.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.nickyramone.deadbydaylight.loop.io.peer.IOPeer.Rating;

/**
 * Manages saved peers.
 *
 * @author ShadowMoose
 */
public class PeerTracker {
    private static final Logger logger = LoggerFactory.getLogger(PeerTracker.class);
    private static final File peerFile = FileUtil.getLoopPath().resolve("loop.hosts.dat").toFile();
    private Map<String, IOPeer> peers = new ConcurrentHashMap<>();

    /**
     * Creates a PeerTracker, which instantly loads the Peer List into memory.
     */
    public PeerTracker() {
        if (!peerFile.exists()) {
            savePeers();
            return;
        }

        // PeerSavers create emergency backups, so loop to check primary file, then attempt fallback if needed.
        for (int i = 0; i <= 1; i++) {
            try {

                if (peerFile.length() > 0) {
                    PeerReader ps = new PeerReader(FileUtil.getSaveName(peerFile, i));
                    while (ps.hasNext()) {
                        IOPeer ioPeer = ps.next();
                        if (!ioPeer.getRating().equals(Rating.UNRATED) || !ioPeer.getDescription().isEmpty()) {
                            peers.put(ioPeer.getUID(), ioPeer);
                        }
                    }
                    logger.info("Loaded {} tracked users.", peers.size());

                    if (i != 0) {
                        // If we had to check a backup, re-save the backup as the primary instantly.
                        savePeers();
                    }
                }
                break;
            } catch (Exception e) {
                if (i == 0) {
                    logger.error("No Peers file located! Checking backups!", e);
                }
            }
        }
    }

    /**
     * Attempts to save the list of IOPeers.  <br>
     * Should be called whenever a Peer's information is updated.  <br><br>
     * Since the Peer List is static between all instances of PeerTracker, this method may be called by anything.
     *
     * @return True if this save works. May not work if a save is already underway.
     */
    public synchronized boolean savePeers() {
        List<IOPeer> peersToSave = peers.values().stream().filter(p -> shouldSavePeer(p)).collect(Collectors.toList());
        logger.info("Saving {} host(s)...", peersToSave.size());
        // Flag that the save file is busy, to avoid thread shenanigans.
        try {
            PeerSaver ps = new PeerSaver(peerFile);
            ps.save(peersToSave);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save hosts data.", e);
        }
        return false;
    }

    private boolean shouldSavePeer(IOPeer peer) {
        return !peer.getUID().isEmpty() &&
                (!peer.getRating().equals(Rating.UNRATED) || !peer.getDescription().isEmpty());
    }

    public IOPeer getPeerBySteamId(String steamId) {
        return peers.get(steamId);
    }


    public void addPeer(String steamId, IOPeer ioPeer) {
        peers.put(steamId, ioPeer);
    }

}
