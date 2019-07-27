package NG.Entities;

import NG.DataStructures.Vector3fx;
import NG.DataStructures.Vector3fxc;
import NG.Tools.Vectors;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 27-7-2019.
 */
public class FixedState implements State {
    private final Vector3fxc position;
    private final Quaternionf orientation;
    private float time;

    /**
     * create a state on the given position and orientation.
     * @param position    the position of this state
     * @param orientation the orientation of this state
     */
    public FixedState(Vector3fxc position, Quaternionf orientation) {
        this(position, orientation, 0);
    }

    /**
     * create a state on the given position and orientation, representing the given time.
     * @param position    the position of this state
     * @param orientation the orientation of this state
     * @param time        the time reporesented by this state. Only influences the result of {@link #update(float)}
     *                    and {@link #interpolate(State, float)}l
     */
    public FixedState(Vector3fxc position, Quaternionf orientation, float time) {
        this.position = new Vector3fx(position);
        this.orientation = new Quaternionf(orientation);
        this.time = time;
    }

    /**
     * create a fixed copy of the given state
     * @param source
     */
    public FixedState(State source) {
        this(source.position(), source.orientation(), source.time());
    }

    @Override
    public State copy() {
        return new FixedState(position, orientation, time);
    }

    @Override
    public Vector3fxc position() {
        return position;
    }

    @Override
    public Vector3fc velocity() {
        return Vectors.O;
    }

    @Override
    public Quaternionf orientation() {
        return orientation;
    }

    @Override
    public float time() {
        return time;
    }

    /**
     * changes the time of this state. This is only effective when using {@link #interpolate(State, float)}
     * @param gameTime the new time of this state.
     */
    public FixedState update(float gameTime) {
        this.time = gameTime;
        return this;
    }

    @Override
    public MutableState interpolate(State other, float gameTime) {
        float fraction = (time - other.time()) / (time - gameTime);

        Vector3fx pos = new Vector3fx(position).lerp(other.position(), fraction);
        Vector3f vel = new Vector3f(other.velocity()).mul(fraction);

        return new MutableState(gameTime, pos, vel, other.orientation());
    }
}
