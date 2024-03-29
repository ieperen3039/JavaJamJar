package NG.GameMap;

import NG.Camera.Camera;
import NG.CollisionDetection.BoundingBox;
import NG.Core.Game;
import NG.DataStructures.Generic.AveragingQueue;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Direction;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.GLFWWindow;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Settings.Settings;
import NG.Storable;
import NG.Tools.AStar;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static NG.Settings.Settings.TILE_SIZE;
import static NG.Settings.Settings.TILE_SIZE_Z;

/**
 * @author Geert van Ieperen created on 3-2-2019.
 */
public class TileMap extends AbstractMap {
    private final int chunkSize;
    private final float realChunkSize;
    private final List<GameMap.ChangeListener> changeListeners;

    private int xChunks = 0;
    private int yChunks = 0;
    private MapChunk[][] map;
    private Game game;

    private Collection<MapChunk> highlightedChunks = new HashSet<>();
    private AveragingQueue culledChunks = new AveragingQueue(30);

    public TileMap(int chunkSize) {
        this.chunkSize = chunkSize;
        this.realChunkSize = chunkSize * Settings.TILE_SIZE;
        map = new MapChunk[0][0];
        changeListeners = new ArrayList<>();
    }

    @Override
    public void init(Game game) {
        this.game = game;
        Logger.printOnline(() -> "culled chunks: " + culledChunks.average());
    }

    @Override
    public void generateNew(MapGenerator mapGenerator) {
        // height map generation
        float[][] heightmap = mapGenerator.generateHeightMap();
        int randomSeed = mapGenerator.getMapSeed();

        if (heightmap.length == 0) throw new IllegalArgumentException("Received map with 0 size in x direction");

        int xChunks = (heightmap.length - 1) / chunkSize;
        int yChunks = (heightmap[0].length - 1) / chunkSize;

        MapChunk[][] newMap = new MapChunk[xChunks][yChunks];
        for (int mx = 0; mx < xChunks; mx++) {
            for (int my = 0; my < yChunks; my++) {
                int fromY = my * chunkSize;
                int fromX = mx * chunkSize;
                MapChunkArray chunk = new MapChunkArray(chunkSize, heightmap, fromX, fromY, randomSeed);

                newMap[mx][my] = chunk;
            }
        }

        synchronized (this) {
            this.map = newMap;
            this.xChunks = xChunks;
            this.yChunks = yChunks;
        }
        changeListeners.forEach(GameMap.ChangeListener::onMapChange);
    }



    @Override
    public int getHeightAt(int x, int y) {
        int cx = x / chunkSize;
        int cy = y / chunkSize;

        if (cx >= xChunks || cx < 0 || cy >= yChunks || cy < 0) {
            return 0;
        }

        MapChunk chunk = map[cx][cy];

        return chunk == null ? 0 : chunk.getHeightAt(x - cx * chunkSize, y - cy * chunkSize);
    }

    @Override
    public Vector2i getCoordinate(Vector3fc position) {
        return new Vector2i(
                (int) (position.x() / TILE_SIZE),
                (int) (position.y() / TILE_SIZE)
        );
    }

    @Override
    public Vector3f getPosition(int x, int y) {
        return new Vector3f(
                (x + 0.5f) * TILE_SIZE, // middle of the tile
                (y + 0.5f) * TILE_SIZE,
                getHeightAt(x, y) * TILE_SIZE_Z
        );
    }

    @Override
    public float getHeightAt(float x, float y) {
        int ix = (int) (x / TILE_SIZE);
        int iy = (int) (y / TILE_SIZE);
        MapTile.Instance tile = getTileData(ix, iy);
        if (tile == null) return 0;

        float rayStartHeight = tile.type.getBoundingBox().maxZ + tile.offset * TILE_SIZE_Z + 1;
        float f = tile.intersectFraction(
                new Vector2f((ix + 0.5f) * TILE_SIZE, (iy + 0.5f) * TILE_SIZE),
                new Vector3f(x, y, rayStartHeight),
                new Vector3f(0, 0, -1)
        );

        return rayStartHeight - f;
    }

    @Override
    public void draw(SGL gl) {
        if (map == null) return;
        assert gl.getPosition(Vectors.O).equals(Vectors.O) : "gl object not placed at origin";

        ShaderProgram shader = gl.getShader();
        boolean isMaterialShader = shader instanceof MaterialShader;
        if (isMaterialShader) {
            ((MaterialShader) shader).setMaterial(Material.ROUGH, new Color4f(85, 153, 0, 1));
        }

        GLFWWindow window = game.get(GLFWWindow.class);
        Matrix4f viewProjection = game.get(Camera.class)
                .getViewProjection((float) window.getWidth() / window.getHeight());
        FrustumIntersection fic = new FrustumIntersection().set(viewProjection, false);
        int numOfCulled = 0;

        synchronized (this) {
            gl.pushMatrix();
            {
                // tile 1 stretches from (0, 0) to (TILE_SIZE, TILE_SIZE)
                gl.translate(TILE_SIZE * 0.5f, TILE_SIZE * 0.5f, 0);

                for (int x = 0; x < map.length; x++) {
                    MapChunk[] chunks = map[x];

                    gl.pushMatrix();
                    {
                        for (int y = 0; y < chunks.length; y++) {
                            MapChunk chunk = chunks[y];
                            MapChunk.Extremes minMax = chunk.getMinMax();
                            boolean isVisible = fic.testAab(
                                    x * realChunkSize, y * realChunkSize, minMax.getMin(),
                                    (x + 1) * realChunkSize, (y + 1) * realChunkSize, minMax.getMax()
                            );

                            if (isVisible) {
                                chunk.setHighlight(isMaterialShader);
                                chunk.draw(gl);
                            } else {
                                numOfCulled++;
                            }

                            gl.translate(0, realChunkSize, 0);
                        }
                    }
                    gl.popMatrix();
                    gl.translate(realChunkSize, 0, 0);
                }
            }
            gl.popMatrix();

            culledChunks.add(numOfCulled);
        }
    }

