package com.example.game.ecs.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

/**
 * Спавнер зомби. Не Entity — не имеет HP, не убивается.
 */
public class ZombieHole {

    public static final int RADIUS = 22;

    public final double x;
    public final double y;

    private double spawnInterval;
    private double timer;
    private double pulse = 0;

    public ZombieHole(double x, double y, double spawnInterval) {
        this.x             = x;
        this.y             = y;
        this.spawnInterval = spawnInterval;
        this.timer         = spawnInterval * Math.random();
    }

    public void setSpawnInterval(double i) { this.spawnInterval = Math.max(0.4, i); }

    /** Возвращает true если пора спавнить зомби. */
    public boolean update(double dt) {
        timer += dt;
        pulse  = (pulse + dt * 3) % (Math.PI * 2);
        if (timer >= spawnInterval) { timer -= spawnInterval; return true; }
        return false;
    }

    public void render(Graphics2D g) {
        int r = (int)(RADIUS * (1.0 + 0.15 * Math.sin(pulse)));
        float[] fr = {0f, 0.6f, 1f};
        Color[] cl = {new Color(0,0,0,240), new Color(30,80,0,180), new Color(0,0,0,0)};
        try {
            g.setPaint(new RadialGradientPaint((float)x, (float)y, r, fr, cl));
        } catch (Exception ignored) {
            g.setColor(new Color(20, 60, 20));
        }
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setColor(new Color(60, 200, 60, (int)(80 + 60 * Math.sin(pulse))));
        g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }
}
