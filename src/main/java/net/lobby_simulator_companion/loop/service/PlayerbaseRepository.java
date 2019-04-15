package net.lobby_simulator_companion.loop.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.io.peer.Security;
import net.lobby_simulator_companion.loop.util.FileUtil;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.io.peer.PeerReader;
import org.pcap4j.core.PcapNetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File-based repository for the playerbase (user-provided data about hosts).
 *
 * @author NickyRamone, ShadowMoose
 */
public class PlayerbaseRepository {

    private static final Logger logger = LoggerFactory.getLogger(PlayerbaseRepository.class);

    private File saveFile;

    private final Gson gson;

    private final String jsonIndent;


    public PlayerbaseRepository() {
        AppProperties appProperties = Factory.getAppProperties();
        saveFile = Paths.get(appProperties.get("app.home"))
                .resolve(appProperties.get("storage.playerbase.file")).toFile();
        GsonBuilder gsonBuilder = new GsonBuilder();

        if ("0".equals(Settings.get("encrypt"))) {
            gsonBuilder.setPrettyPrinting();
            jsonIndent = "    ";
        } else {
            jsonIndent = "";
        }

        gson = gsonBuilder.create();
    }


    public Playerbase load() throws FileNotFoundException {
        // TODO: Use a single method after the first stable release
        Playerbase playerbase;

        try {
            logger.info("Reading data using legacy beta cipher...");
            playerbase = load_legacyBeta();

            // if it succeeds, we immediately save it with the new format
            save(playerbase);
        } catch (Exception e) {
            // mac-based cipher failed. Attempt with the new one
            logger.info("Legacy beta cipher failed. Attempting with latest cipher...");
            try {
                Cipher cipher = Security.getCipher(true);
                JsonReader reader = createJsonReader(cipher);
                playerbase = gson.fromJson(reader, Playerbase.class);
            }
            catch (FileNotFoundException e2) {
                throw e2;
            } catch (Exception e3) {
                throw new RuntimeException("Failed to load playerbase. Data file corrupt?", e3);
            }
        }

        logger.info("Loaded {} tracked players.", playerbase.getPlayers().size());

        return playerbase;
    }


    private Playerbase load_legacyBeta() throws IOException {
        PeerReader peerReader = new PeerReader(saveFile);
        List<Player> peers = new ArrayList<>();

        while (peerReader.hasNext()) {
            Player ioPeer = peerReader.next();
            if (!ioPeer.getRating().equals(Player.Rating.UNRATED) || !ioPeer.getDescription().isEmpty()) {
                peers.add(ioPeer);
            }
        }

        return new Playerbase(peers);
    }


    public void save(Playerbase playerbase) throws IOException {
        // Keep a rolling backup of the Peers file, for safety.
        logger.info("Saving playerbase of {} players...", playerbase.getPlayers().size());
        if (this.saveFile.exists()) {
            FileUtil.saveFile(this.saveFile, "");
        }

        JsonWriter writer = createJsonWriter();
        writer.setIndent(jsonIndent);
        gson.toJson(playerbase, Playerbase.class, writer);
        writer.close();
    }


    private JsonReader createJsonReader(Cipher cipher) throws IOException {
        InputStream inputStream;

        if ("0".equals(Settings.get("encrypt"))) {
            inputStream = new FileInputStream(saveFile);
        } else {
            CipherInputStream decStream;
            FileInputStream fis = new FileInputStream(saveFile);
            try {
                decStream = new CipherInputStream(fis, cipher);
            } catch (Exception e) {
                logger.error("Failed to create encrypted stream.", e);
                throw new IOException(e.getMessage());
            }
            inputStream = new GZIPInputStream(decStream);
        }

        return new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
    }


    private JsonWriter createJsonWriter() throws IOException {
        OutputStream outputStream;

        if ("0".equals(Settings.get("encrypt"))) {
            outputStream = new FileOutputStream(this.saveFile.getAbsolutePath());
        } else {
            Cipher c;
            try {
                c = Security.getCipher(false);
            } catch (Exception e) {
                logger.error("Failed to configure encryption.", e);
                throw new IOException(e.getMessage());
            }
            outputStream = new GZIPOutputStream(new CipherOutputStream(new FileOutputStream(saveFile), c));
        }

        return new JsonWriter(new OutputStreamWriter(outputStream, "UTF-8"));
    }

    // TODO: get rid of this
    public void setNetworkInterface(PcapNetworkInterface nif) {
        Security.nif = nif;
    }
}
