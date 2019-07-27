package NG.Entities;

import NG.DataStructures.Vector3fxc;
import org.joml.Quaternionf;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 26-7-2019.
 */
public interface State {
    /**
     * @return a copy of this state
     */
    State copy();

    /**
     * @return the position on the time given by {@link #time()}
     */
    Vector3fxc position();

    /**
     * @return the velocity on the time given by {@link #time()}
     */
    Vector3fc velocity();

    /**
     * @return the rotation on the time given by {@link #time()}
     */
    Quaternionf orientation();

    /**
     * @return the time that this state refers to
     */
    float time();

    /**
     * Extrapolates this state for the given gameTime and updates this state accordingly. Modifies this state
     * @param gameTime the time of the updated state
     * @return this
     */
    State update(float gameTime);

    /**
     * interpolates this state to the other state to find the state at the given game time, returning a new state as
     * result. can be used to extrapolate based on two states.
     * @param other    another state
     * @param gameTime the fraction of interpolation from this to other
     * @return a new state holding the result
     */
    State interpolate(State other, float gameTime);
}
