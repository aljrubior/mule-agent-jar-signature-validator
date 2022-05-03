package com.example.agent;

import com.example.agent.args.TruststoreConfig;
import com.example.agent.args.ValidatorArgs;
import com.mulesoft.agent.exception.ApplicationValidationException;
import com.mulesoft.agent.services.ApplicationValidator;
import com.mulesoft.agent.services.EncryptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;

@Named("JarSignatureValidator")
@Singleton
public class MuleAgentJarSignatureValidator implements ApplicationValidator {

    private static final Logger LOGGER = LogManager.getLogger(MuleAgentJarSignatureValidator.class);

    public String getType() {
        return "jarSignatureValidator";
    }

    public String getName() {
        return "defaultJarSignatureValidator";
    }

    @Inject
    EncryptionService encryptionService;

    public void validate(Map<String, Object> args) throws IOException, ApplicationValidationException {

        ValidatorArgs validatorArgs = ValidatorArgs.valueOf(args);

        TruststoreConfig truststoreConfig = validatorArgs.getTruststoreConfig();

        String artifactPath = validatorArgs.getArtifactPath();

        Optional<Path> truststorePath = getTrustStorePath(truststoreConfig.getPath());

        String truststorePassword = null;

        try {
            truststorePassword = encryptionService.decrypt(truststoreConfig.getPassword());
        } catch (Exception e) {

            e.printStackTrace();
            LOGGER.error(e);
        }

        KeyStore jarSigners = FileUtils.loadTruststore(truststorePath, truststoreConfig.getType(), truststorePassword);

        JarFile jarFile = newJarFile(artifactPath);

        JarSigner.newInstance(jarFile, jarSigners).verify();
    }

    private JarFile newJarFile(String path) throws IOException {
        File file = new File(path);
        return new JarFile(file, true);
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

        // $MULE_HOME/conf
        String configurationFolder = System.getProperty("mule.agent.configuration.folder");

        // Filename verification
        path = Paths.get(configurationFolder + File.separator + path.getFileName().toString());

        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(path);
        }

        return Optional.empty();
    }
}
