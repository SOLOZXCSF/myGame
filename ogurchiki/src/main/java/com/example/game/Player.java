package com.example.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Игрок: движение + стрельба + 3 ХП.
 *
 * Логика считается только на хосте; клиент получает
 * (x, y, hp, aimDirX, aimDirY) и просто рисует.
 */
public class Player extends Sprite {

    public static final int MAX_HP = 3;

    private static final double SPEED        = 250;   // пикс/с
    private static final double SHOOT_DELAY  = 0.35;  // с между выстрелами
    private static final int    GUN_LEN      = 18;    // длина ствола (пикс)
    private static final int    GUN_W        = 5;     // толщина ствола

    private static final Color[] PALETTE = {
        Color.CYAN, Color.ORANGE, Color.GREEN, Color.PINK,
        Color.YELLOW, new Color(160, 120, 255), Color.RED, Color.WHITE
    };

    private final int id;
    private final int worldWidth;
    private final int worldHeight;

    // ─── состояние (volatile: читается из потока отрисовки) ───
    private volatile int inputMask;
    public  volatile int hp      = MAX_HP;
    public  volatile float aimDX = 1f;   // последнее направление прицела
    public  volatile float aimDY = 0f;

    // кулдаун стрельбы (только на хосте)
    private double shootCooldown = 0;

    // мигание при попадании
    private double hitFlash = 0;

    public Player(int id, double x, double y, int worldWidth, int worldHeight) {
        super(x, y, 32, 32);
        this.id          = id;
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.color       = PALETTE[Math.floorMod(id, PALETTE.length)];
    }

    public int getId() { return id; }

    public void setInputMask(int mask) { this.inputMask = mask; }

    /** Нанести урон. Возвращает true, если игрок умер. */
    public boolean hit() {
        hitFlash = 0.15;
        return --hp <= 0;
    }

    public void markHit() { hitFlash = 0.15; } // для клиента (визуал без логики)

    /**
     * Обновление на хосте. Возвращает новую пулю, если игрок выстрелил, иначе null.
     */
    public Bullet tick(double dt) {
        int mask = inputMask;
        double dx = ((mask & KeyInput.RIGHT) != 0 ? 1 : 0) - ((mask & KeyInput.LEFT) != 0 ? 1 : 0);
        double dy = ((mask & KeyInput.DOWN)  != 0 ? 1 : 0) - ((mask & KeyInput.UP)   != 0 ? 1 : 0);

        if (dx != 0 && dy != 0) {
            double inv = 1 / Math.sqrt(2);
            dx *= inv;
            dy *= inv;
        }
        // запомнить направление движения как прицел
        if (dx != 0 || dy != 0) {
            aimDX = (float) dx;
            aimDY = (float) dy;
        }

        x = Math.max(0, Math.min(x + dx * SPEED * dt, worldWidth  - width));
        y = Math.max(0, Math.min(y + dy * SPEED * dt, worldHeight - height));

        shootCooldown -= dt;
        if (hitFlash > 0) hitFlash -= dt;

        if (shootCooldown <= 0 && (mask & KeyInput.SHOOT) != 0) {
            shootCooldown = SHOOT_DELAY;
            double cx = x + width  / 2.0;
            double cy = y + height / 2.0;
            double len = Math.hypot(aimDX, aimDY);
            double ndx = aimDX / len;
            double ndy = aimDY / len;
            return new Bullet(id, cx + ndx * (GUN_LEN + width / 2.0),
                                  cy + ndy * (GUN_LEN + height / 2.0),
                                  ndx * Bullet.SPEED, ndy * Bullet.SPEED);
        }
        return null;
    }


    @Override
    public void update(double dt) {

    }

    @Override
    public void render(Graphics2D g) {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;

        // ── тело ──
        Color body = hitFlash > 0 ? Color.WHITE : color;
        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 8, 8));

        // ── пистолет ──
        double len   = Math.hypot(aimDX, aimDY);
        double ndx   = aimDX / len;
        double ndy   = aimDY / len;
        double gx1   = cx + ndx * (width  / 2.0 - 2);
        double gy1   = cy + ndy * (height / 2.0 - 2);
        double gx2   = gx1 + ndx * GUN_LEN;
        double gy2   = gy1 + ndy * GUN_LEN;

        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke(GUN_W, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.DARK_GRAY);
        g.draw(new Line2D.Double(gx1, gy1, gx2, gy2));
        g.setStroke(new BasicStroke(1));

        // ── метка ──
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String lbl = "P" + id;
        int tw = g.getFontMetrics().stringWidth(lbl);
        g.drawString(lbl, (int)(cx - tw / 2.0), (int) y - 4);

        // ── HP-сердечки ──
        for (int i = 0; i < MAX_HP; i++) {
            g.setColor(i < hp ? Color.RED : new Color(80, 20, 20));
            drawHeart(g, (int)(x + i * 12), (int)(y + height + 4), 10);
        }
    }

    /** Рисует маленькое сердечко (cx, cy — верхний левый угол, size — высота). */
    private void drawHeart(Graphics2D g, int ox, int oy, int size) {
        int[] xs = {ox + size/2, ox + size, ox + size, ox + size*3/4, ox + size/2,
                    ox + size/4, ox,         ox,         ox + size/2};
        int[] ys = {oy + size,   oy + size/3, oy,       oy,           oy + size/2,
                    oy,          oy,           oy + size/3, oy + size};
        g.fillPolygon(xs, ys, xs.length);
    }
}
