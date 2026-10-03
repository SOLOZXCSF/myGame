package com.example.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Зомби. Двигается к ближайшему игроку с учётом стен и наносит урон при касании.
 */
public class Zombie extends Sprite {

    public static final int SIZE = 28;

    private final int    id;
    private       int    hp;
    private final int    maxHp;
    private       double hitFlash = 0;
    private final double speed;

    private double attackCooldown = 0;
    private static final double ATTACK_DELAY = 0.8; // Задержка между атаками (сек)

    private float lookDX = 0;
    private float lookDY = 1;

    // Конструктор для спавна в World по умолчанию
    public Zombie(double x, double y) {
        this((int) (Math.random() * 100000), x, y, 3, 100 + Math.random() * 40);
    }

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

    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public void resetAttackCooldown() {
        this.attackCooldown = ATTACK_DELAY;
    }

    /** Вызывается клиентом для анимации получения урона. */
    @Override
    public void update(double dt) {
        if (hitFlash > 0) hitFlash -= dt;
    }

    /** Вызывается из World.update(...) на сервере/хосте. */
    public void update(double dt, Iterable<Player> players, List<Wall> walls) {
        tick(dt, players, walls);
    }

    /** Полный update для хоста — идем к ближайшему игроку с учетом коллизий. */
    public void tick(double dt, Iterable<Player> players, List<Wall> walls) {
        if (hitFlash > 0)       hitFlash -= dt;
        if (attackCooldown > 0) attackCooldown -= dt;

        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : players) {
            if (p.hp <= 0) continue;
            double dx = (p.getX() + p.getWidth() / 2.0) - (x + SIZE / 2.0);
            double dy = (p.getY() + p.getHeight() / 2.0) - (y + SIZE / 2.0);
            double d  = Math.hypot(dx, dy);
            if (d < minDist) {
                minDist = d;
                target = p;
            }
        }
        if (target == null) return;

        double dx  = (target.getX() + target.getWidth() / 2.0) - (x + SIZE / 2.0);
        double dy  = (target.getY() + target.getHeight() / 2.0) - (y + SIZE / 2.0);
        double len = Math.hypot(dx, dy);
        if (len < 1) return;

        // Нормализованный вектор направления
        lookDX = (float) (dx / len);
        lookDY = (float) (dy / len);

        double moveX = lookDX * speed * dt;
        double moveY = lookDY * speed * dt;

        float radius = SIZE / 2.0f;

        // 1. Движение по X с проверкой коллизии со стенами
        double oldX = this.x;
        this.x = Math.max(0, Math.min(this.x + moveX, World.WIDTH - SIZE));
        if (checkWallCollision(walls, radius)) {
            this.x = oldX;
        }

        // 2. Движение по Y с проверкой коллизии со стенами
        double oldY = this.y;
        this.y = Math.max(0, Math.min(this.y + moveY, World.HEIGHT - SIZE));
        if (checkWallCollision(walls, radius)) {
            this.y = oldY;
        }
    }

    private boolean checkWallCollision(List<Wall> walls, float radius) {
        if (walls == null || walls.isEmpty()) return false;

        float centerX = (float) (x + SIZE / 2.0);
        float centerY = (float) (y + SIZE / 2.0);

        for (Wall wall : walls) {
            if (wall.intersectsCircle(centerX, centerY, radius)) {
                return true;
            }
        }
        return false;
    }

    /** Получить урон. Возвращает true, если погиб. */
    public boolean hit(int dmg) {
        hp -= dmg;
        hitFlash = 0.12;
        if (hp <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        // Тело зомби
        g.setColor(hitFlash > 0 ? Color.WHITE : color);
        g.fill(new RoundRectangle2D.Double(x, y, SIZE, SIZE, 6, 6));

        // Глаза (смещаются немного в сторону игрока)
        int eyeOffX = (int) (lookDX * 2);
        int eyeOffY = (int) (lookDY * 2);

        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(x + 5 + eyeOffX,  y + 7 + eyeOffY, 7, 7));
        g.fill(new Ellipse2D.Double(x + 16 + eyeOffX, y + 7 + eyeOffY, 7, 7));

        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(x + 7 + eyeOffX,  y + 9 + eyeOffY, 3, 3));
        g.fill(new Ellipse2D.Double(x + 18 + eyeOffX, y + 9 + eyeOffY, 3, 3));

        // Зубы
        g.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            g.fillRect((int) (x + 7 + i * 5), (int) (y + SIZE - 9), 3, 5);
        }

        // Полоска здоровья (HP)
        double ratio = Math.max(0, (double) hp / maxHp);
        g.setColor(new Color(120, 0, 0));
        g.fillRect((int) x, (int) (y - 7), SIZE, 4);
        g.setColor(new Color(220, 50, 50));
        g.fillRect((int) x, (int) (y - 7), (int) (SIZE * ratio), 4);
    }
}