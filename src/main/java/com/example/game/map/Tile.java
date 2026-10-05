package com.example.game.map;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * Один тайл на карте. Хранит тип + визуальный «шум» для натуральности.
 * Все тайлы рисуются процедурно (без внешних картинок).
 */
public class Tile {

    public static final int SIZE = 32;   // размер тайла в пикселях

    public final TileType type;

    // Процедурные детали, случайные при создании (стабильны для тайла)
    private final int   noiseR, noiseG, noiseB;  // ±15 от базового цвета
    private final int   detailSeed;               // для доп. декора

    public Tile(TileType type, Random rng) {
        this.type       = type;
        this.noiseR     = rng.nextInt(21) - 10;
        this.noiseG     = rng.nextInt(21) - 10;
        this.noiseB     = rng.nextInt(21) - 10;
        this.detailSeed = rng.nextInt(100);
    }

    /** Нарисовать тайл в мировых координатах (px, py). */
    public void render(Graphics2D g, int px, int py) {
        Color base = type.baseColor;
        Color tinted = clamp(
                base.getRed()   + noiseR,
                base.getGreen() + noiseG,
                base.getBlue()  + noiseB);

        g.setColor(tinted);
        g.fillRect(px, py, SIZE, SIZE);

        // Детали поверх базового цвета
        switch (type) {
            case GRASS, GRASS_DARK -> drawGrassDetails(g, px, py, tinted);
            case DIRT, DIRT_DARK   -> drawDirtDetails(g, px, py, tinted);
            case ROAD              -> drawRoadDetails(g, px, py);
            case ROAD_MARK         -> drawRoadMarkDetails(g, px, py);
            case WALL              -> drawWallDetails(g, px, py, tinted);
            case WALL_TOP          -> drawWallTopDetails(g, px, py, tinted);
            case WALL_MOSS         -> drawWallMossDetails(g, px, py, tinted);
            case WATER             -> drawWaterDetails(g, px, py, tinted);
            case WATER_FOAM        -> drawWaterFoamDetails(g, px, py);
        }
    }

    // ── детали травы ──────────────────────────────────────────────────────

    private void drawGrassDetails(Graphics2D g, int px, int py, Color base) {
        // Случайные стебли травы
        if (detailSeed < 40) {
            int blades = 2 + detailSeed % 3;
            for (int i = 0; i < blades; i++) {
                int bx = px + 4 + (detailSeed * 7 + i * 11) % (SIZE - 8);
                int by = py + SIZE / 2 + (detailSeed * 3 + i * 5) % (SIZE / 3);
                int h  = 4 + i % 5;
                g.setColor(new Color(
                        clampC(base.getRed()   - 15),
                        clampC(base.getGreen() + 20),
                        clampC(base.getBlue()  - 10), 160));
                g.drawLine(bx, by, bx + (i % 3 - 1), by - h);
            }
        }
        // Редкий цветок
        if (detailSeed == 7 || detailSeed == 23 || detailSeed == 51) {
            int fx = px + SIZE / 2 - 2 + detailSeed % 6;
            int fy = py + SIZE / 3 + detailSeed % 8;
            g.setColor(new Color(230, 210, 80, 200));
            g.fillOval(fx, fy, 4, 4);
            g.setColor(new Color(200, 100, 50, 180));
            g.fillOval(fx + 1, fy + 1, 2, 2);
        }
    }

    // ── детали земли ──────────────────────────────────────────────────────

    private void drawDirtDetails(Graphics2D g, int px, int py, Color base) {
        if (detailSeed < 30) {
            g.setColor(new Color(
                    clampC(base.getRed()   - 20),
                    clampC(base.getGreen() - 15),
                    clampC(base.getBlue()  - 10), 100));
            int dx = px + 5 + detailSeed % (SIZE - 10);
            int dy = py + 5 + (detailSeed * 3) % (SIZE - 10);
            g.fillOval(dx, dy, 5 + detailSeed % 5, 3 + detailSeed % 3);
        }
    }

    // ── детали дороги ─────────────────────────────────────────────────────

