package net.blufenix.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.blufenix.teleportationrunes.TeleportationRunes;

public class Serializer {
	
	public static void serializeObject(Object obj, String filename) {
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(filename));
			out.writeObject(obj);
	        out.flush();
	        out.close();
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public static Object getSerializedObject(Class<?> c, String filename) {
		Object serObject = null;
	
			try {
				if (new File(filename).exists()) {
					ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
					Object tempObj = in.readObject();
					if (tempObj.getClass().equals(c)) {
						TeleportationRunes._instance.getLogger().info("Loading "+filename);
						serObject = tempObj;
					}
					in.close();
				}
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			catch (ClassNotFoundException e) { e.printStackTrace(); }
			
			if (serObject != null) {
				return serObject;
			}
			else {
				try {
					Class<?>[] args = {};
					Constructor<?> constructor;
					constructor = c.getConstructor(args);
					return constructor.newInstance((Object[])args);
				}
				catch (NoSuchMethodException e) { e.printStackTrace(); }
				catch (SecurityException e) { e.printStackTrace(); }
				catch (InstantiationException e) { e.printStackTrace(); }
				catch (IllegalAccessException e) { e.printStackTrace(); }
				catch (IllegalArgumentException e) { e.printStackTrace(); }
				catch (InvocationTargetException e) { e.printStackTrace(); }
			}
			
			return null;
	}
	
	public static Long getSerialVersionUID(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
		if (new File(filename).exists()) {
			UIDGrabber uidGrabber = new UIDGrabber(new FileInputStream(filename));
			long uid = uidGrabber.getSerialVersionUID();
			uidGrabber.close();
			return uid;
		}
		else { return null; }
	}
	
}
