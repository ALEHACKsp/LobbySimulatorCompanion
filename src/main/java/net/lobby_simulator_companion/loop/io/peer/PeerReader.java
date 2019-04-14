package net.lobby_simulator_companion.loop.io.peer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.CipherInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Simple class to handle reading IOPeer objects from the given file.  <br>
 * Handles decryption as needed on the file.
 *
 * @author ShadowMoose
 */
@Deprecated
public class PeerReader {
    private static final Logger logger = LoggerFactory.getLogger(PeerReader.class);

    private JsonReader reader;
    private Gson gson;

    /**
     * Builds a Peer Reader for the given File.
     */
    public PeerReader(File f) throws IOException {
        if (!f.createNewFile()) {
            this.gson = new Gson();
            reader = new JsonReader(open(f));
            reader.beginArray();
        }
    }

    /**
     * Gets the next unread IOPeer in the list, or null if none remain.
     */
    public Player next() throws IOException {
        if (reader.hasNext()) {
            Player peer = gson.fromJson(reader, Player.class);
            return peer;
        }
        return null;
    }

    /**
     * If this PeerStream has more IOPeers left to read.
     */
    public boolean hasNext() {
        try {
            if (reader == null) {
                return false;
            } else if (!reader.hasNext()) {
                this.close();
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.error("Encountered a problem while reading the host info file.", e);
        }
        return false;
    }

    public void close() throws IOException {
        reader.endArray();
        reader.close();
    }

    private InputStreamReader open(File f) throws IOException {
        if ("0".equals(Settings.get("encrypt"))) {
            return new InputStreamReader(new FileInputStream(f), "UTF-8");
        }

        CipherInputStream decStream;
        FileInputStream fis = new FileInputStream(f);
        try {
            decStream = new CipherInputStream(fis, Security.getCipher_macBased(true));
        } catch (Exception e) {
            logger.error("Failed to create encrypted stream.", e);
            throw new IOException();
        }
        return new InputStreamReader(new GZIPInputStream(decStream), "UTF-8");
    }
}
