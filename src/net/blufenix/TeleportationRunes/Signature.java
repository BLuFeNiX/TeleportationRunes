package net.blufenix.TeleportationRunes;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Location;

public class Signature {
	
	int northBlock;
	int southBlock;
	int eastBlock;
	int westBlock;
	
	Signature(int n, int s, int e, int w) {
		northBlock = n;
		southBlock = s;
		eastBlock = e;
		westBlock = w;
	}
	
	int getNorth() {
		return northBlock;
	}
	
	int getSouth() {
		return southBlock;
	}
	
	int getEast() {
		return eastBlock;
	}
	
	int getWest() {
		return westBlock;
	}
	
	public static Signature getSignatureFromLocation(Location loc) {
		int northBlock = loc.clone().add(0, 0, -1).getBlock().getType().getId();
		int southBlock = loc.clone().add(0, 0, 1).getBlock().getType().getId();
		int eastBlock = loc.clone().add(1, 0, 0).getBlock().getType().getId();
		int westBlock = loc.clone().add(-1, 0, 0).getBlock().getType().getId();
		Signature sig = new Signature(northBlock, southBlock, eastBlock, westBlock);
		return sig;
	}
	
	public boolean equals(Signature sig) {
		boolean match = (sig.getNorth() == northBlock)
					 && (sig.getSouth() == southBlock)
					 && (sig.getEast() == eastBlock)
					 && (sig.getWest() == westBlock);
		return match;
	}
	
	public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
		if (obj instanceof Signature) {
			return equals((Signature)obj);
//			Signature sig = (Signature) obj;
//	        return new EqualsBuilder().
//	            append(name, rhs.name).
//	            append(age, rhs.age).
//	            isEquals();
//			
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
