package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;

public class Signature {

	public final Material north;
	public final Material south;
	public final Material east;
	public final Material west;

	public Signature(Material north, Material south, Material east, Material west) {
		this.north = north;
		this.south = south;
		this.east = east;
		this.west = west;
	}

	public static Signature fromLocation(Location loc) {
		Material north = loc.clone().add(0, 0, -1).getBlock().getType();
		Material south = loc.clone().add(0, 0, 1).getBlock().getType();
		Material east = loc.clone().add(1, 0, 0).getBlock().getType();
		Material west = loc.clone().add(-1, 0, 0).getBlock().getType();
		Signature sig = new Signature(north, south, east, west);
		return sig;
	}
	
	public boolean equals(Signature sig) {
		boolean match = (sig.north == north)
					 && (sig.south == south)
					 && (sig.east == east)
					 && (sig.west == west);
		return match;
	}
	
	public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
		if (obj instanceof Signature) {
			return equals((Signature)obj);
		}
		
		return false;
	}
	
//	public int hashCode() {
//        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
//        	append(northBlock).
//        	append(southBlock).
//        	append(eastBlock).
//        	append(westBlock).
//            toHashCode();
//    }
	
}
