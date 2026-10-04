package com.example.game.entity;

import com.example.game.ecs.Entity;
import com.example.game.ecs.component.ColliderComponent;
import com.example.game.ecs.component.TransformComponent;

import java.awt.*;
import java.awt.geom.*;

/**
 * Скупщик/торговец. Стоит в верхнем правом углу карты.
 * Зона взаимодействия: INTERACT_RADIUS пикс от центра.
 * Продажа камней и покупка оружия обрабатывается в ShopSystem.
 */
public class ShopNpcEntity extends Entity {

    public static final int    SIZE            = 36;
    public static final double INTERACT_RADIUS = 80;

    /** Цены на покупку оружия игроком (в золоте). */
    public static final int PRICE_SHOTGUN = 10;
    public static final int PRICE_RIFLE   = 25;
    public static final int PRICE_ROCKET  = 50;

    /** Цена продажи одного камня. */
    public static final int STONE_SELL_PRICE = 3;

    private double bobTimer = 0;

    public ShopNpcEntity(int id, double cx, double cy) {
        super(id);
        add(new TransformComponent(cx - SIZE / 2.0, cy - SIZE / 2.0, SIZE, SIZE));
        add(new ColliderComponent(ColliderComponent.Group.NONE));
    }

    public void update(double dt) { bobTimer += dt * 2.0; }

    public boolean isNear(double px, double py) {
        TransformComponent tr = require(TransformComponent.class);
        double dx = px - tr.getCenterX(), dy = py - tr.getCenterY();
        return Math.hypot(dx, dy) <= INTERACT_RADIUS;
    }

    public void render(Graphics2D g) {
        TransformComponent tr = require(TransformComponent.class);
        double bob = Math.sin(bobTimer) * 2.5;
        double x = tr.x, y = tr.y + bob;
        int    s = SIZE;

        // плащ
        g.setColor(new Color(120, 60, 20));
        g.fill(new RoundRectangle2D.Double(x - 4, y + s * 0.45, s + 8, s * 0.7, 6, 6));

        // тело
        g.setColor(new Color(200, 160, 100));
        g.fill(new Ellipse2D.Double(x + s * 0.1, y + s * 0.3, s * 0.8, s * 0.65));

        // голова
        g.setColor(new Color(230, 190, 140));
        g.fill(new Ellipse2D.Double(x + s * 0.15, y, s * 0.7, s * 0.55));

        // шапка торговца
        g.setColor(new Color(80, 40, 10));
        g.fillRect((int)(x + s * 0.1), (int)(y - s * 0.2), (int)(s * 0.8), (int)(s * 0.25));
        g.fillRect((int)(x - s * 0.05), (int)(y - s * 0.02), (int)(s * 1.1), (int)(s * 0.1));

        // знак "$"
        g.setColor(new Color(255, 220, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("$", (int)(x + s * 0.3), (int)(y + s * 1.05 + bob));

        // зона взаимодействия (тихое кольцо)
        g.setColor(new Color(255, 220, 50, 30));
        int r = (int) INTERACT_RADIUS;
        g.fill(new Ellipse2D.Double(tr.getCenterX() - r, tr.getCenterY() - r, r * 2, r * 2));
        g.setColor(new Color(255, 220, 50, 80));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{6, 6}, (float)(bobTimer * 20)));
        g.draw(new Ellipse2D.Double(tr.getCenterX() - r, tr.getCenterY() - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(1));
    }
}
