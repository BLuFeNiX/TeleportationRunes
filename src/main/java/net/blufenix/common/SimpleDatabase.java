package net.blufenix.common;

import net.blufenix.teleportationrunes.TeleportationRunes;

import java.sql.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
//        this.DB_URL = "jdbc:sqlite:"+DB_FILE_PATH;
        this.DB_URL = "jdbc:hsqldb:"+DB_FILE_PATH+";sql.syntax_pgs=true;hsqldb.lock_file=false";
        loadDriver();
        openConnections();
    }

    private static void loadDriver() {
        try {
//            Class.forName("org.sqlite.JDBC");
            Class.forName("org.hsqldb.jdbcDriver");
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

    protected boolean execute(String sql) throws SQLException {
        return (Boolean) rawQuery(StatementType.EXECUTE, sql, null);
    }

    protected Object query(String sql, QueryHandler<?> handler) throws SQLException {
        return rawQuery(StatementType.QUERY, sql, handler);
    }

    protected boolean update(String sql) throws SQLException {
        return (Boolean) rawQuery(StatementType.UPDATE, sql, null);
    }

    // TODO: refactor this logic?
    //  Previosuly, this method did not correctly clean up ResultSets and Statements, and it did not wait
    //  until the query was done before returning the connection to the pool.
    //  This is now fixed with the addition of QueryHandler<T>, but could probably be made cleaner.
    private Object rawQuery(StatementType statementType, String sql, QueryHandler<?> handler) throws SQLException {
        Log.d("Executing SQL (%s): %s", statementType.toString(), sql);

        Connection connection = borrowConnection();
        try (Statement stmt = connection.createStatement()) {
            switch (statementType) {
                case EXECUTE:
                    return stmt.execute(sql);
                case QUERY:
                    if (handler == null) {
                        throw new IllegalArgumentException("query must use a handler!");
                    }
                    return handler.handle0(stmt.executeQuery(sql));
                case UPDATE:
                    return stmt.executeUpdate(sql);
            }
        }
        finally {
            if (connection != null && !connection.isClosed()) {
                returnConnection(connection);
            }
        }

        return null;
    }

    private enum StatementType {
        EXECUTE, QUERY, UPDATE
    }

    public abstract static class QueryHandler<T> {
        Object handle0(ResultSet resultSet) throws SQLException {
            try {
                return handle(resultSet);
            } finally {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    Log.e("error closing resultSet", e);
                }
            }
        }
        protected abstract T handle(ResultSet resultSet) throws SQLException;
    }
}
