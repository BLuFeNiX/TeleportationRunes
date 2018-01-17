package net.blufenix.common;

import net.blufenix.teleportationrunes.TeleportationRunes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtils {

    /**
     * Extracts all files from within the running jar's "libs/" directory, placing them in a "libs/" directory in
     * this plugin's data folder. It then iterates over the files (all assumed to be jars) and adds them to the
     * classpath of the SystemClassLoader
     */
    public static void loadLibs() {
        try {
            String dataFolder = TeleportationRunes.getInstance().getDataFolder().getAbsolutePath();
            extractDirFromJar("libs/", dataFolder);
            for (final File lib : new File(dataFolder+"/libs").listFiles()) {
                addClassPath(JarUtils.getJarUrl(lib));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Copies sourceDir (from JAR) into parentDestDir (on local filesystem)
     */
    private static boolean extractDirFromJar(String sourceDir, String parentDestDir) {
        try {
            if (!sourceDir.endsWith("/")) sourceDir += "/";

            JarFile jar = getRunningJar();
            Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry entry = enumEntries.nextElement();
                String entryName = entry.getName();

                if (!entryName.startsWith(sourceDir)) continue;

                File f = new File(parentDestDir + java.io.File.separator + entry.getName());
                if (entry.isDirectory()) { // if it's a directory, create it
                    f.mkdirs();
                    continue;
                }
                InputStream is = jar.getInputStream(entry); // get the input stream
                FileOutputStream fos = new FileOutputStream(f);
                while (is.available() > 0) {
                    fos.write(is.read()); // write contents of 'is' to 'fos'
                }
                fos.close();
                is.close();
            }
            jar.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static JarFile getRunningJar() {
        try {
            String path = new File(JarUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
            path = URLDecoder.decode(path, "UTF-8");
            return new JarFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static URL getJarUrl(final File file) throws IOException {
        return new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/");
    }

    private static void addClassPath(final URL url) throws IOException {
        final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        final Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            final Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { url });
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IOException("Error adding " + url + " to system classloader");
        }
    }

}