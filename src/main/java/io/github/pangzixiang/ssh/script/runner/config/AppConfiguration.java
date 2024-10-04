package io.github.pangzixiang.ssh.script.runner.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import java.util.Objects;

public class AppConfiguration {
    private final Config config;
    private static AppConfiguration instance;
    private AppConfiguration(String env) {
        this.config = ConfigFactory.load(env);
    }
    public static synchronized AppConfiguration getInstance() {
        if (instance == null) {
            String env = System.getProperty("ssh-script-runner.env", "dev");
            instance = new AppConfiguration(env);
        }
        return instance;
    }

    private Object get(String key, Object defaultValue) {
        try {
            return config.getValue(key).unwrapped();
        } catch (ConfigException.Missing e) {
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    public String getString(String key) {
        return Objects.requireNonNull(getString(key, null), "Missing value for key: " + key);
    }

    public Integer getInt(String key, Integer defaultValue) {
        return (Integer) get(key, defaultValue);
    }

    public Integer getInt(String key) {
        return Objects.requireNonNull(getInt(key, null), "Missing value for key: " + key);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) get(key, defaultValue);
    }

    public Boolean getBoolean(String key) {
        return Objects.requireNonNull(getBoolean(key, null), "Missing value for key: " + key);
    }

}
