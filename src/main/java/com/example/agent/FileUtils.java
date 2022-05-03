package com.example.agent;

import com.example.agent.exceptions.KeyStoreException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.security.tools.KeyStoreUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Optional;
import java.util.jar.JarFile;

import static java.lang.String.format;

public class FileUtils {

    private static final Logger LOGGER = LogManager.getLogger(FileUtils.class);

    private FileUtils() { }

    public static String jvmCacertsPath() {
        String sep = File.separator;
        return System.getProperty("java.home") + sep + "lib" + sep + "security" + sep + "cacerts";
    }

    public static KeyStore loadTruststore(Optional<Path> truststoreFile, String keystoreType, String keystorePassword) throws KeyStoreException {

        String truststorePath = getAbsolutePathOrElseCacertsPath(truststoreFile);

        char[] password = (keystorePassword == null) ? null : keystorePassword.toCharArray();

        try {
            if (truststoreFile.isPresent()) {
                return KeystoreUtils.load(truststorePath, keystoreType, password);
            }

            return KeyStoreUtil.getCacertsKeyStore();

        } catch (Exception e) {
            String message = format("Unable to load Jar Signers truststore '%s'. Reason: %s", truststorePath, e.getMessage());
            LOGGER.error(message);
            throw new KeyStoreException(message);
        }
    }

    public JarFile newJarFile(String path) throws IOException {
        File file = new File(path);
        return new JarFile(file, true);
    }

    private static String getAbsolutePathOrElseCacertsPath(Optional<Path> path) {
        return
            ( path.isPresent() )
            ? path.get().toAbsolutePath().toString()
            : jvmCacertsPath();
    }
}
