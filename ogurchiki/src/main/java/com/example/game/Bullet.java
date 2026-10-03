package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Пуля. Летит прямолинейно, живёт MAX_AGE секунд.
 * Владелец ownerId нужен, чтобы не попасть в самого себя.
 */
public class Bullet extends Sprite {

    public static final double SPEED    = 520; // пикс/с
    public static final int    RADIUS   = 5;
    public static final double MAX_AGE  = 1.5; // секунд

    public final int ownerId;
    private final double vx;
    private final double vy;
    private double age = 0;

    public Bullet(int ownerId, double x, double y, double vx, double vy) {
        super(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
        this.ownerId = ownerId;
        this.vx = vx;
        this.vy = vy;
        this.color = Color.YELLOW;
    }

    @Override
    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        age += dt;
        if (age >= MAX_AGE
                || x < 0 || x > World.WIDTH
                || y < 0 || y > World.HEIGHT) {
            alive = false;
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x, y, width, height));
        // мягкое свечение
        g.setColor(new Color(255, 255, 100, 80));
        g.fill(new Ellipse2D.Double(x - 3, y - 3, width + 6, height + 6));
    }
}