    private void drawRoadDetails(Graphics2D g, int px, int py) {
        // Линии стыков плиток
        g.setColor(new Color(70, 65, 60, 80));
        g.drawLine(px, py, px + SIZE, py);
        g.drawLine(px, py, px, py + SIZE);
        // Мелкие камушки
        if (detailSeed < 20) {
            g.setColor(new Color(110, 105, 100, 120));
            g.fillRect(px + detailSeed % (SIZE - 4), py + (detailSeed * 3) % (SIZE - 4), 3, 2);
        }
    }

    private void drawRoadMarkDetails(Graphics2D g, int px, int py) {
        drawRoadDetails(g, px, py);
        // Центральный светлый блик
        g.setColor(new Color(130, 125, 120, 60));
        g.fillRect(px + 6, py + 6, SIZE - 12, SIZE - 12);
    }

    // ── детали стен ───────────────────────────────────────────────────────

    private void drawWallDetails(Graphics2D g, int px, int py, Color base) {
        // Кладка кирпичей
        Color dark  = new Color(clampC(base.getRed()-20), clampC(base.getGreen()-20), clampC(base.getBlue()-20));
        Color light = new Color(clampC(base.getRed()+15), clampC(base.getGreen()+15), clampC(base.getBlue()+15));

        // горизонтальные швы каждые 8px
        g.setColor(dark);
        for (int row = 0; row < SIZE / 8; row++) {
            g.drawLine(px, py + row * 8, px + SIZE, py + row * 8);
        }
        // вертикальные швы шахматом
        for (int row = 0; row < SIZE / 8; row++) {
            int offset = (row % 2 == 0) ? 0 : SIZE / 2;
            g.drawLine(px + offset,        py + row * 8, px + offset,        py + row * 8 + 8);
            g.drawLine(px + offset + SIZE/2, py + row * 8, px + offset + SIZE/2, py + row * 8 + 8);
        }
        // верхний блик
        g.setColor(new Color(light.getRed(), light.getGreen(), light.getBlue(), 60));
        g.fillRect(px, py, SIZE, 3);
    }

    private void drawWallTopDetails(Graphics2D g, int px, int py, Color base) {
        drawWallDetails(g, px, py, base);
        // Дополнительный яркий верхний край
        g.setColor(new Color(clampC(base.getRed()+25), clampC(base.getGreen()+25), clampC(base.getBlue()+25), 120));
        g.fillRect(px, py, SIZE, 5);
    }

    private void drawWallMossDetails(Graphics2D g, int px, int py, Color base) {
        drawWallDetails(g, px, py, base);
        // Пятна мха
        if (detailSeed < 50) {
            g.setColor(new Color(40, 90, 35, 140));
            int mx = px + detailSeed % (SIZE - 8);
            int my = py + (detailSeed * 5) % (SIZE - 8);
            g.fillOval(mx, my, 6 + detailSeed % 6, 4 + detailSeed % 4);
        }
    }

    // ── детали воды ───────────────────────────────────────────────────────

    private void drawWaterDetails(Graphics2D g, int px, int py, Color base) {
        // Волнистые линии
        g.setColor(new Color(clampC(base.getRed()+20), clampC(base.getGreen()+30), clampC(base.getBlue()+25), 100));
        for (int row = 4; row < SIZE; row += 8) {
            g.drawArc(px + 2, py + row - 2, 12, 4, 0, 180);
            g.drawArc(px + 14, py + row - 2, 12, 4, 180, 180);
        }
    }

    private void drawWaterFoamDetails(Graphics2D g, int px, int py) {
        drawWaterDetails(g, px, py, TileType.WATER_FOAM.baseColor);
        // Белая пена по краям
        g.setColor(new Color(200, 230, 255, 120));
        g.fillRect(px, py, SIZE, 3);
        g.fillRect(px, py, 3, SIZE);
    }

    // ── утилиты ───────────────────────────────────────────────────────────

    private Color clamp(int r, int g, int b) {
        return new Color(clampC(r), clampC(g), clampC(b));
    }

    private int clampC(int v) { return Math.max(0, Math.min(255, v)); }
}
