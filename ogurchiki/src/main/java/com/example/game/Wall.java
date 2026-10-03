package com.example.game;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;

public class Wall {
    public float x, y;
    public float width, height;

    public Wall(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x, (int) y, (int) width, (int) height);

        g.setColor(Color.BLACK);
        g.drawRect((int) x, (int) y, (int) width, (int) height);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    public boolean intersectsCircle(float cx, float cy, float radius) {
        float closestX = clamp(cx, x, x + width);
        float closestY = clamp(cy, y, y + height);

        float distanceX = cx - closestX;
        float distanceY = cy - closestY;

        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius);
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}