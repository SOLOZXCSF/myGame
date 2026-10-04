package com.example.game.entity;

import com.example.game.ecs.Entity;
import com.example.game.ecs.component.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Игрок. Компоненты:
 *   TransformComponent, RenderComponent, ColliderComponent,
 *   HealthComponent, WeaponComponent, InputComponent
 */
public class PlayerEntity extends Entity {

    public static final int    SIZE    = 32;
    public static final int    MAX_HP  = 3;
    private static final int   GUN_LEN = 18;

    private static final Color[] PALETTE = {
        Color.CYAN, Color.ORANGE, Color.GREEN, Color.PINK,
        Color.YELLOW, new Color(160,120,255), Color.RED, Color.WHITE
    };

    private volatile int kills = 0;

    public PlayerEntity(int id, double x, double y) {
        super(id);
        add(new TransformComponent(x, y, SIZE, SIZE));
        add(new RenderComponent(PALETTE[Math.floorMod(id, PALETTE.length)], 30));
        add(new ColliderComponent(ColliderComponent.Group.PLAYER, 0.1));
        add(new HealthComponent(MAX_HP));
        add(new WeaponComponent());
        add(new InputComponent());
    }

    public int getKills() { return kills; }

    public void addKill() {
        kills++;
        WeaponComponent wp = require(WeaponComponent.class);
        wp.weaponOrdinal = WeaponType.forKills(kills).ordinal();
    }

    public void render(Graphics2D g) {
        TransformComponent tr = require(TransformComponent.class);
        HealthComponent    hp = require(HealthComponent.class);
        WeaponComponent    wp = require(WeaponComponent.class);
        RenderComponent    rc = require(RenderComponent.class);

        double cx = tr.getCenterX(), cy = tr.getCenterY();
        Color body = hp.hitFlash > 0 ? Color.WHITE : rc.color;

        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(tr.x, tr.y, tr.width, tr.height, 8, 8));

        // ствол
        WeaponType w    = WeaponType.forOrdinal(wp.weaponOrdinal);
        int        gLen = GUN_LEN + w.ordinal() * 3;
        int        gW   = 5       + w.ordinal();
        double     ndx  = wp.aimDX, ndy = wp.aimDY;
        double     gx1  = cx + ndx * (tr.width  / 2.0 - 2);
        double     gy1  = cy + ndy * (tr.height / 2.0 - 2);

        g.setStroke(new BasicStroke(gW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(w.bulletColor.darker());
        g.draw(new Line2D.Double(gx1, gy1, gx1 + ndx * gLen, gy1 + ndy * gLen));
        g.setStroke(new BasicStroke(1));

        // метка
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        String lbl = "P" + getId() + " [" + w.name + "]";
        int tw = g.getFontMetrics().stringWidth(lbl);
        g.drawString(lbl, (int)(cx - tw / 2.0), (int) tr.y - 4);

        // HP сердечки
        for (int i = 0; i < MAX_HP; i++) {
            g.setColor(i < hp.hp ? Color.RED : new Color(80,20,20));
            drawHeart(g, (int)(tr.x + i * 12), (int)(tr.y + tr.height + 4), 10);
        }
    }

    private void drawHeart(Graphics2D g, int ox, int oy, int s) {
        int[] xs = {ox+s/2,ox+s,ox+s,ox+s*3/4,ox+s/2,ox+s/4,ox,ox,ox+s/2};
        int[] ys = {oy+s,oy+s/3,oy,oy,oy+s/2,oy,oy,oy+s/3,oy+s};
        g.fillPolygon(xs, ys, xs.length);
    }
}
