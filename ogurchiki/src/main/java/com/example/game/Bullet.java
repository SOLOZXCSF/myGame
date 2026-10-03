package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Пуля. Летит прямолинейно, живёт MAX_AGE секунд.
 * ownerId < 0  → принадлежит зомби (наносит урон игрокам).
 * ownerId >= 0 → принадлежит игроку (наносит урон зомби).
 */
public class Bullet extends Sprite {

    public static final double SPEED    = 520;
    public static final double MAX_AGE  = 1.6;

    public final int    ownerId;   // id игрока или -1 (зомби)
    private final double vx;
    private final double vy;
    private final int    radius;
    private double       age = 0;

    /** Простой конструктор (визуальный на клиенте). */
    public Bullet(int ownerId, double cx, double cy, double vx, double vy) {
        this(ownerId, cx, cy, vx, vy, 5, Color.YELLOW);
    }

    /** Полный конструктор — для разных типов оружия. */
    public Bullet(int ownerId, double cx, double cy, double vx, double vy,
                  int radius, Color color) {
        super(cx - radius, cy - radius, radius * 2, radius * 2);
        this.ownerId = ownerId;
        this.vx      = vx;
        this.vy      = vy;
        this.radius  = radius;
        this.color   = color;
    }

    @Override
    public void update(double dt) {
        x   += vx * dt;
        y   += vy * dt;
        age += dt;
        if (age >= MAX_AGE
                || x < -40 || x > World.WIDTH  + 40
                || y < -40 || y > World.HEIGHT + 40) {
            alive = false;
        }
    }

    @Override
    public void render(Graphics2D g) {
        // ядро
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x, y, width, height));
        // свечение
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g.fill(new Ellipse2D.Double(x - radius * 0.6, y - radius * 0.6,
                                    width + radius * 1.2, height + radius * 1.2));
    }

    public int getRadius() { return radius; }
}
