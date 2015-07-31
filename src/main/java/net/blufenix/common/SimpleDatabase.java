package net.blufenix.common;

import net.blufenix.teleportationrunes.TeleportationRunes;

import java.sql.*;

/**
 * Created by blufenix on 7/27/15.
 */
public class SimpleDatabase {

    private static final String BASE_PATH = TeleportationRunes.getInstance().getDataFolder().getAbsolutePath();

    Connection connection = null;

    private final String DB_FILE_PATH;
    private final String DB_URL;

    public SimpleDatabase(String filename) {
        this.DB_FILE_PATH = BASE_PATH + "/" + filename;
        this.DB_URL = "jdbc:sqlite:"+DB_FILE_PATH;
        loadDriver();
        openConnection();
    }

    // TODO close connection (when?)
    // TODO connection pool

    private void openConnection() {
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }

//        try {
//            if (connection != null) {
//                connection.close();
//            }
//        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getDatabaseFilePath() {
        return DB_FILE_PATH;
    }

    protected boolean execute(String sql) {
        return (Boolean) rawQuery(StatementType.EXECUTE, sql);
    }

    protected ResultSet query(String sql) {
        return (ResultSet) rawQuery(StatementType.QUERY, sql);
    }

    protected boolean update(String sql) {
        return (Boolean) rawQuery(StatementType.UPDATE, sql);
    }

    private Object rawQuery(StatementType statementType, String sql) {
        TeleportationRunes.getInstance().getLogger().info("SQL: "+sql);

        try {
            Statement stmt = connection.createStatement();

            switch (statementType) {
                case EXECUTE:
                    return stmt.execute(sql);
                case QUERY:
                    return stmt.executeQuery(sql);
                case UPDATE:
                    return stmt.executeUpdate(sql);
            }

        } catch (SQLException e1) { e1.printStackTrace(); }

        return null;
    }

    private enum StatementType {
        EXECUTE, QUERY, UPDATE
    }
}
