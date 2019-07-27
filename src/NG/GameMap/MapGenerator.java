package NG.GameMap;

import java.util.Map;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface MapGenerator {

    /**
     * get a list of properties of this generator which can be changed. Units and range should be given in the name. The
     * properties do NOT include the seed of the generator.
     * @return a list of properties and their current value. Changes in this list are reflected to the generator.
     * @see #getMapSeed()
     */
    Map<String, Integer> getProperties();

    /**
     * generate a heightmap which will be used to render the world.
     * @return the new heightmap, of size (xSize x ySize)
     */
    float[][] generateHeightMap();

    /**
     * @return the seed used to create the map
     */
    int getMapSeed();

    void setXSize(int xSize);

    void setYSize(int ySize);

    default void setSize(int x, int y) {
        setXSize(x);
        setYSize(y);
    }
}
