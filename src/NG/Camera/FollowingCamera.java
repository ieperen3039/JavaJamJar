package NG.Camera;

import NG.CollisionDetection.PhysicsEngine;
import NG.DataStructures.Tracked.ExponentialSmoothVector;
import NG.DataStructures.Vector3fx;
import NG.DataStructures.Vector3fxc;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Entities.State;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Implementation of a camera that follows a given entity, trying to stay behind this entity
 */
public class FollowingCamera implements Camera {
    /** camera settings */
    private static final Vector3fc eyeRelative = new Vector3f(-20, 0, 5);
    private static final Vector3fc focusRelativeToEye = new Vector3f(30, 0, 0);
    private static final double EYE_PRESERVE = 1.0E-5;
    private static final double ORIENT_PRESERVE = 1.0E-4;
    private static final float TELEPORT_DISTANCE_SQ = 50f * 50f;

    /**
     * The position of the camera.
     */
    private final ExponentialSmoothVector eye;
    private final ExponentialSmoothVector vecToFocus;
    private final ExponentialSmoothVector up;
    private Entity target;
    private Game game;

    public FollowingCamera(Vector3f eye, Entity focus, Vector3f up, Vector3f vecToFocus) {
        this.eye = new ExponentialSmoothVector(eye, EYE_PRESERVE);
        this.vecToFocus = new ExponentialSmoothVector(vecToFocus, ORIENT_PRESERVE);
        this.up = new ExponentialSmoothVector(up, ORIENT_PRESERVE);
        this.target = focus;
    }

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
    }

    @Override
    public void cleanup() {
        target = null;
    }

    @Override
    public void onScroll(float value) {
        // ignore
    }

    /**
     * @param deltaTime the animation time difference
     * @param renderTime
     */
    @Override
    public void updatePosition(float deltaTime, float renderTime) {
        State state = target.getStateAt(renderTime);

        Vector3fc targetUp = Vectors.newZVector().rotate(state.orientation());
        Vector3f targetEye = new Vector3f(eyeRelative).rotate(state.orientation());
        Vector3fc targetPos = state.position().toVector3f();

        // prevent looking through walls;
        Float fraction = game.get(PhysicsEngine.class).getEntityByRay(targetPos, targetEye).right;
        if (fraction < 1) targetEye.mul(fraction);
        targetEye.add(targetPos);

        Vector3fxc targetFocus;
        // if the target is removed, look at the place where the target was
        if (target.isDisposed()) {
            targetFocus = state.position();
        } else {
            targetFocus = new Vector3fx(targetEye).add(focusRelativeToEye);
        }

        // teleport camera when too far away
        if (eye.current().distanceSquared(targetEye) > TELEPORT_DISTANCE_SQ) {
            eye.update(targetEye);
        } else {
            eye.updateFluent(targetEye, deltaTime);
        }

        vecToFocus.updateFluent(targetFocus.toVector3f(), deltaTime);
        up.updateFluent(targetUp, deltaTime);
    }

    @Override
    public Vector3fc vectorToFocus(){
        return vecToFocus.current();
    }

    @Override
    public Vector3fc getEye() {
        return eye.current();
    }

    @Override
    public Vector3fc getFocus() {
        return getEye().add(vectorToFocus(), new Vector3f());
    }

    @Override
    public Vector3fc getUpVector() {
        return up.current();
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye) {
        this.eye.update(eye);
        this.vecToFocus.update(new Vector3f(focus).sub(eye));
    }

    @Override
    public boolean isIsometric() {
        return false;
    }
}
