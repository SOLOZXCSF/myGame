package com.example.game.entity;

import com.example.game.ecs.Entity;
import com.example.game.ecs.component.ColliderComponent;
import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.TransformComponent;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Камень-ресурс. Добывается киркой → даёт ItemType.STONE.
 * Не движется, не атакует.
 */
public class ResourceNodeEntity extends Entity {

    public static final int SIZE    = 24;
    public static final int MAX_HP  = 3;

    private double shakeTimer = 0;   // визуальное "дрожание" при ударе

    public ResourceNodeEntity(int id, double x, double y) {
        super(id);
        add(new TransformComponent(x - SIZE / 2.0, y - SIZE / 2.0, SIZE, SIZE));
        add(new HealthComponent(MAX_HP));
        add(new ColliderComponent(ColliderComponent.Group.NONE));
    }

    /** Вызывается PickaxeSystem при попадании. */
    public void onHit() { shakeTimer = 0.12; }

    public void update(double dt) { if (shakeTimer > 0) shakeTimer -= dt; }

    public void render(Graphics2D g) {
        TransformComponent tr = require(TransformComponent.class);
        HealthComponent    hp = require(HealthComponent.class);

        double ox = shakeTimer > 0 ? (Math.random() - 0.5) * 4 : 0;
        double oy = shakeTimer > 0 ? (Math.random() - 0.5) * 4 : 0;

        double x = tr.x + ox, y = tr.y + oy;
        int s = SIZE;

        // тень
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x + 3, y + 5, s, s * 0.55));

        // тело камня
        int shade = 80 + (int)(70.0 * hp.hp / hp.maxHp);
        g.setColor(new Color(shade, shade, shade + 10));
        int[] px = {(int)(x+s/2),(int)(x+s),(int)(x+s*0.85),(int)(x+s*0.6),
                    (int)(x+s*0.1),(int)(x),(int)(x+s*0.15)};
        int[] py = {(int)(y),(int)(y+s*0.35),(int)(y+s*0.9),(int)(y+s),
                    (int)(y+s*0.95),(int)(y+s*0.4),(int)(y+s*0.1)};
        g.fillPolygon(px, py, px.length);

        // блик
        g.setColor(new Color(220, 220, 230, 120));
        g.fill(new Ellipse2D.Double(x + s * 0.2, y + s * 0.05, s * 0.3, s * 0.2));

        // HP-черточки
        for (int i = 0; i < hp.maxHp; i++) {
            g.setColor(i < hp.hp ? new Color(200, 200, 210) : new Color(80, 80, 90));
            g.fillRect((int)(x + 2 + i * 8), (int)(y + s + 3), 6, 3);
        }
    }
}
