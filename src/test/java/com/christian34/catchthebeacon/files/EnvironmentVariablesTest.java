package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.PluginTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@code ${NAME}} in config.yml ({@link EnvironmentVariables}), with a fake environment.
 *
 * @author Christian34
 */
class EnvironmentVariablesTest extends PluginTestBase {
    private final List<String> warnings = new ArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            warnings.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void fakeEnvironment() {
        Map<String, String> environment = Map.of("CTB_MYSQL_PASSWORD", "s3cr$t\\pw", "CTB_MYSQL_PORT", "3307",
                "CTB_MYSQL_HOST", "db.example.com");
        EnvironmentVariables.setLookup(environment::get);
        plugin.getLogger().addHandler(handler);
    }

    @AfterEach
    void realEnvironment() {
        EnvironmentVariables.resetLookup();
        plugin.getLogger().removeHandler(handler);
    }

    private ConfigFile config() {
        return plugin.getFileManager().getConfigFile();
    }

    @Test
    void variableIsSet() {
        config().set("sql.password", "${CTB_MYSQL_PASSWORD}");
        assertEquals("s3cr$t\\pw", config().getString("sql.password"));
        assertTrue(warnings.isEmpty(), warnings::toString);
    }

    @Test
    void variableInsideAValue() {
        config().set("sql.host", "${CTB_MYSQL_HOST}:${CTB_MYSQL_PORT}");
        assertEquals("db.example.com:3307", config().getString("sql.host"));
    }

    @Test
    void portFromVariable() {
        config().set("sql.port", "${CTB_MYSQL_PORT}");
        assertEquals(3307, config().getInt("sql.port"));
        config().set("sql.port", 3306);
        assertEquals(3306, config().getInt("sql.port"));
    }

    @Test
    void variableNotSet() {
        config().set("sql.password", "${CTB_MISSING}");
        assertEquals("", config().getString("sql.password"));
        assertEquals(1, warnings.size(), warnings::toString);
        assertTrue(warnings.getFirst().contains("CTB_MISSING") && warnings.getFirst().contains("sql.password"));

        config().set("sql.port", "${CTB_MISSING}");
        assertEquals(0, config().getInt("sql.port"));
    }

    @Test
    void plainValue() {
        config().set("sql.password", "password");
        assertEquals("password", config().getString("sql.password"));
        config().set("sql.password", "pass$word{1}");
        assertEquals("pass$word{1}", config().getString("sql.password"));
        assertTrue(warnings.isEmpty(), warnings::toString);
    }

    @Test
    void savedFileKeepsTheReference() {
        config().set("sql.password", "${CTB_MYSQL_PASSWORD}");
        config().getString("sql.password");
        config().set("sql.username", "ctb");
        assertEquals("${CTB_MYSQL_PASSWORD}",
                YamlConfiguration.loadConfiguration(config().getSourceFile()).getString("sql.password"));
    }

    @Test
    void bundledConfigHasNoReference() {
        assertEquals("password", config().getString("sql.password"));
        assertEquals(3306, config().getInt("sql.port"));
    }

}
