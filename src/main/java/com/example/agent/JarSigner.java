package com.example.agent;

import com.example.agent.exceptions.SignatureVerificationFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.security.util.SignatureFileVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.lang.String.format;

public class JarSigner {

    private static final Logger LOGGER = LogManager.getLogger(JarSigner.class);

    private final JarFile jarFile;

    private final KeyStore truststore;

    private JarSigner(JarFile jarFile, KeyStore truststore) {
        this.jarFile = jarFile;
        this.truststore = truststore;
    }

    public static JarSigner newInstance(JarFile jarFile, KeyStore truststore) {
        return new JarSigner(jarFile, truststore);
    }

    public void verify() throws IOException, SignatureVerificationFailedException {

        Vector<JarEntry> jarEntries = readJarEntries(jarFile);

        Manifest manifest = jarFile.getManifest();

        validate(truststore, manifest, jarEntries);

        LOGGER.info(format("Jar Signature verified: File %s", jarFile.getName()));
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

    private Vector<JarEntry> readJarEntries(JarFile jarFile) {

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


}
