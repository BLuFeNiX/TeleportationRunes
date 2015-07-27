package net.blufenix.teleportationrunes;

import java.io.Serializable;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Location;
import org.bukkit.Material;

public class Signature2 implements Serializable {

	private static final long serialVersionUID = 8365784548207556929L;
	
	Material northBlock;
	Material southBlock;
	Material eastBlock;
	Material westBlock;
	
	Signature2(Material n, Material s, Material e, Material w) {
		northBlock = n;
		southBlock = s;
		eastBlock = e;
		westBlock = w;
	}
	
	Material getNorth() {
		return northBlock;
	}
	
	Material getSouth() {
		return southBlock;
	}
	
	Material getEast() {
		return eastBlock;
	}
	
	Material getWest() {
		return westBlock;
	}
	
	public static Signature2 getSignatureFromLocation(Location loc) {
		Material northBlock = loc.clone().add(0, 0, -1).getBlock().getType();
		Material southBlock = loc.clone().add(0, 0, 1).getBlock().getType();
		Material eastBlock = loc.clone().add(1, 0, 0).getBlock().getType();
		Material westBlock = loc.clone().add(-1, 0, 0).getBlock().getType();
		Signature2 sig = new Signature2(northBlock, southBlock, eastBlock, westBlock);
		return sig;
	}
	
	public boolean equals(Signature2 sig) {
		boolean match = (sig.getNorth() == northBlock)
					 && (sig.getEast() == eastBlock)
					 && (sig.getWest() == westBlock);
		return match;
	}
	
	public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
		if (obj instanceof Signature2) {
			return equals((Signature2)obj);
		}
		
		return false;
	}
	
	public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
        	append(northBlock).
        	append(southBlock).
        	append(eastBlock).
        	append(westBlock).
            toHashCode();
    }
	
}
