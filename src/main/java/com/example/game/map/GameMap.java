package com.example.game.map;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Тайловая карта 50×38 тайлов (1600×1216 пикселей при SIZE=32).
 *
 * Генерируется процедурно при старте:
 *   – трава           — основной фон
 *   – кольцевая дорога вокруг центра
 *   – крестообразные дороги к краям
 *   – каменные стены по периметру и в виде блоков внутри
 *   – вода — лужи по углам
 *
 * Рендер кэшируется в BufferedImage, перерисовывается только при изменении.
 * Коллизии проверяются через isSolid(worldX, worldY).
 */
public class GameMap {

    public static final int TILE_SIZE = 32;

    private final int cols;   // количество тайлов по X
    private final int rows;   // количество тайлов по Y
    private final Tile[][] tiles;

    // Кэш рендера — рисуем в картинку один раз, потом просто drawImage
    private BufferedImage cache;
    private boolean cacheDirty = true;

    public GameMap(int worldWidth, int worldHeight, long seed) {
        this.cols  = worldWidth  / TILE_SIZE;
        this.rows  = worldHeight / TILE_SIZE;
        this.tiles = new Tile[cols][rows];
        generate(new Random(seed));
    }

    // ── генерация ──────────────────────────────────────────────────────────

    private void generate(Random rng) {
        // 1. Заполнить всё травой (с вариацией)
        for (int x = 0; x < cols; x++)
            for (int y = 0; y < rows; y++)
                tiles[x][y] = new Tile(rng.nextInt(6) == 0 ? TileType.GRASS_DARK : TileType.GRASS, rng);

        // 2. Стены по периметру (2 тайла шириной)
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                boolean edge = x < 2 || x >= cols - 2 || y < 2 || y >= rows - 2;
                if (edge) {
                    TileType t = (x < 2 || x >= cols-2 || y < 2)
                            ? TileType.WALL_TOP : TileType.WALL;
                    tiles[x][y] = new Tile(t, rng);
                }
            }
        }

        // 3. Дорожная сеть: горизонталь + вертикаль через центр (3 тайла шириной)
        int cx = cols / 2, cy = rows / 2;
        paintRoad(rng, 2, cols - 3, cy - 1, cy + 1, true);   // горизонтальная
        paintRoad(rng, cx - 1, cx + 1, 2, rows - 3, false);  // вертикальная

        // 4. Кольцевая дорога на расстоянии ~10 тайлов от краёв
        int margin = 10;
        paintRoad(rng, margin, cols - margin - 1, margin,        margin + 2,    true);
        paintRoad(rng, margin, cols - margin - 1, rows-margin-3, rows-margin-1, true);
        paintRoad(rng, margin,        margin + 2, margin, rows - margin - 1,    false);
        paintRoad(rng, cols-margin-3, cols-margin-1, margin, rows-margin-1,     false);

        // 5. Блоки стен внутри — придают ощущение руин/укреплений
        int[][] wallBlocks = {
            {6,  5,  4,  4},  {18, 5,  3,  5},  {28, 4,  5,  3},  {38, 6,  4,  4},
            {6, 25,  4,  4},  {18,27,  3,  4},  {28,25,  5,  3},  {38,25,  4,  4},
            {13,14,  3,  3},  {22,14,  3,  3},  {32,14,  3,  3},
            {13,21,  3,  3},  {22,21,  3,  3},  {32,21,  3,  3},
        };
        for (int[] b : wallBlocks) {
            paintWallBlock(rng, b[0], b[1], b[2], b[3]);
        }

