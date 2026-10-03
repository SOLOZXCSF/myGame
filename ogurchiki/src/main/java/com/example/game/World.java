package com.example.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Игровой мир: игроки + пули.
 * Всё что здесь — доступно из разных потоков (сетевой + игровой цикл).
 */
public class World {

    public static final int WIDTH  = 800;
    public static final int HEIGHT = 600;

    private final Map<Integer, Player>  players = new ConcurrentHashMap<>();
    private final List<Bullet>          bullets = new CopyOnWriteArrayList<>();

    // ── игроки ──────────────────────────────────────────────────────────────

    public Map<Integer, Player> getPlayers() { return players; }

    public Player spawn(int id) {
        double x = 80 + (id * 150) % (WIDTH  - 160);
        double y = 80 + (id * 110) % (HEIGHT - 160);
        Player p = new Player(id, x, y, WIDTH, HEIGHT);
        players.put(id, p);
        return p;
    }

    // ── пули ────────────────────────────────────────────────────────────────

    public List<Bullet> getBullets() { return bullets; }

    public void addBullet(Bullet b) { bullets.add(b); }

    /**
     * Обновление физики (вызывается только на хосте).
     * Возвращает список игроков, которых убили в этом тике (для перезапуска).
     */
    public List<Integer> updateHost(double dt) {
        List<Integer> killed = new ArrayList<>();

        // обновить пули
        for (Bullet b : bullets) b.update(dt);
        bullets.removeIf(b -> !b.isAlive());

        // обновить игроков и проверить попадания
        for (Player p : players.values()) {
            Bullet shot = p.tick(dt);
            if (shot != null) bullets.add(shot);
        }

        // коллизии пуля → игрок
        Collection<Bullet> bulletSnap = new ArrayList<>(bullets);
        for (Bullet b : bulletSnap) {
            for (Player p : players.values()) {
                if (p.getId() == b.ownerId) continue;
                if (b.isAlive() && b.collidesWith(p)) {
                    b.alive = false;
                    if (p.hit()) {
                        killed.add(p.getId());
                        // respawn
                        respawn(p);
                    }
                }
            }
        }
        bullets.removeIf(b -> !b.isAlive());

        return killed;
    }

    /** Возрождение: полное HP, новая позиция. */
    private void respawn(Player p) {
        p.hp = Player.MAX_HP;
        p.x  = 80 + (p.getId() * 150 + 40) % (WIDTH  - 160);
        p.y  = 80 + (p.getId() * 110 + 40) % (HEIGHT - 160);
    }
}
