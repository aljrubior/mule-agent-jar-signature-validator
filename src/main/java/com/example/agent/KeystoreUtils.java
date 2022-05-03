package com.example.agent;

import com.example.agent.exceptions.KeyStoreException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import static java.lang.String.format;

public class KeystoreUtils {

    private static final Logger LOGGER = LogManager.getLogger(KeystoreUtils.class);

    private KeystoreUtils() { }

    private static File newFileOrElseThrowsException(String path) throws KeyStoreException {
        File file = new File(path);

        if (!file.exists()) {
            String message = format("Unable to load the Jar Signers truststore '%s'. Reason: File does not exists.", path);
            LOGGER.error(message);
            throw new KeyStoreException(message);
        }

        return file;
    }

    private static KeyStore newKeystoreOrElseThrowsException(File file, String type, char[] password) throws KeyStoreException {

        KeyStore keystore;

        try (FileInputStream fis = new FileInputStream(file)) {

            keystore = KeyStore.getInstance(type);
            keystore.load(fis, password);
            return keystore;
        } catch (Exception e) {

            String message = format("Unable to load Jar Signers truststore '%s'. Reason: %s", file.getAbsolutePath(), e.getMessage());
            LOGGER.error(message);
            throw new KeyStoreException(message);
        }
    }

    public static KeyStore load(String path, String keystoreType, char[] keystorePassword) throws KeyStoreException {

        File file = newFileOrElseThrowsException(path);

        KeyStore keystore = newKeystoreOrElseThrowsException(file, keystoreType, keystorePassword);

        LOGGER.info("Loading Jar Signers Truststore from '{}'", path);

        return keystore;
    }
}
