package com.example.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Игрок: движение + стрельба (с разными типами оружий) + 3 ХП.
 * Логика — только на хосте; клиент получает (x,y,hp,aimDX,aimDY,kills,weaponOrdinal).
 */
public class Player extends Sprite {

    public static final int MAX_HP = 3;

    private static final double SPEED   = 250;
    private static final int    GUN_LEN = 18;
    private static final int    GUN_W   = 5;

    private static final Color[] PALETTE = {
        Color.CYAN, Color.ORANGE, Color.GREEN, Color.PINK,
        Color.YELLOW, new Color(160, 120, 255), Color.RED, Color.WHITE
    };

    private final int id;
    private final int worldWidth;
    private final int worldHeight;

    // состояние (volatile: читается потоком отрисовки)
    private volatile int inputMask;
    public  volatile int   hp           = MAX_HP;
    public  volatile float aimDX        = 1f;
    public  volatile float aimDY        = 0f;
    public  volatile int   kills        = 0;   // убийства зомби (для HUD и оружия)
    public  volatile int   weaponOrdinal = 0;  // WeaponType.ordinal()

    // только хост
    private double shootCooldown = 0;
    private double hitFlash      = 0;

    public Player(int id, double x, double y, int worldWidth, int worldHeight) {
        super(x, y, 32, 32);
        this.id          = id;
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.color       = PALETTE[Math.floorMod(id, PALETTE.length)];
    }

    public int getId() { return id; }
    public void setInputMask(int mask) { this.inputMask = mask; }

    public boolean hit() {
        hitFlash = 0.15;
        return --hp <= 0;
    }

    public void markHit()    { hitFlash = 0.15; }
    public void addKill()    {
        kills++;
        weaponOrdinal = WeaponType.forKills(kills).ordinal();
    }

    public WeaponType getWeapon() {
        WeaponType[] vals = WeaponType.values();
        return vals[Math.min(weaponOrdinal, vals.length - 1)];
    }

    /**
     * Обновление на хосте. Возвращает пули (0-3 штуки).
     */
    public java.util.List<Bullet> tick(double dt) {
        int mask = inputMask;
        double dx = ((mask & KeyInput.RIGHT) != 0 ? 1 : 0) - ((mask & KeyInput.LEFT)  != 0 ? 1 : 0);
        double dy = ((mask & KeyInput.DOWN)  != 0 ? 1 : 0) - ((mask & KeyInput.UP)    != 0 ? 1 : 0);

        if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }
        if (dx != 0 || dy != 0) { aimDX = (float) dx; aimDY = (float) dy; }

        x = Math.max(0, Math.min(x + dx * SPEED * dt, worldWidth  - width));
        y = Math.max(0, Math.min(y + dy * SPEED * dt, worldHeight - height));

        shootCooldown -= dt;
        if (hitFlash > 0) hitFlash -= dt;

        java.util.List<Bullet> shots = new java.util.ArrayList<>();
        if (shootCooldown <= 0 && (mask & KeyInput.SHOOT) != 0) {
            WeaponType w = getWeapon();
            shootCooldown = w.shootDelay;

            double len = Math.hypot(aimDX, aimDY);
            double ndx = aimDX / len;
            double ndy = aimDY / len;
            double cx  = x + width  / 2.0;
            double cy  = y + height / 2.0;

            if (w == WeaponType.SHOTGUN) {
                // 3 пули веером ±20°
                for (int spread : new int[]{-20, 0, 20}) {
                    double rad = Math.toRadians(spread);
                    double sdx = ndx * Math.cos(rad) - ndy * Math.sin(rad);
                    double sdy = ndx * Math.sin(rad) + ndy * Math.cos(rad);
                    shots.add(makeBullet(id, cx, cy, sdx, sdy, w));
                }
            } else {
                shots.add(makeBullet(id, cx, cy, ndx, ndy, w));
            }
        }
        return shots;
    }

    private Bullet makeBullet(int ownerId, double cx, double cy,
                               double ndx, double ndy, WeaponType w) {
        double ox = cx + ndx * (GUN_LEN + width  / 2.0);
        double oy = cy + ndy * (GUN_LEN + height / 2.0);
        return new Bullet(ownerId, ox, oy,
                ndx * w.bulletSpeed, ndy * w.bulletSpeed,
                w.bulletRadius, w.bulletColor);
    }

    // вызывается на клиенте для анимации hitFlash
    @Override
    public void update(double dt) {
        if (hitFlash > 0) hitFlash -= dt;
    }

    @Override
    public void render(Graphics2D g) {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;

        // тело
        g.setColor(hitFlash > 0 ? Color.WHITE : color);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 8, 8));

        // пистолет (ствол длиннее/толще у лучших оружий)
        WeaponType w    = getWeapon();
        int        gLen = GUN_LEN + w.ordinal() * 3;
        int        gW   = GUN_W   + w.ordinal();
        double len  = Math.hypot(aimDX, aimDY);
        double ndx  = aimDX / len,  ndy = aimDY / len;
        double gx1  = cx + ndx * (width  / 2.0 - 2);
        double gy1  = cy + ndy * (height / 2.0 - 2);

        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke(gW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(w.bulletColor.darker());
        g.draw(new Line2D.Double(gx1, gy1, gx1 + ndx * gLen, gy1 + ndy * gLen));
        g.setStroke(new BasicStroke(1));

        // метка + оружие
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        String lbl = "P" + id + " [" + w.name + "]";
        int tw = g.getFontMetrics().stringWidth(lbl);
        g.drawString(lbl, (int)(cx - tw / 2.0), (int) y - 4);

        // HP сердечки
        for (int i = 0; i < MAX_HP; i++) {
            g.setColor(i < hp ? Color.RED : new Color(80, 20, 20));
            drawHeart(g, (int)(x + i * 12), (int)(y + height + 4), 10);
        }
    }

    private void drawHeart(Graphics2D g, int ox, int oy, int s) {
        int[] xs = {ox+s/2, ox+s, ox+s, ox+s*3/4, ox+s/2, ox+s/4, ox, ox,     ox+s/2};
        int[] ys = {oy+s,   oy+s/3, oy, oy,       oy+s/2, oy,     oy, oy+s/3, oy+s  };
        g.fillPolygon(xs, ys, xs.length);
    }
}
