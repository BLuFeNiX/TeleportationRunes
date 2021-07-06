package net.blufenix.common;

import net.blufenix.teleportationrunes.TeleportationRunes;

import java.sql.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Created by blufenix on 7/27/15.
 */
public class SimpleDatabase {

    private static final String BASE_PATH = TeleportationRunes.getInstance().getDataFolder().getAbsolutePath();
    private static final int NUM_INITIAL_CONNECTIONS = 0;

    private final Queue<Connection> connectionPool = new ConcurrentLinkedQueue<Connection>();

    private final String DB_FILE_PATH;
    private final String DB_URL;

    public SimpleDatabase(String filename) {
        this.DB_FILE_PATH = BASE_PATH + "/" + filename;
        this.DB_URL = "jdbc:sqlite:"+DB_FILE_PATH;
        loadDriver();
        openConnections();
    }

    private static void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.e("can't load JDBC driver", e);
        }
    }

    private void openConnections() {
        for (int i = 0; i < NUM_INITIAL_CONNECTIONS; i++) {
            connectionPool.add(getNewConnection());
        }
    }

    public void closeConnections() {
        try {
            for (Connection con : connectionPool) {
                if (con != null) {
                    con.close();
                }
            }
        } catch (SQLException e) {
            Log.e("error closing connections", e);
        }
    }

    private Connection borrowConnection() {
        if (!connectionPool.isEmpty()) {
            Connection con = connectionPool.remove();
            if (con != null) {
                return con;
            }
            else {
                Log.e("NULL connection returned from queue.");
            }
        }
        return getNewConnection();
    }

    private void returnConnection(Connection con) {
        connectionPool.add(con);
    }

    private Connection getNewConnection() {
        Log.d("Creating new database connection: "+connectionPool.size()+1);
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            Log.e("can't get DB connection", e);
        }
        return null;
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
        Log.d("Executing SQL: "+sql);

        Connection connection = borrowConnection();
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
        } catch (SQLException e1) {
            Log.e("query error", e1);
        }
        finally {
            if (connection != null) {
                returnConnection(connection);
            }
        }

        return null;
    }

    private enum StatementType {
        EXECUTE, QUERY, UPDATE
    }
}
