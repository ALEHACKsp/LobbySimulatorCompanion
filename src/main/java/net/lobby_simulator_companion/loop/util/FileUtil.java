package net.lobby_simulator_companion.loop.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.Factory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Includes various methods commonly needed by various modules of the program.
 *
 * @author ShadowMoose
 */
@Slf4j
@UtilityClass
public class FileUtil {

    private Path APP_PATH = Paths.get(Factory.appProperties().get("app.home"));


    /**
     * Get the base directory path for all LOOP files, if left at default.
     * Can be changed by the user, by editing 'base_dir' in the settings ini.
     * The path given will always end with a backslash.
     *
     * @return A String file path, for convenience.
     */
    public Path getLoopPath() {
        return APP_PATH;
    }


    /**
     * Attempts to back up any copies of valid Files passed to it. <br>
     * Supports creating multiple rolling backups of the same file within the same supplied backup dir.
     *
     * @param f         The file to duplicate
     * @param backupDir The directory to store the backup in.
     * @return True if the save works.
     */
    public boolean saveFile(File f, File backupDir) {
        if (!backupDir.exists()) {
            backupDir.mkdirs();
            log.info("Built backup directory: {}", backupDir.getAbsolutePath());
        }

        if (!f.exists()) {
            return false;
        }

        File copy = getSaveName(f, 1);
        if (!copy.getParentFile().exists())
            copy.getParentFile().mkdirs();

        try {
            if (f.exists()) {
                Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.debug("Made backup of file.");
            }
        } catch (IOException e) {
            log.error("Failed to copy file for backup.", e);
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
    public boolean saveFile(File f, String subdirs) {
        File backupDir = getLoopPath().resolve(subdirs).toFile();

        return saveFile(f, backupDir);
    }

    /**
     * Get a File argument representing version <i>version</i> of the given File <i>f</i>.<br>
     * It is crucial (for ease of tracking) that all backup files follow the same naming conventions.<br>
     * This function exists to enforce those conventions.
     */
    public File getSaveName(File f, int version) {
        return new File(f.getParentFile().getAbsolutePath() + "/" + f.getName()
                + (version != 0 ? "." + version + ".bak" : ""));
    }

    /**
     * Generate an InputStream to the given resource file name.  <br>
     * Automatically toggles between JAR and Build paths.
     *
     * @param resourceName The name or relative filepath of the desired File.
     * @return Null if File cannot be found, otherwise the resource's Stream.
     */
    public InputStream localResource(String resourceName) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
    }

}
