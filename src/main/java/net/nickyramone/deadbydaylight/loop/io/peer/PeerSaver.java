package net.nickyramone.deadbydaylight.loop.io.peer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.nickyramone.deadbydaylight.loop.io.FileUtil;
import net.nickyramone.deadbydaylight.loop.io.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Class for saving lists of IOPeers to JSON format.  <br>
 * Handles output File encryption natively within the class as needed.
 *
 * @author ShadowMoose
 */
public class PeerSaver {

    private static final Logger logger = LoggerFactory.getLogger(PeerSaver.class);

    private final File saveFile;

    private final Gson gson;

    private final String jsonIndent;

    /**
     * Builds a new {@link #PeerSaver(File)} Object, which handles encryption and saving
     * IOPeer lists to the given file.
     *
     * @param save The file to use for saving.
     */
    public PeerSaver(File save) {
        this.saveFile = save;

        GsonBuilder gsonBuilder = new GsonBuilder();

        if (!Settings.ENCRYPT_STORED_DATA) {
            gsonBuilder.setPrettyPrinting();
            jsonIndent = "    ";
        } else {
            jsonIndent = "";
        }

        gson = gsonBuilder.create();
    }

    /**
     * Saves the given list of Peers to this Saver's file.  <br>
     * Automatically creates a backup file first if a save already exists.
     *
     * @param peers The list to save.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void save(List<IOPeer> peers) throws FileNotFoundException, IOException {
        // Keep a rolling backup of the Peers file, for safety.
        if (this.saveFile.exists()) {
            FileUtil.saveFile(this.saveFile, "");
        }

        if (Settings.ENCRYPT_STORED_DATA) {
            savePeers(openEncryptedStream(this.saveFile), peers);
        } else {
            savePeers(new FileOutputStream(this.saveFile.getAbsolutePath()), peers);
        }
    }


    /**
     * This method handles opening an OutputStream to the given file.
     *
     * @param f The file to open.
     * @return The stream opened to the desired file.
     * @throws IOException
     */
    private OutputStream openEncryptedStream(File f) throws IOException {
        Cipher c;
        try {
            c = Security.getCipher(false);
        } catch (Exception e) {
            logger.error("Failed to configure encryption.", e);
            throw new IOException();
        }
        return new GZIPOutputStream(new CipherOutputStream(new FileOutputStream(f), c));
    }

    private void savePeers(OutputStream out, List<IOPeer> peers) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent(jsonIndent);
        writer.beginArray();
        peers.forEach(p -> gson.toJson(p, IOPeer.class, writer));
        writer.endArray();
        writer.close();
    }


}
