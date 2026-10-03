package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Зомби. Появляется из «дырки» и ползёт к ближайшему игроку.
 */
public class Zombie extends Sprite {

    public static final int SIZE = 28;

    private final int    id;
    private       int    hp;
    private final int    maxHp;
    private       double hitFlash = 0;
    private final double speed;

    public Zombie(int id, double cx, double cy, int hp, double speed) {
        super(cx - SIZE / 2.0, cy - SIZE / 2.0, SIZE, SIZE);
        this.id    = id;
        this.hp    = hp;
        this.maxHp = hp;
        this.speed = speed;
        this.color = new Color(50, 160, 50);
    }

    public int getId()    { return id; }
    public int getHp()    { return hp; }
    public int getMaxHp() { return maxHp; }

    /** Нужен для Sprite (абстрактный), на клиенте только анимация flash. */
    @Override
    public void update(double dt) {
        if (hitFlash > 0) hitFlash -= dt;
    }

    /** Полный update для хоста — идём к ближайшему игроку. */
    public void tick(double dt, Iterable<Player> players) {
        if (hitFlash > 0) hitFlash -= dt;

        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : players) {
            double dx = (p.getX() + p.getWidth()  / 2.0) - (x + SIZE / 2.0);
            double dy = (p.getY() + p.getHeight() / 2.0) - (y + SIZE / 2.0);
            double d  = Math.hypot(dx, dy);
            if (d < minDist) { minDist = d; target = p; }
        }
        if (target == null) return;

        double dx  = (target.getX() + target.getWidth()  / 2.0) - (x + SIZE / 2.0);
        double dy  = (target.getY() + target.getHeight() / 2.0) - (y + SIZE / 2.0);
        double len = Math.hypot(dx, dy);
        if (len < 1) return;

        x = Math.max(0, Math.min(x + (dx / len) * speed * dt, World.WIDTH  - SIZE));
        y = Math.max(0, Math.min(y + (dy / len) * speed * dt, World.HEIGHT - SIZE));
    }

    /** Получить урон. Возвращает true если погиб. */
    public boolean hit(int dmg) {
        hp -= dmg;
        hitFlash = 0.12;
        if (hp <= 0) { alive = false; return true; }
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        // тело
        g.setColor(hitFlash > 0 ? Color.WHITE : color);
        g.fill(new RoundRectangle2D.Double(x, y, SIZE, SIZE, 6, 6));

        // глаза
        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(x + 5,  y + 7, 7, 7));
        g.fill(new Ellipse2D.Double(x + 16, y + 7, 7, 7));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(x + 7,  y + 9, 3, 3));
        g.fill(new Ellipse2D.Double(x + 18, y + 9, 3, 3));

        // зубы
        g.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++)
            g.fillRect((int)(x + 7 + i * 5), (int)(y + SIZE - 9), 3, 5);

        // HP-полоска
        double ratio = (double) hp / maxHp;
        g.setColor(new Color(120, 0, 0));
        g.fillRect((int) x, (int)(y - 7), SIZE, 4);
        g.setColor(new Color(220, 50, 50));
        g.fillRect((int) x, (int)(y - 7), (int)(SIZE * ratio), 4);
    }
}
