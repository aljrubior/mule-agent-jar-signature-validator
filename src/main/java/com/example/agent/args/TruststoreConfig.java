package com.example.agent.args;

public class TruststoreConfig {

    private final String type;

    private final String path;

    private final String password;

    private TruststoreConfig(final String type, final String path, final String password) {
        this.type = type;
        this.path = path;
        this.password = password;
    }

    public static TruststoreConfig newInstance(final String type, final String path, final String password) {
        return new TruststoreConfig(type, path, password);
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public String getPassword() {
        return password;
    }
}
