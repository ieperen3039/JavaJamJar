package NG.Entities;

import NG.CollisionDetection.BoundingBox;
import NG.Core.GameTimer;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 26-7-2019.
 */
public interface Entity {
    /**
     * Draws this entity using the provided SGL object. This method may only be called from the rendering loop, and
     * should not change the internal representation of this object. Possible animations should be based on {@link
     * GameTimer#getRendertime()}. Material must be set using {@link SGL#getShader()}.
     * @param gl the graphics object to be used for rendering. It is initialized at world's origin. (no translation or
     *           scaling has been applied)
     */
    void draw(SGL gl);

    /**
     * updates the {@link #getCurrentState()} of this entity to the given gameTime
     * @param gameTime the current game time
     * @see State#update(float)
     */
    void update(float gameTime);

    /**
     * @return the state of this entity at the latest physics update
     */
    State getCurrentState();

    /**
     * get the state of this entity on the given moment in time, interpolating and extrapolating linearly when
     * necessary. The resulting state is only definite if {@code gameTime} is less than that of {@link
     * #getCurrentState()}{@link State#time() .time()}
     * @param gameTime the time where the state must be queried
     * @return the state at the given time using linear interpolation
     * @implNote default always returns the current state
     */
    default State getStateAt(float gameTime) {
        return getCurrentState();
    }

    /**
     * @return the relative (local-space) bounding box of this entity
     */
    BoundingBox hitbox();

    /**
     * given a point on position {@code origin} and a direction of {@code direction}, calculates the fraction t in [0
     * ... 1] such that (origin + direction * t) is the first point on this entity
     * @param origin    the origin of the line
     * @param direction the direction and extend of the line
     * @return the value t such that (origin + direction * t) is the first point on this entity, or 1 if it does not
     * hit.
     */
    float getIntersection(Vector3fc origin, Vector3fc direction);

    /**
     * returns the points of the shape of this entity as consistent with {@link #getCurrentState()}
     * @return a list of the exact wolrd-positions of the vertices of the shape of this object. Changes in the list are
     * not reflected in this object.
     */
    default List<Vector3f> getShapePoints() {
        return getShapePoints(new ArrayList<>());
    }

    /**
     * returns the points of the shape of this entity as consistent with {@link #getCurrentState()} and store the result
     * in the vectors of dest.
     * @param dest a list of vectors. If the result requires more or less elements, the redundant elements are deleted,
     *             and required elements are added.
     * @return a list of the exact wolrd-positions of the vertices of the shape of this object. Changes in the list are
     * not reflected in this object.
     */
    List<Vector3f> getShapePoints(List<Vector3f> dest);

    /**
     * @param other another entity
     * @return false if this entity does not respond on a collision with the other entity. In that case, the other
     * entity should also not respond on a collision with this.
     */
    default boolean canCollideWith(Entity other) {
        return (other != this && other instanceof MovingEntity);
    }

    /**
     * process a collision with the other entity, happening at collisionTime. The other entity will be called with this
     * same function, as {@code other.collideWith(this, collisionTime)}. The effect is that this entity's {@link
     * #getCurrentState()} gets updated to {@code collisionTime}
     * <p>
     * Should not be called if either {@code this.}{@link #canCollideWith(Entity) canCollideWith}{@code (other)} or
     * {@code other.}{@link #canCollideWith(Entity) canCollideWith}{@code (this)}
     * @param other         another entity
     * @param collisionTime the moment of collision
     */
    void collideWith(Entity other, float collisionTime);

    /**
     * Marks the entity to be invalid, such that the {@link #isDisposed()} method returns true.
     */
    void dispose();

    /**
     * @return true iff this unit should be removed from the game world.
     */
    boolean isDisposed();
}
