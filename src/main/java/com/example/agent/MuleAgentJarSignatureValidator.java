package com.example.agent;

import com.mulesoft.agent.exception.ApplicationValidationException;
import com.mulesoft.agent.services.ApplicationValidator;
import com.mulesoft.agent.services.EncryptionService;
import com.example.agent.exceptions.KeyStoreException;
import com.example.agent.exceptions.SignatureVerificationFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.security.tools.KeyStoreUtil;
import sun.security.util.SignatureFileVerifier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.lang.String.format;

@Named("JarSignatureValidator")
@Singleton
public class MuleAgentJarSignatureValidator implements ApplicationValidator {

    private static final Logger LOGGER = LogManager.getLogger(MuleAgentJarSignatureValidator.class);

    // Properties provided by Mule Agent Plugin
    public static final String APPLICATION_NAME_KEY = "_APPLICATION_NAME";
    public static final String APPLICATION_FILE_PATH_KEY = "_APPLICATION_FILE_PATH";

    // Properties configured in the Artifact Validator service
    public static final String TRUSTSTORE_KEY = "truststore";
    public static final String TRUSTSTORE_TYPE_KEY = "truststoreType";
    public static final String TRUSTSTORE_PASSWORD_KEY = "truststorePassword";

    public String getType() {
        return "jarSignatureValidator";
    }

    public String getName() {
        return "defaultJarSignatureValidator";
    }

    @Inject
    EncryptionService encryptionService;

    public void validate(Map<String, Object> args) throws IOException, ApplicationValidationException {

        // KeyStore
        String truststore = (String) args.get(TRUSTSTORE_KEY);
        String truststoreType = this.getTruststoreType((String) args.get(TRUSTSTORE_TYPE_KEY));
        Optional<Path> truststorePath = getTrustStorePath(truststore);
        String encryptedTruststorePassword = (String) args.get(TRUSTSTORE_PASSWORD_KEY);
        String truststorePassword = null;

        try {
            truststorePassword = encryptionService.decrypt(encryptedTruststorePassword);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e);
        }

        KeyStore jarSigners = this.loadTruststore(truststorePath, truststoreType, truststorePassword);

        // Artifact

        String artifactPath = (String) args.get(APPLICATION_FILE_PATH_KEY);

        File artifactFile = new File(artifactPath);

        // Validation Start

        JarFile jarFile = newJarFile(artifactFile);

        Vector<JarEntry> jarEntries = readJarEntries(jarFile);
        
        Manifest manifest = jarFile.getManifest();

        validate(jarSigners, manifest, jarEntries);
    }

    private void validate(KeyStore truststore, Manifest manifest, Vector<JarEntry> jarEntries) throws SignatureVerificationFailedException {

        int filesCount = 0;
        int signedFilesCount = 0;

        if (manifest == null) {
            String message = "Artifact is unsigned. (signatures missing or not parsable)";
            LOGGER.error(message);
            throw new SignatureVerificationFailedException(message);
        }

        for (JarEntry jarEntry: jarEntries){
            if (jarEntry.isDirectory() || SignatureFileVerifier.isBlockOrSF(jarEntry.getName())) continue;

            filesCount++;

            CodeSigner[] codeSigners = jarEntry.getCodeSigners();
            boolean isSigned = (codeSigners != null);

            if (!isSigned) continue;

            for (CodeSigner signer: codeSigners) {
                if (contains(truststore, signer)) signedFilesCount++;
            }
        }

        if (filesCount != signedFilesCount) {
            String message = "Signature validation failed.";
            LOGGER.error(message);
            throw new SignatureVerificationFailedException(message);
        }
    }

    private JarFile newJarFile(File jarFile) throws IOException {
        return new JarFile(jarFile, true);
    }

    public Vector<JarEntry> readJarEntries(JarFile jarFile) {

        Vector<JarEntry> jarEntries = new Vector<>();

        byte[] buffer = new byte[8192];

        Collections.list(jarFile.entries()).forEach(entry -> {
            jarEntries.add(entry);

            try {
                InputStream is = null;
                try {
                    is = jarFile.getInputStream(entry);
                    int n;
                    while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                        // we just read. this will throw a SecurityException
                        // if  a signature/digest check fails.
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (Exception e) {

            }
        });

        return jarEntries;
    }

    private boolean contains(KeyStore keystore, CodeSigner codeSigner) {

        List<? extends Certificate> certificates = codeSigner.getSignerCertPath().getCertificates();

        try {
            for(Certificate c: certificates) {
                String alias = keystore.getCertificateAlias(c);

                if (alias != null) return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        return false;
    }

    private String getTruststoreType(String type) {
        if (type == null) {
            return "JKS";
        }

        return type;
    }

    private Optional<Path> getTrustStorePath(String filePath) {

        if (filePath == null) {
            LOGGER.warn("Truststore property not defined for Artifact Validator Service");
            return Optional.empty();
        }

        if (filePath.isEmpty()) {
            LOGGER.warn("Truststore property in Artifact Validator Service configuration is empty");
            return Optional.empty();
        }

        Path path = Paths.get(filePath);

        // Absolute Path verification
        if (path.isAbsolute() && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(path);
        }

        // Relative Path verification
        if (path.getParent() != null) {
            LOGGER.error("Relative path in truststore file reference not supported by Artifact Validator Service.");
            return Optional.empty();
        }

        String configurationFolder = System.getProperty("mule.agent.configuration.folder");

        // Filename verification
        path = Paths.get(configurationFolder + File.separator + path.getFileName().toString());

        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(path);
        }

        return Optional.empty();
    }

    private KeyStore loadTruststore(Optional<Path> truststoreFile, String keystoreType, String keystorePassword) throws KeyStoreException {

        String truststorePath = "";

        KeyStore keystore;

        try {
            if (!truststoreFile.isPresent()) {
                String sep = File.separator;
                truststorePath  = System.getProperty("java.home") + sep + "lib" + sep + "security" + sep + "cacerts";

                keystore = KeyStoreUtil.getCacertsKeyStore();
            } else {
                truststorePath = truststoreFile.get().toAbsolutePath().toString();

                File file = new File(truststorePath);

                if (!file.exists()) {
                    String message = format("Unable to load the Jar Signers truststore '%s'. Reason: File does not exists.", truststorePath);
                    LOGGER.error(message);
                    throw new KeyStoreException(message);
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    keystore = KeyStore.getInstance(keystoreType);

                    char[] password = (keystorePassword == null) ? null : keystorePassword.toCharArray();

                   keystore.load(fis, password);
                }
            }

            LOGGER.info("Loading Jar Signers Truststore from '{}'", truststorePath);

            return keystore;

        } catch (Exception e) {
            String message = format("Unable to load Jar Signers truststore '%s'. Reason: %s", truststorePath, e.getMessage());
            LOGGER.error(message);
            throw new KeyStoreException(message);
        }
    }
}
