package net.lobby_simulator_companion.loop.io.peer;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Class to simplify key generation/file access.
 */
public class Security {

    private static byte[] keyMaterial = new byte[]{2, 3, -57, 11, 73, 57, -66, 21};


    /**
     * Builds the Cipher Object used for encryption/decryption.
     *
     * @param readMode The Cipher will be initiated in either encrypt/decrypt mode.
     * @return Cipher object, ready to go.
     * @throws Exception Many possible issues can arise, so this is a catch-all.
     */
    public static Cipher getCipher(boolean readMode) throws InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, NoSuchAlgorithmException {

        DESKeySpec key = new DESKeySpec(keyMaterial);
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
