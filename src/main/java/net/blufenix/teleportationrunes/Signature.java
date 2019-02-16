package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the 4 blocks that act as the unique key between a waypoint and its teleporters
 */
public class Signature {

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

//		Log.d("NSEW: %s, %s, %s, %s", north.toString(), south.toString(), east.toString(), west.toString());

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

	public String getEncoded() {
		StringBuilder sb = new StringBuilder();
		for (BlockState blockState : new BlockState[]{north, south, east, west}) {
			sb.append(blockState.getType().name());
			sb.append(":");
			sb.append(new String(new byte[]{blockState.getData().getData()}, Charset.defaultCharset()));
			sb.append(";");
		}
		return sb.toString();
	}

    public List<String> getLoreEncoding() {
        List<String> encoded = new ArrayList<>();
        StringBuilder blockDataSb = new StringBuilder();
        for (BlockState blockState : new BlockState[]{north, south, east, west}) {
            encoded.add(blockState.getType().name());
            blockDataSb.append(new String(new byte[]{blockState.getData().getData()}, Charset.defaultCharset()));
        }
        encoded.add(blockDataSb.toString());
        return encoded;
    }

	// can't make one of these by implementing the interface
	// since it causes reflection issues
	private static BlockState getDonor(Material material, byte aByte) {
		BlockState donor = TeleportationRunes.getInstance().getServer().getWorld("world").getBlockAt(100, 100, 100).getState();
		donor.setType(material);
		donor.getData().setData(aByte);
		return donor;
	}

	public static Signature fromEncoded(String encoded) {
		String[] parts = encoded.split(";");
		String[] n = parts[0].split(":");
		String[] s = parts[1].split(":");
		String[] e = parts[2].split(":");
		String[] w = parts[3].split(":");

		BlockState north = getDonor(Material.getMaterial(n[0]), n[1].getBytes(Charset.defaultCharset())[0]);
		BlockState south = getDonor(Material.getMaterial(s[0]), s[1].getBytes(Charset.defaultCharset())[0]);
		BlockState east = getDonor(Material.getMaterial(e[0]), e[1].getBytes(Charset.defaultCharset())[0]);
		BlockState west = getDonor(Material.getMaterial(w[0]), w[1].getBytes(Charset.defaultCharset())[0]);

		return new Signature(north, south, east, west);
	}

    public static Signature fromLoreEncoding(List<String> encoded) {
	    if (encoded.size() != 5) return null;

	    String n = encoded.get(0);
        String s = encoded.get(1);
        String e = encoded.get(2);
        String w = encoded.get(3);
        byte[] data = encoded.get(4).getBytes(Charset.defaultCharset());

        BlockState north = getDonor(Material.getMaterial(n), data[0]);
        BlockState south = getDonor(Material.getMaterial(s), data[1]);
        BlockState east = getDonor(Material.getMaterial(e), data[2]);
        BlockState west = getDonor(Material.getMaterial(w), data[3]);

        return new Signature(north, south, east, west);
    }
}
