package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;

public abstract class Sprite {

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected Color  color;
    protected boolean alive = true;

    public Sprite(double x, double y, double width, double height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean intersects(Sprite other) {
        if (other == null) return false;
        return this.x < other.x + other.width  &&
                this.x + this.width > other.x  &&
                this.y < other.y + other.height &&
                this.y + this.height > other.y;
    }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public abstract void update(double dt);
    public abstract void render(Graphics2D g);
}