    public CopyGenerator cornerCopyGenerator() {
        return new CopyGenerator(this) {
            @Override
            public float[][] generateHeightMap() {
                progress = 0;
                int mapXSize = chunkSize * xChunks;
                int mapYSize = chunkSize * yChunks;
                setSize(mapXSize + 1, mapYSize + 1);
                float[][] floats = new float[mapXSize + 1][mapYSize + 1];

                for (int x = 0; x < mapXSize; x++) {
                    for (int y = 0; y < mapYSize; y++) {
                        MapTile.Instance tile = getTileData(x, y);
                        floats[x][y] = tile.heightOfCorner(false, false);
                        progress++;
                    }

                    MapTile.Instance tile = getTileData(x, mapYSize - 1);
                    floats[x][mapYSize] = tile.heightOfCorner(false, true);
                    progress++;
                }

                for (int y = 0; y < mapYSize; y++) {
                    MapTile.Instance tile = getTileData(mapXSize - 1, y);
                    floats[mapXSize][y] = tile.heightOfCorner(true, false);
                    progress++;
                }

                MapTile.Instance tile = getTileData(mapXSize - 1, mapYSize - 1);
                floats[mapXSize][mapYSize] = tile.heightOfCorner(true, true);
                progress++;

                return floats;
            }
        };
    }

    @Override
    public BoundingBox hitbox() {
        Vector2ic size = getSize();
        return new BoundingBox(0, 0, -100, size.x(), size.y(), 100);
    }

