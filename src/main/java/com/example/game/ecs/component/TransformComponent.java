package com.example.game.ecs.component;

import java.awt.Rectangle;

/**
 * Позиция, размер, угол в мировом пространстве.
 * volatile на x/y: сетевой поток пишет, поток отрисовки читает.
 */
public class TransformComponent implements Component {

    public volatile double x;
    public volatile double y;
    public int    width;
    public int    height;
    public double angle;   // радианы, 0 = вправо

    public TransformComponent(double x, double y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public double getCenterX() { return x + width  / 2.0; }
    public double getCenterY() { return y + height / 2.0; }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void set(double x, double y) { this.x = x; this.y = y; }
}
