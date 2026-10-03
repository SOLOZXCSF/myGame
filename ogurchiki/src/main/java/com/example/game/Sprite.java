package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Базовый класс для всех игровых объектов (игрок, враги, пули и т.д.).
 * Хранит позицию, размер и (опционально) картинку.
 * Если картинки нет — рисуется цветной прямоугольник.
 */
public abstract class Sprite {

    // volatile: позиция может обновляться сетевым потоком, а читаться потоком отрисовки
    protected volatile double x;
    protected volatile double y;
    protected int width;
    protected int height;
    protected BufferedImage image;   // может быть null
    protected Color color = Color.MAGENTA;
    protected boolean alive = true;

    protected Sprite(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Логика объекта. dt — время кадра в секундах. */
    public abstract void update(double dt);

    /** Отрисовка. Переопределите, если нужна своя графика. */
    public void render(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) x, (int) y, width, height, null);
        } else {
            g.setColor(color);
            g.fillRect((int) x, (int) y, width, height);
        }
    }

    /** Прямоугольник для проверки столкновений. */
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public boolean collidesWith(Sprite other) {
        return getBounds().intersects(other.getBounds());
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isAlive() { return alive; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
