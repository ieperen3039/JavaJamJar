package NG.Entities.Projectiles;

import NG.Core.Game;
import NG.Core.GameTimer;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 2-4-2019.
 */
public abstract class Projectile implements MovingEntity {
    protected final Game game;
    private float spawnTime = Float.MAX_VALUE;
    private Object source;

    private boolean isDisposed = false;
    private boolean isLaunched = false;

    public Projectile(Game game, Object source) {
        this.game = game;
        this.source = source;
    }

    /**
     * Sets the position of this projectile to startPosition and make it move to endPosition No call to {@link
     * #getStateAt(float)} may happen before this method * is called
     * @param startPosition a position in the world, where this projectile spawns at the current game time
     * @param spawnTime     moment of launching, should be equal to the current game time
     */
    public void launch(Vector3fc startPosition, float spawnTime) {
        this.spawnTime = spawnTime;
        setSpawnPosition(startPosition);
        isLaunched = true;
    }

    /**
     * sets the start position of the projectile.
     * @param spawnPosition the position of spawning
     */
    protected abstract void setSpawnPosition(Vector3fc spawnPosition);

    public boolean isLaunched() {
        return isLaunched;
    }

    @Override
    public void draw(SGL gl) {
        if (isDisposed) return;
        float now = game.get(GameTimer.class).getRendertime();
        if (now < spawnTime) return;

        gl.pushMatrix();
        {
            gl.translateRotate(getStateAt(now));
            drawProjectile(gl, now);
        }
        gl.popMatrix();
    }

    /**
     * @param gl         draw the projectile, without additional positioning
     * @param renderTime the current rendering time
     */
    protected abstract void drawProjectile(SGL gl, float renderTime);

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other != this && other != source;
    }

    @Override
    public void collideWith(Entity other, float collisionTime) {
        assert !(other instanceof MovingEntity) || canCollideWith((Entity) other);
        dispose();
    }

    protected float getSpawnTime() {
        return spawnTime;
    }
}
