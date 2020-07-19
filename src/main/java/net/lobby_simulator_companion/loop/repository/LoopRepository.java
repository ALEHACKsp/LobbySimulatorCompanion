package net.lobby_simulator_companion.loop.repository;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.domain.LoopData;
import net.lobby_simulator_companion.loop.util.FileUtil;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File-based repository for Loop-data storage.
 *
 * @author NickyRamone, ShadowMoose
 */
@Slf4j
public class LoopRepository {

    private static final byte[] CIPHER_KEY_MATERIAL = new byte[]{2, 3, -57, 11, 73, 57, -66, 21};
    private static final String PROPERTY__READ_ENCRYPTED = "storage.read.encrypted";
    private static final String PROPERTY__WRITE_ENCRYPTED = "storage.write.encrypted";

    private AppProperties properties;
    private File saveFile;
    private final Gson gson;
    private final String jsonIndent;


    public LoopRepository(AppProperties properties, Gson gson) {
        this.properties = properties;
        this.gson = gson;
        saveFile = Paths.get(properties.get("app.home")).resolve(properties.get("storage.file")).toFile();

        if (properties.getBoolean(PROPERTY__WRITE_ENCRYPTED)) {
            jsonIndent = "";
        } else {
            jsonIndent = "    ";
        }
    }

    public LoopData load() throws IOException {
        log.info("Loading data...");
        LoopData loopData;

        try {
            Instant loadStartTime = Instant.now();
            Cipher cipher = getCipher(true);
            JsonReader reader = createJsonReader(saveFile, cipher);
            loopData = gson.fromJson(reader, LoopData.class);
            reader.close();
            Duration elapsed = Duration.between(loadStartTime, Instant.now());
            log.info("Loaded data ({} players; {} matches) in {} ms.",
                    loopData.getPlayers().size(),
                    loopData.getMatchLog().matchCount(),
                    elapsed.toMillis());

        } catch (FileNotFoundException e1) {
            throw e1;
        } catch (Exception e2) {
            throw new IOException("Failed to load data. File corrupt?", e2);
        }

        return loopData;
    }

    public void save(LoopData loopData) throws IOException {
        log.debug("Saving data ({} players)...", loopData.getPlayers().size());
        Instant saveStartTime = Instant.now();
        if (this.saveFile.exists()) {
            FileUtil.saveFile(this.saveFile, "");
        }

        JsonWriter writer = createJsonWriter();
        writer.setIndent(jsonIndent);
        gson.toJson(loopData, LoopData.class, writer);
        writer.close();
        Duration elapsed = Duration.between(saveStartTime, Instant.now());
        log.debug("Saved data ({} players; {} matches) in {} ms.",
                loopData.getPlayers().size(),
                loopData.getMatchLog().matchCount(),
                elapsed.toMillis());
    }


    private JsonReader createJsonReader(File file, Cipher cipher) throws IOException {
        InputStream inputStream;

        if (properties.getBoolean(PROPERTY__READ_ENCRYPTED)) {
            CipherInputStream decStream;
            FileInputStream fis = new FileInputStream(file);
            try {
                decStream = new CipherInputStream(fis, cipher);
            } catch (Exception e) {
                log.error("Failed to create encrypted stream.", e);
                throw new IOException(e.getMessage());
            }
            inputStream = new GZIPInputStream(decStream);
        } else {
            inputStream = new FileInputStream(file);
        }

        return gson.newJsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }


    private JsonWriter createJsonWriter() throws IOException {
        OutputStream outputStream;

        if (properties.getBoolean(PROPERTY__WRITE_ENCRYPTED)) {
            Cipher cipher;
            try {
                cipher = getCipher(false);
            } catch (Exception e) {
                log.error("Failed to configure encryption.", e);
                throw new IOException(e.getMessage());
            }
            outputStream = new GZIPOutputStream(new CipherOutputStream(new FileOutputStream(saveFile), cipher));
        } else {
            outputStream = new FileOutputStream(this.saveFile.getAbsolutePath());
        }

        return gson.newJsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }


    /**
     * Builds the Cipher Object used for encryption/decryption.
     *
     * @param readMode The Cipher will be initiated in either encrypt/decrypt mode.
     * @return Cipher argument, ready to go.
     * @throws Exception Many possible issues can arise, so this is a catch-all.
     */
    public static Cipher getCipher(boolean readMode) throws Exception {

        DESKeySpec key = new DESKeySpec(CIPHER_KEY_MATERIAL);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey desKey = keyFactory.generateSecret(key);
        Cipher cipher = Cipher.getInstance("DES");

        if (readMode) {
            cipher.init(Cipher.DECRYPT_MODE, desKey);
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
        }
        return cipher;
    }

}
