package com.example.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Игровой мир. Содержит игроков, зомби, пули и стены.
 * Логика обновления (tick) выполняется только на сервере (хосте).
 */
public class World {

    public static final int WIDTH  = 1280;
    public static final int HEIGHT = 720;

    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Zombie> zombies         = new CopyOnWriteArrayList<>();
    private final List<Bullet> bullets         = new CopyOnWriteArrayList<>();
    private final List<Wall> walls             = new CopyOnWriteArrayList<>();

    private double zombieSpawnTimer = 0;
    private static final double ZOMBIE_SPAWN_INTERVAL = 2.5;

    public World() {
        initWalls();
    }

    private void initWalls() {
        // Создаем стены карты (центральный столп сдвинут, чтобы не перекрывать спавн)
        walls.add(new Wall(200, 150, 400, 30));
        walls.add(new Wall(680, 150, 400, 30));
        walls.add(new Wall(590, 220, 100, 100)); // Убран перехват спавна (X:640, Y:360)
        walls.add(new Wall(200, 540, 400, 30));
        walls.add(new Wall(680, 540, 400, 30));
    }

    public Map<Integer, Player> getPlayers() { return players; }
    public List<Zombie> getZombies()         { return zombies; }
    public List<Bullet> getBullets()         { return bullets; }
    public List<Wall> getWalls()             { return walls; }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
    }

    public void update(double dt) {
        // 1. Обновляем игроков и собираем новые пули от выстрелов
        List<Bullet> newBullets = new ArrayList<>();
        for (Player p : players.values()) {
            if (p.hp > 0) {
                List<Bullet> shots = p.tick(dt, walls);
                newBullets.addAll(shots);
            }
        }
        bullets.addAll(newBullets);

        // 2. Спавн зомби
        zombieSpawnTimer += dt;
        if (zombieSpawnTimer >= ZOMBIE_SPAWN_INTERVAL && !players.isEmpty()) {
            zombieSpawnTimer = 0;
            spawnZombie();
        }

        // 3. Обновляем зомби
        for (Zombie z : zombies) {
            z.update(dt, players.values(), walls);
        }

        // 4. Обновляем пули
        for (Bullet b : bullets) {
            b.update(dt);
        }

        // 5. Обработка коллизий
        checkCollisions();

        // 6. Удаляем неактивные пули и мертвых зомби
        bullets.removeIf(b -> !b.isAlive() || checkBulletWallCollision(b));
        zombies.removeIf(z -> z.getHp() <= 0);
    }

    private void spawnZombie() {
        double x, y;
        if (Math.random() < 0.5) {
            x = Math.random() < 0.5 ? -20 : WIDTH + 20;
            y = Math.random() * HEIGHT;
        } else {
            x = Math.random() * WIDTH;
            y = Math.random() < 0.5 ? -20 : HEIGHT + 20;
        }
        zombies.add(new Zombie(x, y));
    }

    private boolean checkBulletWallCollision(Bullet b) {
        for (Wall wall : walls) {
            if (wall.intersectsCircle((float) b.getX(), (float) b.getY(), (float) b.getRadius())) {
                return true;
            }
        }
        return false;
    }

    public Player spawn(int id) {
        // Безопасная точка спавна игрока (по центру экрана, вне центральной стены)
        double spawnX = (WIDTH / 2.0) - 16 + (id * 40);
        double spawnY = (HEIGHT / 2.0) - 16;

        Player player = new Player(id, spawnX, spawnY, WIDTH, HEIGHT);
        addPlayer(player);
        return player;
    }

    private void checkCollisions() {
        // Пули <-> Зомби
        for (Bullet b : bullets) {
            if (!b.isAlive()) continue;

            for (Zombie z : zombies) {
                if (z.getHp() <= 0) continue;

                if (b.intersects(z)) {
                    b.setAlive(false);
                    z.hit(1);

                    if (z.getHp() <= 0) {
                        Player shooter = players.get(b.getOwnerId());
                        if (shooter != null) {
                            shooter.addKill();
                        }
                    }
                    break;
                }
            }
        }

        // Зомби <-> Игроки
        for (Zombie z : zombies) {
            if (z.getHp() <= 0) continue;

            for (Player p : players.values()) {
                if (p.hp <= 0) continue;

                if (z.intersects(p)) {
                    if (z.canAttack()) {
                        z.resetAttackCooldown();
                        p.hit();
                    }
                }
            }
        }
    }
}