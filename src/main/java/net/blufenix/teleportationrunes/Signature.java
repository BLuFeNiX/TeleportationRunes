package net.blufenix.teleportationrunes;

import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the 4 blocks that act as the unique key between a waypoint and its teleporters
 */
public class Signature {

	public final String north;
	public final String south;
	public final String east;
	public final String west;

	public Signature(String north, String south, String east, String west) {
		this.north = north;
		this.south = south;
		this.east = east;
		this.west = west;
	}

    public static Signature fromLocation(Location loc, Blueprint.RotatedBlueprint blueprint) {
		Location tempLoc = loc.clone().subtract(blueprint.clickableVector);

		String north = tempLoc.clone().add(blueprint.signatureVectors[0]).getBlock().getType().name();
		String south = tempLoc.clone().add(blueprint.signatureVectors[1]).getBlock().getType().name();
		String east = tempLoc.clone().add(blueprint.signatureVectors[2]).getBlock().getType().name();
		String west = tempLoc.clone().add(blueprint.signatureVectors[3]).getBlock().getType().name();

		return new Signature(north, south, east, west);
    }
	
	public boolean equals(Signature sig) {
        return sig.north.equals(north)
				&& sig.south.equals(south)
				&& sig.east.equals(east)
				&& sig.west.equals(west);
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		return obj instanceof Signature && equals((Signature) obj);
	}

    public List<String> asLore() {
        return Arrays.asList(north, south, east, west);
    }

    public static Signature fromLore(List<String> encoded) {
	    if (encoded.size() != 4) return null;

	    String n = encoded.get(0);
        String s = encoded.get(1);
        String e = encoded.get(2);
        String w = encoded.get(3);

        return new Signature(n, s, e, w);
    }
}
