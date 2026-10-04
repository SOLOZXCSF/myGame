package com.example.game.graphics;

import java.awt.Rectangle;

/**
 * Объект участвует в коллизиях.
 * Намеренно отделён от Transformable:
 * триггер-зоны могут коллайдить без видимой формы.
 */
public interface Collidable extends Transformable {

    default Rectangle getHitbox() { return getBounds(); }

    CollisionGroup getCollisionGroup();

    default boolean collidesWith(Collidable other) {
        return getHitbox().intersects(other.getHitbox());
    }

    enum CollisionGroup { PLAYER, ZOMBIE, PLAYER_BULLET, ZOMBIE_BULLET, NONE }
}
