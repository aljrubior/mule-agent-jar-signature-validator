package com.example.agent.args;

import java.util.Map;

public class ValidatorArgs {

    // Properties provided by Mule Agent Plugin
    public static final String APPLICATION_NAME_KEY = "_APPLICATION_NAME";
    public static final String APPLICATION_FILE_PATH_KEY = "_APPLICATION_FILE_PATH";

    // Properties configured in the Artifact Validator service
    public static final String TRUSTSTORE_PATH_KEY = "truststore";
    public static final String TRUSTSTORE_TYPE_KEY = "truststoreType";
    public static final String TRUSTSTORE_PASSWORD_KEY = "truststorePassword";

    private final Map<String, Object> args;

    private ValidatorArgs(final Map<String, Object> args) {
        this.args = args;
    }

    public static ValidatorArgs valueOf(final Map<String, Object> args) {
        return new ValidatorArgs(args);
    }

    public TruststoreConfig getTruststoreConfig() {
        String type = (String) args.get(TRUSTSTORE_TYPE_KEY);
        String path = (String) args.get(TRUSTSTORE_PATH_KEY);
        String password = (String) args.get(TRUSTSTORE_PASSWORD_KEY);

        return TruststoreConfig.newInstance(type, getTruststoreType(path), password);
    }

    public String getArtifactPath() {
        return (String) args.get(APPLICATION_FILE_PATH_KEY);
    }

    private String getTruststoreType(String type) {
        if (type == null) {
            return "JKS";
        }

        return type;
    }

}
