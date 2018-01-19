package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.util.logging.Logger;

/**
 * Represents the 4 blocks that act as the unique key between a waypoint and its teleporters
 */
public class Signature {

	private static final boolean DEBUG = false;

	public final BlockState north;
	public final BlockState south;
	public final BlockState east;
	public final BlockState west;

	private Signature(BlockState north, BlockState south, BlockState east, BlockState west) {
		this.north = north;
		this.south = south;
		this.east = east;
		this.west = west;
	}

    public static Signature fromLocation(Location loc, Blueprint.RotatedBlueprint blueprint) {
		Location tempLoc = loc.clone().subtract(blueprint.clickableVector);

		BlockState north = tempLoc.clone().add(blueprint.signatureVectors[0]).getBlock().getState();
		BlockState south = tempLoc.clone().add(blueprint.signatureVectors[1]).getBlock().getState();
		BlockState east = tempLoc.clone().add(blueprint.signatureVectors[2]).getBlock().getState();
		BlockState west = tempLoc.clone().add(blueprint.signatureVectors[3]).getBlock().getState();
        if (DEBUG) {
        	Logger log = TeleportationRunes.getInstance().getLogger();
			log.info(north.toString());
			log.info(south.toString());
			log.info(east.toString());
			log.info(west.toString());
		}
		return new Signature(north, south, east, west);
    }
	
	public boolean equals(Signature sig) {
        return sig.north.getType() == north.getType()
                && sig.south.getType() == south.getType()
                && sig.east.getType() == east.getType()
                && sig.west.getType() == west.getType()
                && sig.north.getData().getData() == north.getData().getData()
                && sig.south.getData().getData() == south.getData().getData()
                && sig.east.getData().getData() == east.getData().getData()
                && sig.west.getData().getData() == west.getData().getData();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		return obj instanceof Signature && equals((Signature) obj);
	}
	
}
