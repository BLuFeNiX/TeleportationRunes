package net.blufenix.teleportationrunes;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

/**
 * Created by blufenix on 8/2/15.
 */
public class Blueprint {

    private static final String SIGNATURE_BLOCK_1 = "SIGNATURE_BLOCK_1";
    private static final String SIGNATURE_BLOCK_2 = "SIGNATURE_BLOCK_2";
    private static final String SIGNATURE_BLOCK_3 = "SIGNATURE_BLOCK_3";
    private static final String SIGNATURE_BLOCK_4 = "SIGNATURE_BLOCK_4";

    private Blueprint.Block[][][] materialMatrix;
    private Vector[] vectors;

    public Blueprint(ConfigurationSection blueprintSection, ConfigurationSection materialsSection) {
        Map<String, Object> materials = materialsSection.getValues(false);
        List<List<List<String>>> layerList = (List<List<List<String>>>) blueprintSection.getList("layers");

        materialMatrix = new Blueprint.Block[layerList.size()][][];
        for (int i = 0; i < layerList.size(); i++) {
            List<List<String>> layer = layerList.get(i);
            materialMatrix[i] = new Blueprint.Block[layer.size()][];
            for (int j = 0; j < layer.size(); j++) {
                List<String> row = layer.get(j);
                materialMatrix[i][j] = new Blueprint.Block[row.size()];
                for (int k = 0; k < row.size(); k++) {
                    String materialID = row.get(k); // user created string that represents material
                    String materialName = (String) materials.get(materialID);
                    materialMatrix[i][j][k] = new Blueprint.Block(materialName);
                }
            }
        }

        // config file structure is a different orientation that the actual game
        // so, rotate and reverse each layer
        for (int i = 0; i < materialMatrix.length; i++) {
            materialMatrix[i] = rotateMatrixLeft(materialMatrix[i]);
            ArrayUtils.reverse(materialMatrix[i]);
        }

        vectors = new Vector[5];
        vectors[0] = blueprintSection.getVector("clickableBlock");

        for (int i = 0; i < materialMatrix.length; i++) {
            for (int j = 0; j < materialMatrix[i].length; j++) {
                for (int k = 0; k < materialMatrix[i][j].length; k++) {
                    Blueprint.Block bb = materialMatrix[i][j][k];
                    Material material = bb.getMaterial();

                    if (material == null) {
                        switch (bb.getMaterialName()) {
                            case SIGNATURE_BLOCK_1:
                                vectors[1] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_2:
                                vectors[2] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_3:
                                vectors[3] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_4:
                                vectors[4] = new Vector(j, -i, k);
                                break;
                        }
                    }
                }
            }
        }
    }

    private static Blueprint.Block[][] rotateMatrixLeft(Blueprint.Block[][] matrix) {
    /* W and H are already swapped */
        int w = matrix.length;
        int h = matrix[0].length;
        Blueprint.Block[][] ret = new Blueprint.Block[h][w];
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                ret[i][j] = matrix[j][h - i - 1];
            }
        }
        return ret;
    }

//    private static Blueprint.Block[][] rotateMatrixRight(Blueprint.Block[][] matrix) {
//    /* W and H are already swapped */
//        int w = matrix.length;
//        int h = matrix[0].length;
//        Blueprint.Block[][] ret = new Blueprint.Block[h][w];
//        for (int i = 0; i < h; ++i) {
//            for (int j = 0; j < w; ++j) {
//                ret[i][j] = matrix[w - j - 1][i];
//            }
//        }
//        return ret;
//    }

    public Vector[] getVectors() {
        return vectors;
    }

    public Blueprint.Block[][][] getMaterialMatrix() {
        return materialMatrix;
    }

    protected static class Block {

        private String materialName;
        private Material material;

        Block(String materialName) {
            this.materialName = materialName;
            Material material = Material.matchMaterial(materialName);
            this.material = material;
        }

        public String getMaterialName() {
            return materialName;
        }

        public Material getMaterial() {
            return material;
        }
    }
}
