package NG.Entities;

import NG.DataStructures.Vector3fx;
import NG.DataStructures.Vector3fxc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Describes the state of an entity. Simply put, this describes the position and velocity at a given time
 * @author Geert van Ieperen created on 26-7-2019.
 */
public class MutableState implements State {
    private float time;
    private Vector3fx position;
    private Vector3f velocity;
    private Quaternionf orientation;
    private Quaternionf rotationSpeed;

    public MutableState(float time, Vector3fxc position, Vector3fc velocity, Quaternionf orientation) {
        this(time, position, velocity, orientation, new Quaternionf());
    }

    public MutableState(
            float time, Vector3fxc position, Vector3fc velocity, Quaternionf orientation, Quaternionf rotationSpeed
    ) {
        this.time = time;
        this.position = new Vector3fx(position);
        this.orientation = new Quaternionf(orientation);
        this.velocity = new Vector3f(velocity);
        this.rotationSpeed = rotationSpeed;
    }

    public MutableState(float time, Vector3fxc position) {
        this.time = time;
        this.position = new Vector3fx(position);
        this.orientation = new Quaternionf();
        this.velocity = new Vector3f();
    }

    /**
     * copies the given state
     * @param source another state
     */
    public MutableState(State source) {
        this(source.time(), source.position(), source.velocity(), source.orientation(), new Quaternionf());
    }

    @Override
    public MutableState copy() {
        return new MutableState(this.time, this.position, this.velocity, this.orientation, this.rotationSpeed);
    }

    @Override
    public Vector3fxc position() {
        return position;
    }

    @Override
    public Vector3fc velocity() {
        return velocity;
    }

    @Override
    public Quaternionf orientation() {
        return orientation;
    }

    @Override
    public float time() {
        return time;
    }

    @Override
    public MutableState update(float gameTime) {
        float deltaTime = gameTime - time;

        Vector3f movement = new Vector3f(velocity).mul(deltaTime);
        position.add(movement);

        Quaternionf rotation = new Quaternionf(rotationSpeed).scale(deltaTime);
        orientation.add(rotation);

        return this;
    }

    /**
     * Adds the given velocity and rotational velocity to this state. During a physics update, the correct order of
     * updating is {@code this.add(velocityChange, rotationSpeedChange)}{@link #update(float) .update(gameTime)}.
     *
     * @param velocityChange      the change in velocity
     * @param rotationSpeedChange the change in rotation speed
     * @return this;
     */
    public MutableState add(Vector3fc velocityChange, Quaternionfc rotationSpeedChange) {
        velocity.add(velocityChange);
        rotationSpeed.add(rotationSpeedChange);

        return this;
    }

    @Override
    public MutableState interpolate(State other, float gameTime) {
        float fraction = (time - other.time()) / (time - gameTime);

        Vector3fx p = new Vector3fx(position).lerp(other.position(), fraction);
        Vector3f v = new Vector3f(velocity).lerp(other.velocity(), fraction);

        return new MutableState(gameTime, p, v, orientation, rotationSpeed);
    }
}
