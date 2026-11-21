package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code ${NAME}} in config values with the environment variable {@code NAME}, so secrets like the MySQL
 * password don't have to stand in config.yml (which server owners share in support chats, with AI assistants or in
 * git). Values are resolved when they are read, never written back: saving the config keeps {@code ${NAME}}.
 *
 * @author Christian34
 */
public final class EnvironmentVariables {
    private static final Pattern REFERENCE = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");
    private static Function<String, String> lookup = System::getenv;

    private EnvironmentVariables() {
    }

    /**
     * @param value a config value, e.g. {@code "${CTB_MYSQL_PASSWORD}"}
     * @return the value with every {@code ${NAME}} replaced by the environment variable (empty if it isn't set, with a
     * warning naming the variable, never its value); values without a reference are returned unchanged
     */
    @Nullable
    public static String resolve(@Nullable String value, @NotNull String key) {
        if (value == null || !value.contains("${")) return value;
        Matcher matcher = REFERENCE.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String variable = lookup.apply(name);
            if (variable == null) {
                Debug.warn("The environment variable " + name + " (used in " + key + " of config.yml) isn't set");
                variable = "";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(variable));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * replaces {@link System#getenv(String)} (tests can't set real environment variables)
     */
    public static void setLookup(@NotNull Function<String, String> lookup) {
        EnvironmentVariables.lookup = Objects.requireNonNull(lookup);
    }

    public static void resetLookup() {
        lookup = System::getenv;
    }

}
