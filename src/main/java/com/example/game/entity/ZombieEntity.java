package com.example.game.entity;

import com.example.game.ecs.Entity;
import com.example.game.ecs.component.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class ZombieEntity extends Entity {

    public static final int SIZE = 28;
    private final double speed;

    public ZombieEntity(int id, double cx, double cy, int hp, double speed) {
        super(id);
        this.speed = speed;
        add(new TransformComponent(cx - SIZE/2.0, cy - SIZE/2.0, SIZE, SIZE));
        add(new RenderComponent(new Color(50,160,50), 20));
        add(new ColliderComponent(ColliderComponent.Group.ZOMBIE));
        add(new HealthComponent(hp));
    }

    public double getSpeed() { return speed; }

    public void render(Graphics2D g) {
        TransformComponent tr = require(TransformComponent.class);
        HealthComponent    hp = require(HealthComponent.class);
        RenderComponent    rc = require(RenderComponent.class);

        Color body = hp.hitFlash > 0 ? Color.WHITE : rc.color;
        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(tr.x, tr.y, SIZE, SIZE, 6, 6));

        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(tr.x + 5,  tr.y + 7, 7, 7));
        g.fill(new Ellipse2D.Double(tr.x + 16, tr.y + 7, 7, 7));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(tr.x + 7,  tr.y + 9, 3, 3));
        g.fill(new Ellipse2D.Double(tr.x + 18, tr.y + 9, 3, 3));
        g.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++)
            g.fillRect((int)(tr.x + 7 + i * 5), (int)(tr.y + SIZE - 9), 3, 5);

        // HP bar
        double ratio = (double) hp.hp / hp.maxHp;
        g.setColor(new Color(120, 0, 0));
        g.fillRect((int) tr.x, (int)(tr.y - 7), SIZE, 4);
        g.setColor(new Color(220, 50, 50));
        g.fillRect((int) tr.x, (int)(tr.y - 7), (int)(SIZE * ratio), 4);
    }
}
