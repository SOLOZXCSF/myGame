package com.example.game.entity;

import com.example.game.ecs.Entity;
import com.example.game.ecs.component.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class BulletEntity extends Entity {

    private static final double MAX_AGE = 1.6;

    private final int    ownerId;
    private final double vx;
    private final double vy;
    private       double age = 0;

    /** Полный конструктор — для хоста (физика + коллизии). */
    public BulletEntity(int ownerId, double cx, double cy,
                        double ndx, double ndy, WeaponType w) {
        this.ownerId = ownerId;
        int r = w.bulletRadius;
        this.vx = ndx * w.bulletSpeed;
        this.vy = ndy * w.bulletSpeed;
        double ox = cx + ndx * (18 + 16);
        double oy = cy + ndy * (18 + 16);
        add(new TransformComponent(ox - r, oy - r, r * 2, r * 2));
        add(new RenderComponent(w.bulletColor, 10));
        add(new ColliderComponent(ColliderComponent.Group.PLAYER_BULLET));
    }

    /** Визуальный конструктор — для клиента (только рендер, нет физики). */
    public BulletEntity(double x, double y, int radius, Color color) {
        this.ownerId = -1;
        this.vx = 0;
        this.vy = 0;
        add(new TransformComponent(x, y, radius * 2, radius * 2));
        add(new RenderComponent(color, 10));
        add(new ColliderComponent(ColliderComponent.Group.NONE));
    }

    public int    getOwnerId() { return ownerId; }

    /** Вызывается BulletMoveSystem (только на хосте). */
    public void move(double dt, int worldW, int worldH) {
        TransformComponent tr = require(TransformComponent.class);
        tr.x += vx * dt;
        tr.y += vy * dt;
        age  += dt;
        if (age >= MAX_AGE || tr.x < -40 || tr.x > worldW + 40
                           || tr.y < -40 || tr.y > worldH + 40) kill();
    }

    public void render(Graphics2D g) {
        TransformComponent tr = require(TransformComponent.class);
        RenderComponent    rc = require(RenderComponent.class);
        int r = tr.width / 2;
        g.setColor(rc.color);
        g.fill(new Ellipse2D.Double(tr.x, tr.y, tr.width, tr.height));
        g.setColor(new Color(rc.color.getRed(), rc.color.getGreen(), rc.color.getBlue(), 70));
        g.fill(new Ellipse2D.Double(tr.x - r * 0.6, tr.y - r * 0.6,
                tr.width + r * 1.2, tr.height + r * 1.2));
    }
}
