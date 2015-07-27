package net.blufenix.teleportationrunes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.blufenix.common.Serializer;

import org.bukkit.Material;

public class Upgrader {

    @SuppressWarnings("unchecked")
    public static void checkForOldDataFormat() {
    	Map<Object, Object> tempData = (HashMap<Object, Object>)Serializer.getSerializedObject(HashMap.class, TeleportationRunes.dataFile);
    	if (!tempData.isEmpty()) {
    		if (tempData.keySet().iterator().next() instanceof Signature) {
    			TeleportationRunes._instance.getLogger().info("Old data format detected. Upgrading!");
				convertDataFormat();
    		}
    	}
    	else {
    		new File(TeleportationRunes.dataFile).delete(); // delete the empty data file (in case the format of empty matters), it will be recreated
    	}		
	}

	public static void convertDataFormat() {
    	Map<Signature, LocationWrapper> oldData = (HashMap<Signature, LocationWrapper>) Serializer.getSerializedObject(HashMap.class, TeleportationRunes.dataFile);
    	Map<Signature2, LocationWrapper> newData = new HashMap<Signature2, LocationWrapper>();
    	for (Entry<Signature, LocationWrapper> entry : oldData.entrySet()) {
    		Signature oldSig = entry.getKey();
    		int n = oldSig.getNorth();
    		int s = oldSig.getSouth();
    		int e = oldSig.getEast();
    		int w = oldSig.getWest();
    		
    		Signature2 newSig = new Signature2(Material.getMaterial(n), Material.getMaterial(s), Material.getMaterial(e), Material.getMaterial(w));
    		newData.put(newSig, entry.getValue());
    	}
    	Serializer.serializeObject(newData, TeleportationRunes.dataFile);
	}
	
}
