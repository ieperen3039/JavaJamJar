package NG.Camera;

import NG.CollisionDetection.BoundingBox;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Entities.State;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 5-2-2019.
 */
public class Cursor implements MovingEntity {
    private final Supplier<State> positionSupplier;
    private boolean isDisposed = false;

    public Cursor(Supplier<State> positionSupplier) {
        this.positionSupplier = positionSupplier;
    }

    public void update(float gameTime) {

    }

    @Override
    public State getCurrentState() {
        return positionSupplier.get();
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        gl.translate(positionSupplier.get().position().toVector3f());
        Toolbox.draw3DPointer(gl);
        gl.popMatrix();
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    @Override
    public BoundingBox hitbox() {
        return new BoundingBox(0, 0, 0, 0, 0, 0);
    }

    @Override
    public float getIntersection(Vector3fc origin, Vector3fc direction) {
        return 1;
    }

    @Override
    public List<Vector3f> getShapePoints(List<Vector3f> dest) {
        dest.clear();
        return dest;
    }

    @Override
    public void collideWith(Entity other, float collisionTime) {

    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }
}