    @Override
    public void addChangeListener(GameMap.ChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void setHighlights(Vector2ic... coordinates) {
        highlightedChunks.forEach(MapChunk::clearHighlight);
        highlightedChunks.clear();

        for (Vector2ic c : coordinates) {
            int cx = c.x() / chunkSize;
            int cy = c.y() / chunkSize;

            if (cx >= xChunks || cx < 0 || cy >= yChunks || cy < 0) continue;

            MapChunk chunk = map[cx][cy];
            highlightedChunks.add(chunk);

            int rx = c.x() - cx * chunkSize;
            int ry = c.y() - cy * chunkSize;

            chunk.highlight(rx, ry);
        }
    }

    @Override
    public Vector2ic getSize() {
        return new Vector2i(xChunks * chunkSize, yChunks * chunkSize);
    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc) {
        return checkMouseClick(tool, xSc, ySc, game);
    }

    @Override
    public void cleanup() {
        changeListeners.clear();
        highlightedChunks.clear();
    }

    @Override
    public void writeToDataStream(DataOutputStream out) throws IOException {
        List<MapTile> tileTypes = MapTiles.values();

        // number of tile types
        int nrOfTileTypes = tileTypes.size();
        out.writeInt(nrOfTileTypes);

        // write all tiles in order
        for (MapTile tileType : tileTypes) {
            out.writeInt(tileType.tileID);
            out.writeUTF(tileType.toString());
            out.writeUTF(String.valueOf(tileType.sourceSet));
            out.writeInt(tileType.orientationBits());
        }

        synchronized (this) {
            out.writeInt(chunkSize);
            out.writeInt(xChunks);
            out.writeInt(yChunks);

            // now write the chunks themselves
            for (MapChunk[] mapChunks : map) {
                for (MapChunk chunk : mapChunks) {
                    chunk.writeToStream(out);
                }
            }
        }
    }

    /**
     * Constructs an instance from a data stream. Must be executed on the render thread for loading tile models.
     * @param in the data stream synchonized to the call to {@link Storable#writeToDataStream(DataOutputStream)}
     * @throws IOException if the data produces unexpected values
     */
    public TileMap(DataInputStream in) throws IOException {

        // get number of tile types
        int nrOfTileTypes = in.readInt();
        Map<Integer, MapTile> types = new HashMap<>(nrOfTileTypes);

        // read tiles in order
        for (int i = 0; i < nrOfTileTypes; i++) {
            int tileID = in.readInt();
            String name = in.readUTF();
            String set = in.readUTF();
            int orientation = in.readInt();

            if (!set.equals("null")) {
                TileThemeSet.valueOf(set).load();
            }
            MapTile presumedTile = MapTiles.getByName(name);

            if (presumedTile == MapTile.DEFAULT_TILE) {
                types.put(tileID, MapTiles.getByOrientationBits(orientation, 0));

            } else if (presumedTile.orientationBits() == orientation) {
                types.put(tileID, presumedTile);

            } else {
                Logger.ASSERT.printf("Closest match for %s (%s) did not have correct rotation. Defaulting to a plain tile",
                        name, presumedTile
                );
                types.put(tileID, MapTiles.getByOrientationBits(orientation, 0));
            }
        }

        chunkSize = in.readInt();
        this.realChunkSize = chunkSize * Settings.TILE_SIZE;
        this.xChunks = in.readInt();
        this.yChunks = in.readInt();

        Logger.DEBUG.printf("Tilemap: %s x %s", chunkSize * xChunks, chunkSize * yChunks);

        // now read chunks themselves
        map = new MapChunk[xChunks][];
        for (int mx = 0; mx < xChunks; mx++) {
            MapChunk[] yStrip = new MapChunk[yChunks];

            for (int my = 0; my < yChunks; my++) {
                MapChunkArray chunk = new MapChunkArray(chunkSize);
                chunk.readFromStream(in, types);
                yStrip[my] = chunk;
            }
            map[mx] = yStrip;
        }

        changeListeners = new ArrayList<>();
    }

    public MapTile.Instance getTileData(int x, int y) {
        if (x < 0 || y < 0) return null;

        int cx = x / chunkSize;
        int cy = y / chunkSize;

        if (cx >= xChunks || cy >= yChunks) return null;

        MapChunk chunk = map[cx][cy];

        int rx = x - cx * chunkSize;
        int ry = y - cy * chunkSize;
        return chunk.get(rx, ry);
    }

    public void setTile(int x, int y, MapTile.Instance instance) {
        int cx = x / chunkSize;
        int cy = y / chunkSize;

        if (cx >= xChunks || cx < 0 || cy >= yChunks || cy < 0) return;

        MapChunk chunk = map[cx][cy];

        int rx = x - cx * chunkSize;
        int ry = y - cy * chunkSize;

        chunk.set(rx, ry, instance);

        changeListeners.forEach(GameMap.ChangeListener::onMapChange);
    }

    public void replaceTile(int x, int y, MapTile newShape) {
        MapTile.Instance oldTile = getTileData(x, y);
        setTile(x, y, oldTile.replaceWith(newShape));
    }

    @Override
    public Collection<Vector2i> findPath(
            Vector2ic beginPosition, Vector2ic target, float walkSpeed, float climbSpeed
    ) {
        int xMax = (xChunks * chunkSize) - 1;
        int yMax = (yChunks * chunkSize) - 1;

        return new AStar(beginPosition, target, xMax, yMax) {
            @Override
            public float distanceAdjacent(int x1, int y1, int x2, int y2) {
                // the total duration of this movement
                float duration = 0;

                if (x1 == x2 || y1 == y2) {
                    MapTile.Instance fromTile = getTileData(x1, y1);
                    MapTile.Instance toTile = getTileData(x2, y2);
                    assert fromTile != null && toTile != null;

                    Direction move = Direction.get(x2 - x1, y2 - y1);

                    int fromHeight = fromTile.heightOf(move);
                    int toHeight = toTile.heightOf(move.inverse());

                    // steepness
                    float t1inc = (fromHeight - fromTile.getHeight()) / (TILE_SIZE / 2);
                    float t2inc = (toHeight - toTile.getHeight()) / (TILE_SIZE / 2);

                    // actual duration of walking
                    float walkSpeedT1 = (1f / hypoLength(t1inc)) * walkSpeed;
                    float walkSpeedT2 = (1f / hypoLength(t2inc)) * walkSpeed;

                    // duration += walkspeed(steepness_1) * dist + walkspeed(steepness_2) * dist
                    duration += (walkSpeedT1 + walkSpeedT2) * TILE_SIZE;

                    // height difference on the sides of the tiles
                    float cliffHeight = (toHeight - fromHeight) * TILE_SIZE_Z;

                    // climbing if more than 'an acceptable height'
                    if (cliffHeight > 0) {
                        duration += (cliffHeight / climbSpeed);
                    }

                } else {
                    // TODO allow diagonal tracing
                    Logger.WARN.print(String.format(
                            "Pathfinding (%s) asked for non-adjacent tiles (%d, %d) (%d, %d)",
                            getClass(), x1, y1, x2, y2
                    ));

                    return Float.POSITIVE_INFINITY;
                }

                return duration;
            }

        }.call();
    }

    @Override
    public Float getTileIntersect(Vector3fc origin, Vector3fc direction, int xCoord, int yCoord) {
        MapTile.Instance tileData = getTileData(xCoord, yCoord);

        if (tileData == null) {
            Logger.WARN.print(String.format(
                    "%s is not on the map",
                    Vectors.asVectorString(xCoord, yCoord)
            ));
            return null;

        } else {
            Vector2f tilePosition = new Vector2f((xCoord + 0.5f) * TILE_SIZE, (yCoord + 0.5f) * TILE_SIZE);
            return tileData.intersectFraction(tilePosition, origin, direction);
        }
    }
}
