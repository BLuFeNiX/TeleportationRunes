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

    private final Blueprint.Block[][][] materialMatrix_0;
    private final Blueprint.Block[][][] materialMatrix_90;
    private final Blueprint.Block[][][] materialMatrix_180;
    private final Blueprint.Block[][][] materialMatrix_270;

    private final Vector[] signatureVectors_0;
    private final Vector[] signatureVectors_90;
    private final Vector[] signatureVectors_180;
    private final Vector[] signatureVectors_270;

    private final Vector clickableBlockVector_0;
    private final Vector clickableBlockVector_90;
    private final Vector clickableBlockVector_180;
    private final Vector clickableBlockVector_270;

    public Blueprint(ConfigurationSection blueprintSection, ConfigurationSection materialsSection) {
        Map<String, Object> materials = materialsSection.getValues(false);
        List<List<List<String>>> layerList = (List<List<List<String>>>) blueprintSection.getList("layers");

        materialMatrix_0 = constructMatrix(materials, layerList);
        materialMatrix_90 = rotateMatrixLeft(materialMatrix_0);
        materialMatrix_180 = rotateMatrixLeft(materialMatrix_90);
        materialMatrix_270 = rotateMatrixLeft(materialMatrix_180);

        signatureVectors_0 = findSignatureVectors(materialMatrix_0);
        signatureVectors_90 = findSignatureVectors(materialMatrix_90);
        signatureVectors_180 = findSignatureVectors(materialMatrix_180);
        signatureVectors_270 = findSignatureVectors(materialMatrix_270);

        clickableBlockVector_0 = blueprintSection.getVector("clickableBlock");
        clickableBlockVector_90 = rotateVectorLeft(clickableBlockVector_0, materialMatrix_0[0].length);
        clickableBlockVector_180 = rotateVectorLeft(clickableBlockVector_90, materialMatrix_90[0].length);
        clickableBlockVector_270 = rotateVectorLeft(clickableBlockVector_180, materialMatrix_180[0].length);

    }

    public RotatedBlueprint atRotation(int degrees) {
        switch (degrees) {
            case 0:
                return new RotatedBlueprint(materialMatrix_0, clickableBlockVector_0, signatureVectors_0);
            case 90:
                return new RotatedBlueprint(materialMatrix_90, clickableBlockVector_90, signatureVectors_90);
            case 180:
                return new RotatedBlueprint(materialMatrix_180, clickableBlockVector_180, signatureVectors_180);
            case 270:
                return new RotatedBlueprint(materialMatrix_270, clickableBlockVector_270, signatureVectors_270);
//            default:
//                throw new Exception("degrees not in {0, 90, 180, 270}");
        }
        return null;
    }

    private Vector rotateVectorLeft(Vector v, int sizeX) {
        return new Vector(v.getZ(), v.getY(), sizeX - v.getX()-1);
    }

    private Blueprint.Block[][][] constructMatrix(Map<String, Object> materials, List<List<List<String>>> layerList) {
        // create 3D array of Blueprint.Block
        Blueprint.Block[][][] matrix = new Block[layerList.size()][][];
        for (int i = 0; i < layerList.size(); i++) {
            List<List<String>> layer = layerList.get(i);
            matrix[i] = new Blueprint.Block[layer.size()][];
            for (int j = 0; j < layer.size(); j++) {
                List<String> row = layer.get(j);
                matrix[i][j] = new Blueprint.Block[row.size()];
                for (int k = 0; k < row.size(); k++) {
                    String materialCode = row.get(k); // user-created string that represents material
                    String materialName = (String) materials.get(materialCode);
                    matrix[i][j][k] = new Blueprint.Block(materialName);
                }
            }
        }

        // config file structure is a different orientation than the actual game
        // so, rotate and reverse each layer
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = rotateLayerLeft(matrix[i]);
            ArrayUtils.reverse(matrix[i]);
        }

        return matrix;
    }

    private Vector[] findSignatureVectors(Block[][][] materialMatrix) {
        // find location of signature blocks within the matrix
        Vector[] vectors = new Vector[4];

        for (int i = 0; i < materialMatrix.length; i++) {
            for (int j = 0; j < materialMatrix[i].length; j++) {
                for (int k = 0; k < materialMatrix[i][j].length; k++) {
                    Blueprint.Block bb = materialMatrix[i][j][k];
                    Material material = bb.getMaterial();

                    if (material == null) {
                        switch (bb.getMaterialName()) {
                            case SIGNATURE_BLOCK_1:
                                vectors[0] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_2:
                                vectors[1] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_3:
                                vectors[2] = new Vector(j, -i, k);
                                break;
                            case SIGNATURE_BLOCK_4:
                                vectors[3] = new Vector(j, -i, k);
                                break;
                        }
                    }
                }
            }
        }
        return vectors;
    }

    private static Blueprint.Block[][][] rotateMatrixLeft(Blueprint.Block[][][] matrix) {
        int layers = matrix.length;
        Blueprint.Block[][][] ret = new Blueprint.Block[layers][][];
        for (int i = 0; i < layers; i++) {
            ret[i] = rotateLayerLeft(matrix[i]);
        }
        return ret;
    }

    private static Blueprint.Block[][] rotateLayerLeft(Blueprint.Block[][] matrix) {
        // w and h are already swapped
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

//    private static Blueprint.Block[][] rotateLayerRight(Blueprint.Block[][] matrix) {
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

    protected static class Block {

        private String materialName;
        private Material material;

        Block(String materialName) {
            this.materialName = materialName;
            this.material = Material.matchMaterial(materialName);;
        }

        public String getMaterialName() {
            return materialName;
        }

        public Material getMaterial() {
            return material;
        }
    }

    protected static class RotatedBlueprint {

        private final Block[][][] materialMatrix;
        private final Vector clickableVector;
        private final Vector[] signatureVectors;

        RotatedBlueprint(Blueprint.Block[][][] materialMatrix, Vector clickableVector, Vector[] signatureVectors) {
            this.materialMatrix = materialMatrix;
            this.clickableVector = clickableVector;
            this.signatureVectors = signatureVectors;
        }

        public Blueprint.Block[][][] getMaterialMatrix() {
            return materialMatrix;
        }

        public Vector getClickableBlockVector() {
            return clickableVector;
        }

        public Vector[] getSignatureVectors() {
            return signatureVectors;
        }
    }
}
