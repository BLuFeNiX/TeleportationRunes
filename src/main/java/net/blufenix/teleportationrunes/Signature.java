package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

/**
 * Represents the 4 blocks that act as the unique key between a waypoint and its teleporters
 */
public class Signature {

	public final BlockState north;
	public final BlockState south;
	public final BlockState east;
	public final BlockState west;

	public Signature(BlockState north, BlockState south, BlockState east, BlockState west) {
		this.north = north;
		this.south = south;
		this.east = east;
		this.west = west;
	}

	public static Signature fromLocation(Location loc) {
		BlockState north = loc.clone().add(0, 0, -1).getBlock().getState();
        BlockState south = loc.clone().add(0, 0, 1).getBlock().getState();
        BlockState east = loc.clone().add(1, 0, 0).getBlock().getState();
        BlockState west = loc.clone().add(-1, 0, 0).getBlock().getState();
		Signature sig = new Signature(north, south, east, west);
		return sig;
	}
	
	public boolean equals(Signature sig) {
		boolean match = (sig.north.equals(north))
					 && (sig.south.equals(south))
					 && (sig.east.equals(east))
					 && (sig.west.equals(west));
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
	
}
