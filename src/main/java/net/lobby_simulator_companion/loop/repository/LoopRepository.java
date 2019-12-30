package net.lobby_simulator_companion.loop.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.LoopData;
import net.lobby_simulator_companion.loop.domain.Stats;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File-based repository for Loop-data storage.
 *
 * @author NickyRamone, ShadowMoose
 */
public class LoopRepository {

    private static final Logger logger = LoggerFactory.getLogger(LoopRepository.class);
    private static final byte[] CIPHER_KEY_MATERIAL = new byte[]{2, 3, -57, 11, 73, 57, -66, 21};
    private static final String PROPERTY__READ_ENCRYPTED = "storage.read.encrypted";
    private static final String PROPERTY__WRITE_ENCRYPTED = "storage.write.encrypted";

    private AppProperties properties;
    private File saveFile;
    private File legacySaveFile;
    private final Gson gson;
    private final String jsonIndent;


    public LoopRepository(AppProperties properties) {
        this.properties = properties;
        saveFile = Paths.get(properties.get("app.home")).resolve(properties.get("storage.file")).toFile();
        legacySaveFile = Paths.get(properties.get("app.home")).resolve(properties.get("storage.file.legacy")).toFile();
        GsonBuilder gsonBuilder = new GsonBuilder();

        if (properties.getBoolean(PROPERTY__WRITE_ENCRYPTED)) {
            jsonIndent = "";
        } else {
            gsonBuilder.setPrettyPrinting();
            jsonIndent = "    ";
        }

        configureSerializers(gsonBuilder);
        gson = gsonBuilder.create();
    }

    private void configureSerializers(GsonBuilder gsonBuilder) {

        JsonSerializer<LocalDateTime> dateTimeSerializer = (o, type, jsonSerializationContext) ->
                new JsonPrimitive(o.toEpochSecond(OffsetDateTime.now().getOffset()));
        JsonDeserializer<LocalDateTime> dateTimeDeserializer = (jsonElement, type, jsonDeserializationContext) ->
                LocalDateTime.ofEpochSecond(jsonElement.getAsLong(), 0, OffsetDateTime.now().getOffset());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, dateTimeSerializer);
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, dateTimeDeserializer);

        // Gson serializes map keys using toString()
        JsonDeserializer<Killer> killerDeserializer = (jsonElement, type, context) ->
                Killer.valueOf(jsonElement.getAsString().toUpperCase());
        gsonBuilder.registerTypeAdapter(Killer.class, killerDeserializer);

        gsonBuilder.registerTypeAdapter(Stats.class, new Stats.Serializer());
        gsonBuilder.registerTypeAdapter(Stats.class, new Stats.Deserializer());
    }


    public LoopData load() throws IOException {
        logger.info("Loading data...");
        LoopData loopData;

        try {
            Cipher cipher = getCipher(true);
            File dataFile = saveFile.exists() ? saveFile : legacySaveFile;
            JsonReader reader = createJsonReader(dataFile, cipher);
            loopData = gson.fromJson(reader, LoopData.class);
            reader.close();
        } catch (FileNotFoundException e1) {
            throw e1;
        } catch (Exception e2) {
            throw new IOException("Failed to load data. File corrupt?", e2);
        }
        logger.info("Loaded {} players.",
                loopData.getPlayers().size());

        return loopData;
    }


    public void save(LoopData loopData) throws IOException {
        // Keep a rolling backup of the Peers file, for safety.
        logger.debug("Saving data ({} players)...",
                loopData.getPlayers().size());

        if (this.saveFile.exists()) {
            FileUtil.saveFile(this.saveFile, "");
        }

        JsonWriter writer = createJsonWriter();
        writer.setIndent(jsonIndent);
        gson.toJson(loopData, LoopData.class, writer);
        writer.close();
    }


    private JsonReader createJsonReader(File file, Cipher cipher) throws IOException {
        InputStream inputStream;

        if (properties.getBoolean(PROPERTY__READ_ENCRYPTED)) {
            CipherInputStream decStream;
            FileInputStream fis = new FileInputStream(file);
            try {
                decStream = new CipherInputStream(fis, cipher);
            } catch (Exception e) {
                logger.error("Failed to create encrypted stream.", e);
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
                logger.error("Failed to configure encryption.", e);
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
    public static Cipher getCipher(boolean readMode) throws InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, NoSuchAlgorithmException {

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