//        // 6. Лужи воды по углам (внутри периметра)
//        paintWater(rng, 3, 3, 4, 3);
//        paintWater(rng, cols - 7, 3, 4, 3);
//        paintWater(rng, 3, rows - 6, 4, 3);
//        paintWater(rng, cols - 7, rows - 6, 4, 3);

        // 7. Грунтовые тропинки — диагональные полосы земли для разнообразия
        for (int i = 5; i < cols - 5; i += 8) {
            for (int j = 5; j < rows - 5; j++) {
                if (tiles[i][j].type == TileType.GRASS || tiles[i][j].type == TileType.GRASS_DARK) {
                    if (rng.nextInt(3) == 0)
                        tiles[i][j] = new Tile(TileType.DIRT, rng);
                }
            }
        }
    }

    private void paintRoad(Random rng, int x0, int x1, int y0, int y1, boolean horizontal) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (!inBounds(x, y)) continue;
                // центральная линия — ROAD_MARK, края — ROAD
                boolean center = horizontal ? (y == (y0 + y1) / 2) : (x == (x0 + x1) / 2);
                tiles[x][y] = new Tile(center ? TileType.ROAD_MARK : TileType.ROAD, rng);
            }
        }
    }

    private void paintWallBlock(Random rng, int tx, int ty, int w, int h) {
        for (int x = tx; x < tx + w; x++) {
            for (int y = ty; y < ty + h; y++) {
                if (!inBounds(x, y)) continue;
                TileType t = (y == ty)
                        ? (rng.nextInt(4) == 0 ? TileType.WALL_MOSS : TileType.WALL_TOP)
                        : (rng.nextInt(5) == 0 ? TileType.WALL_MOSS : TileType.WALL);
                tiles[x][y] = new Tile(t, rng);
            }
        }
    }

    private void paintWater(Random rng, int tx, int ty, int w, int h) {
        for (int x = tx; x < tx + w; x++) {
            for (int y = ty; y < ty + h; y++) {
                if (!inBounds(x, y)) continue;
                boolean edge = x == tx || x == tx+w-1 || y == ty || y == ty+h-1;
                tiles[x][y] = new Tile(edge ? TileType.WATER_FOAM : TileType.WATER, rng);
            }
        }
    }

    // ── коллизии ───────────────────────────────────────────────────────────

    /** Непроходим ли пиксель (worldX, worldY). */
    public boolean isSolid(double worldX, double worldY) {
        int tx = (int)(worldX / TILE_SIZE);
        int ty = (int)(worldY / TILE_SIZE);
        if (!inBounds(tx, ty)) return true;
        return !tiles[tx][ty].type.passable;
    }

    /**
     * Проверить AABB (wx, wy, w, h) на коллизию со стенами.
     * Возвращает вектор коррекции {dx, dy} который выталкивает из стены.
     * Если коллизий нет — {0, 0}.
     */
    public double[] resolveAABB(double wx, double wy, int w, int h) {
        double[] result = {0, 0};

        // Проверяем 4 угла + середины сторон AABB
        double[] xs = {wx, wx + w - 1, wx + w / 2.0};
        double[] ys = {wy, wy + h - 1, wy + h / 2.0};

        boolean hitLeft = false, hitRight = false, hitTop = false, hitBot = false;

        for (double x : xs) {
            if (isSolid(x, wy))            hitTop   = true;
            if (isSolid(x, wy + h - 1))   hitBot   = true;
        }
        for (double y : ys) {
            if (isSolid(wx, y))            hitLeft  = true;
            if (isSolid(wx + w - 1, y))   hitRight = true;
        }

        if (hitLeft)  result[0] += TILE_SIZE;
        if (hitRight) result[0] -= TILE_SIZE;
        if (hitTop)   result[1] += TILE_SIZE;
        if (hitBot)   result[1] -= TILE_SIZE;

        // Ограничить выталкивание размером тайла
        result[0] = Math.max(-TILE_SIZE, Math.min(TILE_SIZE, result[0]));
        result[1] = Math.max(-TILE_SIZE, Math.min(TILE_SIZE, result[1]));
        return result;
    }

    /** Безопасная позиция спавна (проходимый тайл рядом с центром). */
    public double[] findSpawnNear(double wx, double wy) {
        if (!isSolid(wx, wy)) return new double[]{wx, wy};
        for (int r = 1; r < 15; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    double nx = wx + dx * TILE_SIZE;
                    double ny = wy + dy * TILE_SIZE;
                    if (!isSolid(nx, ny) && !isSolid(nx + 16, ny + 16))
                        return new double[]{nx, ny};
                }
            }
        }
        return new double[]{wx, wy};
    }

    // ── рендер ─────────────────────────────────────────────────────────────

    /**
     * Нарисовать видимую часть карты в g.
     * Рисуем только тайлы, попадающие в viewport (camX, camY, viewW, viewH).
     * Кэш перестраивается при первом вызове и только если cacheDirty.
     */
    public void render(Graphics2D g, double camX, double camY, int viewW, int viewH) {
        if (cacheDirty) buildCache();

        // drawImage всей карты (camera уже применена через g.translate)
        g.drawImage(cache, 0, 0, null);
    }

    /** Перестроить полный кэш. Вызывать один раз (карта статична). */
    private void buildCache() {
        int pw = cols * TILE_SIZE;
        int ph = rows * TILE_SIZE;
        cache = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = cache.createGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int x = 0; x < cols; x++)
            for (int y = 0; y < rows; y++)
                tiles[x][y].render(gc, x * TILE_SIZE, y * TILE_SIZE);
        gc.dispose();
        cacheDirty = false;
    }

    // ── утилиты ────────────────────────────────────────────────────────────

    private boolean inBounds(int tx, int ty) {
        return tx >= 0 && tx < cols && ty >= 0 && ty < rows;
    }

    public int getPixelWidth()  { return cols * TILE_SIZE; }
    public int getPixelHeight() { return rows * TILE_SIZE; }
    public int getCols()        { return cols; }
    public int getRows()        { return rows; }
}
