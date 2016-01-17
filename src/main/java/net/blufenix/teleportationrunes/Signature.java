package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

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

    public static Signature fromLocation(Location loc, Blueprint.RotatedBlueprint blueprint) {
		Vector clickVector = blueprint.getClickableBlockVector();
		Vector[] sigVectors = blueprint.getSignatureVectors();

        BlockState north = loc.clone().subtract(clickVector).add(sigVectors[0]).getBlock().getState();
        BlockState south = loc.clone().subtract(clickVector).add(sigVectors[1]).getBlock().getState();
        BlockState east = loc.clone().subtract(clickVector).add(sigVectors[2]).getBlock().getState();
        BlockState west = loc.clone().subtract(clickVector).add(sigVectors[3]).getBlock().getState();
        TeleportationRunes.getInstance().getLogger().info(north.getBlock().getType().toString());
        TeleportationRunes.getInstance().getLogger().info(south.getBlock().getType().toString());
        TeleportationRunes.getInstance().getLogger().info(east.getBlock().getType().toString());
        TeleportationRunes.getInstance().getLogger().info(west.getBlock().getType().toString());
        Signature sig = new Signature(north, south, east, west);
        return sig;
    }
	
	public boolean equals(Signature sig) {
		boolean match = (sig.north.getType().equals(north.getType()))
					 && (sig.south.getType().equals(south.getType()))
					 && (sig.east.getType().equals(east.getType()))
					 && (sig.west.getType().equals(west.getType()));
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
