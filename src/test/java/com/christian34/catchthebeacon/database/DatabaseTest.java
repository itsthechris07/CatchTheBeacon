package com.christian34.catchthebeacon.database;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The table prefix works like in EasyPrefix.
 *
 * @author Christian34
 */
class DatabaseTest {

    @Test
    void tablePrefix() throws SQLException {
        assertEquals("ctb_", Database.tablePrefix("ctb"));
        assertEquals("ctb_", Database.tablePrefix("ctb_"));
        assertEquals("", Database.tablePrefix(""));
        assertEquals("", Database.tablePrefix(null));
        assertEquals("ctb_", Database.tablePrefix(" ctb "));
        assertThrows(SQLException.class, () -> Database.tablePrefix("ctb; DROP TABLE users"));
    }

}
