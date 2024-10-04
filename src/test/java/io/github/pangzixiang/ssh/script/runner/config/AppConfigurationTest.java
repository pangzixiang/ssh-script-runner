package io.github.pangzixiang.ssh.script.runner.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppConfigurationTest {
    private AppConfiguration appConfiguration;
    @BeforeAll
    public void init() {
        System.setProperty("ssh-script-runner.env", "test");
        appConfiguration = AppConfiguration.getInstance();
    }

    @Test
    void testGetString() {
        Assertions.assertEquals("test", appConfiguration.getString("test.string"));
    }

    @Test
    void testGetBoolean() {
        Assertions.assertFalse(appConfiguration.getBoolean("test.boolean"));
    }

    @Test
    void testGetInteger() {
        Assertions.assertEquals(0, appConfiguration.getInt("app.port"));
    }

    @Test
    void testGetStringDefault() {
        Assertions.assertEquals("test", appConfiguration.getString("default.string", "test"));
    }

    @Test
    void testGetBooleanDefault() {
        Assertions.assertFalse(appConfiguration.getBoolean("default.boolean", false));
    }

    @Test
    void testGetIntegerDefault() {
        Assertions.assertEquals(0, appConfiguration.getInt("default.port", 0));
    }

    @Test
    void testGetStringMissing() {
        Assertions.assertThrows(NullPointerException.class, () -> appConfiguration.getString("default.string"));
    }

    @Test
    void testGetBooleanMissing() {
        Assertions.assertThrows(NullPointerException.class, () -> appConfiguration.getBoolean("default.boolean"));
    }

    @Test
    void testGetIntegerMissing() {
        Assertions.assertThrows(NullPointerException.class, () -> appConfiguration.getInt("default.port"));
    }
}
