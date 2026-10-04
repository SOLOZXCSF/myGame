package com.example.game.graphics;

import java.awt.Rectangle;

/** Позиция, размер и угол в мировом пространстве. */
public interface Transformable {

    double getX();
    double getY();
    int    getWidth();
    int    getHeight();

    /** Угол поворота в радианах (0 = вправо). */
    default double getAngle()   { return 0; }
    default double getCenterX() { return getX() + getWidth()  / 2.0; }
    default double getCenterY() { return getY() + getHeight() / 2.0; }

    /** AABB для быстрой проверки коллизий. */
    default Rectangle getBounds() {
        return new Rectangle((int) getX(), (int) getY(), getWidth(), getHeight());
    }
}
