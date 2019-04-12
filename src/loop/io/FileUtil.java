package loop.io;

import loop.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Includes various methods commonly needed by various modules of the program.
 *
 * @author ShadowMoose
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Get the base directory path for all LOOP files, if left at default .  <br>
     * Can be changed by the user, by editing 'base_dir' in the settings ini. <br>
     * The path given will always end with a backslash. <br>
     *
     * @return A String file path, for convenience.
     */
    public static String getLoopPath() {
        String f = Settings.get("base_dir", null);
        if (f == null) {
            f = new File(System.getenv("APPDATA") + "/" + Constants.APP_SHORT_NAME + "/").getAbsolutePath();
            Settings.set("base_dir", f);
        }
        return (f.endsWith("/") ? f : (f + "/"));
    }


    /**
     * Attempts to back up any copies of valid Files passed to it. <br>
     * Supports creating multiple rolling backups of the same file within the same supplied backup dir.
     *
     * @param f          The file to duplicate
     * @param backup_dir The directory to store the backup in.
     * @return True if the save works.
     */
    public static boolean saveFile(File f, File backup_dir) {
        if (!backup_dir.exists()) {
            backup_dir.mkdirs();
            logger.info("Built backup directory: {}", backup_dir.getAbsolutePath());
        }

        if (!f.exists()) {
            return false;
        }

        logger.info("Saving: {}", f);
        File copy = getSaveName(f, 1);
        if (!copy.getParentFile().exists())
            copy.getParentFile().mkdirs();

        try {
            if (f.exists()) {
                Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Made backup of file.");
            }
        } catch (IOException e) {
            logger.error("Failed to copy file for backup.", e);
            return false;
        }
        return true;
    }

    /**
     * Simplified version of {@link #saveFile(File, File)},
     * this method always uses the directory provided by {@link #getLoopPath()},
     * and appends the supplied string to the directory as a subdirectory path for the backup files.
     *
     * @param f       The file to duplicate
     * @param subdirs The subdirectory path within the installation directory to use for the copies.
     * @return True if the save works.
     */
    public static boolean saveFile(File f, String subdirs) {
        return saveFile(f, new File(getLoopPath() + subdirs + "/"));
    }

    /**
     * Get a File object representing version <i>version</i> of the given File <i>f</i>.<br>
     * It is crucial (for ease of tracking) that all backup files follow the same naming conventions.<br>
     * This function exists to enforce those conventions.
     */
    public static File getSaveName(File f, int version) {
        return new File(f.getParentFile().getAbsolutePath() + "/" + f.getName()
                + (version != 0 ? "." + version : ""));
    }

    /**
     * Generate an InputStream to the given resource file name.  <br>
     * Automatically toggles between JAR and Build paths.
     *
     * @param resourceName The name or relative filepath (if resource is deeper than just "resources/") of the desired File.
     * @return Null if File cannot be found, otherwise the resource's Stream.
     */
    public static InputStream localResource(String resourceName) {
        if (ClassLoader.getSystemClassLoader().getResourceAsStream("resources/" + resourceName) != null) {
            return ClassLoader.getSystemClassLoader().getResourceAsStream("resources/" + resourceName);
        } else {
            return ClassLoader.getSystemClassLoader().getResourceAsStream("src/resources/" + resourceName);
        }
    }

}
