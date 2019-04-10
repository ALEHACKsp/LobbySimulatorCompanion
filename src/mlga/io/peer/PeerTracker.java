package mlga.io.peer;

import mlga.io.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static mlga.io.peer.IOPeer.Status;

/**
 * Manages saved peers.
 *
 * @author ShadowMoose
 */
public class PeerTracker {
    private static final File peerFile = new File(FileUtil.getMlgaPath() + "peers.mlga");
    private static boolean saving = false;
    private Map<String, IOPeer> peers = new HashMap<>();

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
                        if (!ioPeer.getStatus().equals(IOPeer.Status.UNRATED) || !ioPeer.getDescription().isEmpty()) {
                            peers.put(ioPeer.getUID(), ioPeer);
                        }
                    }
                    System.out.println("Loaded " + peers.size() + " tracked users!");

                    if (i != 0) // If we had to check a backup, re-save the backup as the primary instantly.
                        savePeers();
                }
                break;
            } catch (Exception e) {
                e.printStackTrace();
                if (i == 0)
                    System.err.println("No Peers file located! Checking backups!");
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
    public boolean savePeers() {
        if (saving) {
            // This type of check is less than ideal,
            // but if save is being called at the same time, the first instance should still save all listed IOPeers.
            System.err.println("Peer File is busy!");
            return false;
        }
        List<IOPeer> peersToSave = peers.values().stream().filter(p -> shouldSavePeer(p)).collect(Collectors.toList());
        System.out.println("Saving peers!");
        // Flag that the save file is busy, to avoid thread shenanigans.
        saving = true;
        try {
            PeerSaver ps = new PeerSaver(peerFile);
            ps.save(peersToSave);
            saving = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        saving = false;
        return false;
    }

    private boolean shouldSavePeer(IOPeer peer) {
        return !peer.getUID().isEmpty() &&
                (!peer.getStatus().equals(Status.UNRATED) || !peer.getDescription().isEmpty());
    }

    public IOPeer getPeerBySteamId(String steamId) {
        return peers.get(steamId);
    }


    public void addPeer(String steamId, IOPeer ioPeer) {
        peers.put(steamId, ioPeer);
    }

}
