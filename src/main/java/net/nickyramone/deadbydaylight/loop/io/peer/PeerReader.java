package net.nickyramone.deadbydaylight.loop.io.peer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.nickyramone.deadbydaylight.loop.io.Settings;
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
    public IOPeer next() throws IOException {
        if (reader.hasNext()) {
            IOPeer peer = gson.fromJson(reader, IOPeer.class);
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
        if (!Settings.ENCRYPT_STORED_DATA) {
            return new InputStreamReader(new FileInputStream(f), "UTF-8");
        }

        CipherInputStream decStream = null;
        FileInputStream fis = new FileInputStream(f);
        try {
            decStream = new CipherInputStream(fis, Security.getCipher(true));
        } catch (Exception e) {
            logger.error("Failed to create encrypted stream.", e);
            throw new IOException();
        }
        return new InputStreamReader(new GZIPInputStream(decStream), "UTF-8");
    }
}
