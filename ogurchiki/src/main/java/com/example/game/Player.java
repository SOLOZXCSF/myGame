package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Игрок. Управляется маской ввода (см. {@link KeyInput}), поэтому одинаково
 * работает и для локального игрока, и для удалённого (маска приходит по сети).
 */
public class Player extends Sprite {

    private static final double SPEED = 250; // пикселей в секунду
    private static final Color[] PALETTE = {
            Color.CYAN, Color.ORANGE, Color.GREEN, Color.PINK,
            Color.YELLOW, new Color(160, 120, 255), Color.RED, Color.WHITE
    };

    private final int id;
    private final int worldWidth;
    private final int worldHeight;
    private volatile int inputMask;

    public Player(int id, double x, double y, int worldWidth, int worldHeight) {
        super(x, y, 32, 32);
        this.id = id;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.color = PALETTE[Math.floorMod(id, PALETTE.length)];
    }

    public int getId() { return id; }

    public void setInputMask(int mask) {
        this.inputMask = mask;
    }

    @Override
    public void update(double dt) {
        int mask = inputMask;
        double dx = ((mask & KeyInput.RIGHT) != 0 ? 1 : 0) - ((mask & KeyInput.LEFT) != 0 ? 1 : 0);
        double dy = ((mask & KeyInput.DOWN) != 0 ? 1 : 0) - ((mask & KeyInput.UP) != 0 ? 1 : 0);

        // Нормализация, чтобы по диагонали не двигаться быстрее
        if (dx != 0 && dy != 0) {
            double inv = 1 / Math.sqrt(2);
            dx *= inv;
            dy *= inv;
        }

        double nx = x + dx * SPEED * dt;
        double ny = y + dy * SPEED * dt;

        // Ограничение границами мира
        x = Math.max(0, Math.min(nx, worldWidth - width));
        y = Math.max(0, Math.min(ny, worldHeight - height));
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        g.setColor(Color.WHITE);
        g.drawString("P" + id, (int) x + 6, (int) y - 4);
    }
}
