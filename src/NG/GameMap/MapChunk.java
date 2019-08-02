package NG.GameMap;

import NG.Core.GameTimer;
import NG.Rendering.MatrixStack.SGL;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 3-2-2019.
 */
public interface MapChunk {
    /**
     * @param x the x coordinate relative to this chunk
     * @param y the y coordinate relative to this chunk
     * @return the MapTile on the given coordinate
     */
    MapTile.Instance get(int x, int y);

    /**
     * sets a given position to a new tile
     * @param x the x coordinate relative to this chunk
     * @param y the y coordinate relative to this chunk
     * @return the previous tile, or null if there was none
     */
    MapTile set(int x, int y, MapTile.Instance tile);

    /**
     * return the height of the MapTile on the given position
     * @param x the x coordinate relative to this chunk
     * @param y the y coordinate relative to this chunk
     * @return the base height / elevation of the tile on the given coordinate, or 0 when the coordinate is out of
     * bounds.
     */
    int getHeightAt(int x, int y);

    /**
     * Draws this chunk using the provided SGL object. This method may only be called from the rendering loop, and
     * should not change the internal representation of this object. Possible animations should be based on {@link
     * GameTimer#getRendertime()}. Material must be set using {@link SGL#getShader()}.
     * @param gl the graphics object to be used for rendering. It is initialized at world's origin. (no translation or
     *           scaling has been applied)
     */
    void draw(SGL gl);

    /**
     * adds a highlight to the given relative coordinate in this chunk
     * @param x the x coordinate relative to this chunk
     * @param y the y coordinate relative to this chunk
     */
    void highlight(int x, int y);

    /**
     * toggles whether the tiles previously set by {@link #highlight(int, int)} should be highlighted or not. Calls to
     * this method do not change the set of tiles that are highlighted.
     * @param doHighlight when true, all highlighted tiles are made visible,. when false, no highlighted tiles are
     *                    visible.
     */
    void setHighlight(boolean doHighlight);

    /**
     * clears all highlights off the tiles.
     */
    void clearHighlight();

    /**
     * write the data to the given stream, such that this chunk can be recreated using the same constructor as this
     * object, and a call to {@link #readFromStream(DataInputStream, Map)}. Note that the tile type is stored by id, and
     * a mapping from id to tile type must be stored separately.
     * @param out the output stream
     * @throws IOException if an exception occurs while writing. The state is then undetermined.
     */
    void writeToStream(DataOutputStream out) throws IOException;

    /**
     * read the data to the given stream, inverting a call to {@link #writeToStream(DataOutputStream)}.
     * @param in      the output stream
     * @param mapping maps a tile id to the tile implementation.
     * @throws IOException if an exception occurs while writing. The state is then undetermined.
     */
    void readFromStream(DataInputStream in, Map<Integer, MapTile> mapping) throws IOException;

    /**
     * @return an object holding the lowest and highest z-values that the surface of this chunk has.
     */
    Extremes getMinMax();

    class Extremes {
        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;

        public void check(float value) {
            if (value > max) max = value;
            if (value < min) min = value;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }
}
