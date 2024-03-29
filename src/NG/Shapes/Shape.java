package NG.Shapes;

import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.MeshLoading.MeshFile;
import NG.Shapes.Primitives.Plane;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.AABBf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Geert van Ieperen created on 30-10-2017.
 */
public interface Shape {

    /** @return all planes of this shape. The order of planes is consistent */
    List<? extends Plane> getPlanes();

    /** @return the points of this shape. The order of planes is consistent */
    List<Vector3fc> getPoints();

    /**
     * given a point on position {@code origin} and a direction of {@code direction}, calculates the fraction t such
     * that (origin + direction * t) lies on this shape, or Float.POSITIVE_INFINITY if it does not hit.
     * @param origin    the begin of a line segment
     * @param direction the direction of the line segment
     * @return the scalar t
     */
    default float getIntersectionScalar(Vector3fc origin, Vector3fc direction) {
        float least = Float.POSITIVE_INFINITY;

        for (Plane plane : getPlanes()) {
            float intersectionScalar = plane.getIntersectionScalar(origin, direction);

            if (intersectionScalar < least) {
                least = intersectionScalar;
            }
        }

        return least;
    }

    AABBf getBoundingBox();

    /**
     * loads a mesh, splitting it into sections of size containersize.
     * @param containerSize size of splitted container, which is applied in 3 dimensions
     * @param scale         possible scaling factor upon loading
     * @param path          path to the .obj file without extension
     * @return a list of shapes, each being roughly containersize in size
     */
    static List<Shape> loadSplit(float containerSize, Vector3fc scale, Path path)
            throws IOException {
        MeshFile file = MeshFile.loadFile(path, Vectors.O, scale);
        HashMap<Vector3i, CustomShape> world = new HashMap<>();

        for (Mesh.Face f : file.getFaces()) {
            Vector3fc[] edges = new Vector3fc[f.size()];
            Vector3f minimum = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
            for (int i = 0; i < f.size(); i++) {
                Vector3fc p = file.getVertices().get(f.vert[i]);
                minimum.min(p);
                edges[i] = p;
            }

            int x = (int) (minimum.x / containerSize);
            int y = (int) (minimum.y / containerSize);
            int z = (int) (minimum.z / containerSize);

            Vector3i key = new Vector3i(x, y, z);
            CustomShape container = world.computeIfAbsent(key, k ->
                    new CustomShape(new Vector3f(x + 0.5f, y + 0.5f, -Float.MAX_VALUE))
            );

            Vector3f normal = new Vector3f();
            for (int ind : f.norm) {
                if (ind < 0) continue;
                normal.add(file.getNormals().get(ind));
            }
            if (Vectors.isScalable(normal)) {
                normal.normalize();
            } else {
                normal = null;
                Logger.DEBUG.printSpamless(file.toString(), file + " has at least one not-computed normal");
            }

            container.addPlane(normal, edges);
        }

        Collection<CustomShape> containers = world.values();
        Logger.DEBUG.print("Loaded model " + file + " in " + containers.size() + " parts");

        List<Shape> shapes = new ArrayList<>();
        for (CustomShape frame : containers) {
            shapes.add(frame.toShape());
        }
        return shapes;
    }
}
