package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Лужа крови, оставляемая на месте гибели зомби.
 * Постепенно исчезает со временем.
 */
public class BloodPuddle {

    private final double x, y;
    private final double radius;
    private double lifeTime = 12.0; // Лужа живет 12 секунд
    private final double maxLifeTime = 12.0;

    public BloodPuddle(double x, double y) {
        this.x = x;
        this.y = y;
        // Небольшой случайный разброс радиуса лужи
        this.radius = 12 + Math.random() * 8;
    }

    public void update(double dt) {
        if (lifeTime > 0) {
            lifeTime -= dt;
        }
    }

    public boolean isExpired() {
        return lifeTime <= 0;
    }

    public void render(Graphics2D g) {
        if (lifeTime <= 0) return;

        // Прозрачность угасает ближе к концу жизни
        float alpha = (float) Math.max(0, Math.min(1.0, lifeTime / maxLifeTime));
        g.setColor(new Color(130, 0, 0, (int) (alpha * 180)));

        double drawX = x - radius;
        double drawY = y - radius;
        g.fill(new Ellipse2D.Double(drawX, drawY, radius * 2, radius * 2));
    }
}