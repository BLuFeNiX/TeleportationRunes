package net.blufenix.teleportationrunes;

import net.blufenix.common.SimpleDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MigrationCompat {

    private static final String baseFileName = "waypoints.db";

    private static String getOldDBFilePath(String filename) {
        return TeleportationRunes.getInstance().getDataFolder().getAbsolutePath() + "/" + filename;
    }

    private static String getNewDBFilePath(String filename, SimpleDatabase.Backend backend) {
        return TeleportationRunes.getInstance().getDataFolder().getAbsolutePath() + "/" + backend.toString() + "/" + filename;
    }

    /**
     * For SQLite, the baseFileName is the actual DB
     */
    public static void maybeRelocateSQLiteDB() {
        File oldSQLiteFile = new File(getOldDBFilePath(baseFileName));
        File newSQLiteFile = new File(getNewDBFilePath(baseFileName, SimpleDatabase.Backend.SQLITE));
        if (oldSQLiteFile.exists()) {
            if (!newSQLiteFile.exists()) {
                try {
                    newSQLiteFile.getParentFile().mkdirs();
                    Files.move(oldSQLiteFile.toPath(), newSQLiteFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException("failed to move SQLite database file to new location!");
                }
            }
        }
    }

    /**
     * For HSQLDB, there are several files with the baseFileName prefix.
     */
    public static void maybeRelocateHSQLDB() {
        for (String ext : new String[]{".log", ".script", ".properties", ".tmp"}) {
            String filename = baseFileName + ext;
            File oldFile = new File(getOldDBFilePath(filename));
            File newFile = new File(getNewDBFilePath(filename, SimpleDatabase.Backend.HSQLDB));
            if (oldFile.exists()) {
                if (!newFile.exists()) {
                    try {
                        newFile.getParentFile().mkdirs();
                        Files.move(oldFile.toPath(), newFile.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException("failed to move HSQLDB database file to new location!");
                    }
                }
            }
        }
    }

    public static void maybeRelocateDB() {
        maybeRelocateSQLiteDB();
        maybeRelocateHSQLDB();
    }
}