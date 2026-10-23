
package com.trustvault;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.SecureRandom;

public class FileEncryptionUtil {

    private static final String ALGORITHM =
            "AES/GCM/NoPadding";

    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    private static final int KEY_SIZE = 32;

    private static final Path KEY_FILE =
            Paths.get("trustvault.key");

    private static final SecretKey SECRET_KEY =
            loadOrCreateKey();

    // Load existing key or create a new key
    private static SecretKey loadOrCreateKey() {

        try {

            if (Files.exists(KEY_FILE)) {

                byte[] keyBytes =
                        Files.readAllBytes(KEY_FILE);

                if (keyBytes.length != KEY_SIZE) {
                    throw new IllegalStateException(
                            "Invalid encryption key file.");
                }

                return new SecretKeySpec(
                        keyBytes,
                        "AES"
                );
            }

            KeyGenerator keyGenerator =
                    KeyGenerator.getInstance("AES");

            keyGenerator.init(256);

            SecretKey key =
                    keyGenerator.generateKey();

            Files.write(
                    KEY_FILE,
                    key.getEncoded()
            );

            return key;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load encryption key",
                    e
            );
        }
    }

    // Encrypt file data
    public static byte[] encrypt(byte[] data) {

        try {

            byte[] iv = new byte[IV_SIZE];

            new SecureRandom().nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(
                            TAG_SIZE,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(data);

            byte[] result =
                    new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    result,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    result,
                    iv.length,
                    encrypted.length
            );

            return result;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Encryption failed",
                    e
            );
        }
    }

    // Decrypt file data
    public static byte[] decrypt(
            byte[] encryptedData) {

        try {

            if (encryptedData == null ||
                    encryptedData.length <= IV_SIZE) {

                throw new IllegalArgumentException(
                        "Invalid encrypted file.");
            }

            byte[] iv =
                    new byte[IV_SIZE];

            System.arraycopy(
                    encryptedData,
                    0,
                    iv,
                    0,
                    IV_SIZE
            );

            byte[] encrypted =
                    new byte[
                            encryptedData.length - IV_SIZE
                    ];

            System.arraycopy(
                    encryptedData,
                    IV_SIZE,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(
                            TAG_SIZE,
                            iv
                    )
            );

            return cipher.doFinal(encrypted);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Decryption failed",
                    e
            );
        }
    }
}
