package com.christian34.catchthebeacon.database;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.PluginFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The database of the player data: SQLite (a file in the plugin folder) or MySQL/MariaDB (shared by all servers of a
 * network), see sql.* in config.yml (same keys as EasyPrefix). Paper ships both drivers.
 * All queries run one after another on an own thread, so they never block the server and keep their order.
 *
 * @author Christian34
 */
public class Database {
    /**
     * no SSL like EasyPrefix (MySQL 8 then needs allowPublicKeyRetrieval), don't block the server start for long
     */
    private static final String MYSQL_PARAMETERS = "useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000";
    private final Type type;
    private final String url;
    private final String user;
    private final String password;
    private final String tablePrefix;
    private final ExecutorService executor;
    private Connection connection;

    public enum Type {
        SQLITE, MYSQL
    }

    @FunctionalInterface
    public interface Query<T> {
        T run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface Update {
        void run(Connection connection) throws SQLException;
    }

    /**
     * @throws SQLException if the database can't be reached
     */
    public Database(CatchTheBeacon plugin) throws SQLException {
        PluginFile config = plugin.getFileManager().getConfigFile();
        this.type = config.getBoolean("sql.enabled") ? Type.MYSQL : Type.SQLITE;
        this.tablePrefix = tablePrefix(config.getString("sql.table-prefix"));
        if (type == Type.MYSQL) {
            this.url = "jdbc:mysql://" + config.getString("sql.host") + ":" + config.getInt("sql.port")
                    + "/" + config.getString("sql.database") + "?" + MYSQL_PARAMETERS;
            this.user = config.getString("sql.username");
            this.password = config.getString("sql.password");
        } else {
            this.url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath();
            this.user = null;
            this.password = null;
        }
        loadDriver();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CatchTheBeacon-Database");
            thread.setDaemon(true);
            return thread;
        });
        this.connection = connect();
    }

    /**
     * @return the prefix with "_" appended like EasyPrefix ("ctb" → "ctb_"), "" if there is none
     * @throws SQLException if it isn't a valid table name (it is part of the queries)
     */
    static String tablePrefix(@Nullable String prefix) throws SQLException {
        if (prefix == null || prefix.isBlank()) return "";
        prefix = prefix.trim();
        if (!prefix.matches("\\w+")) throw new SQLException("invalid sql.table-prefix '" + prefix + "' (only letters, digits and _)");
        return prefix.endsWith("_") ? prefix : prefix + "_";
    }

    private void loadDriver() {
        try {
            Class.forName(type == Type.MYSQL ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            // DriverManager may still find another one
        }
    }

    private Connection connect() throws SQLException {
        return user == null ? DriverManager.getConnection(url) : DriverManager.getConnection(url, user, password);
    }

    private Connection getConnection() throws SQLException {
        // MySQL closes idle connections
        if (connection == null || !connection.isValid(2)) {
            connection = connect();
        }
        return connection;
    }

    public Type getType() {
        return type;
    }

    /**
     * @return the name of the table with the configured prefix
     */
    public String table(String name) {
        return tablePrefix + name;
    }

    /**
     * @return "INSERT OR IGNORE" (SQLite) or "INSERT IGNORE" (MySQL): inserts a row unless the key exists already
     */
    public String insertIgnore() {
        return type == Type.SQLITE ? "INSERT OR IGNORE" : "INSERT IGNORE";
    }

    /**
     * runs the query on the database thread
     *
     * @return completes exceptionally if the query failed (already logged)
     */
    public <T> CompletableFuture<T> query(@NotNull Query<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query.run(getConnection());
            } catch (SQLException ex) {
                Debug.warn("Database error: " + ex.getMessage());
                throw new CompletionException(ex);
            }
        }, executor);
    }

    public CompletableFuture<Void> update(@NotNull Update update) {
        return query(connection -> {
            update.run(connection);
            return null;
        });
    }

    /**
     * finishes the queued queries and closes the connection
     */
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                Debug.warn("Some database queries didn't finish in time!");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            Debug.warn("Couldn't close the database connection: " + ex.getMessage());
        }
    }

}
