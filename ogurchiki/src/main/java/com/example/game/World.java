package com.example.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Игровой мир: игроки + зомби + пули + дырки.
 * Вся логика — только на хосте (updateHost).
 */
public class World {

    public static final int WIDTH  = 800;
    public static final int HEIGHT = 600;

    // ── коллекции ────────────────────────────────────────────────────────────
    private final Map<Integer, Player>  players  = new ConcurrentHashMap<>();
    private final List<Bullet>          bullets  = new CopyOnWriteArrayList<>();
    private final List<Zombie>          zombies  = new CopyOnWriteArrayList<>();
    private final List<ZombieHole>      holes    = new CopyOnWriteArrayList<>();

    private final AtomicInteger zombieIdSeq = new AtomicInteger(0);

    // волновой счётчик
    private int    wave            = 1;
    private double waveTimer       = 0;
    private static final double WAVE_DURATION = 30.0; // сек на волну

    // фиксированные дырки
    private static final double[][] HOLE_COORDS = {
        {80,  80},  {400, 40},  {720, 80},
        {40,  300}, {760, 300},
        {80,  520}, {400, 560}, {720, 520}
    };

    public World() {
        for (double[] c : HOLE_COORDS) {
            holes.add(new ZombieHole(c[0], c[1], 4.0));
        }
    }

    // ── геттеры ──────────────────────────────────────────────────────────────
    public Map<Integer, Player> getPlayers() { return players; }
    public List<Bullet>         getBullets() { return bullets; }
    public List<Zombie>         getZombies() { return zombies; }
    public List<ZombieHole>     getHoles()   { return holes; }
    public int                  getWave()    { return wave; }

    // ── спавн игрока ─────────────────────────────────────────────────────────
    public Player spawn(int id) {
        double x = 300 + (id * 150) % (WIDTH  - 300);
        double y = 200 + (id * 110) % (HEIGHT - 300);
        Player p = new Player(id, x, y, WIDTH, HEIGHT);
        players.put(id, p);
        return p;
    }

    private void respawn(Player p) {
        p.hp = Player.MAX_HP;
        p.x  = 300 + (p.getId() * 150 + 40) % (WIDTH  - 300);
        p.y  = 200 + (p.getId() * 110 + 40) % (HEIGHT - 300);
    }

    // ── главный апдейт (только хост) ─────────────────────────────────────────
    public void updateHost(double dt) {
        waveTimer += dt;
        if (waveTimer >= WAVE_DURATION) {
            waveTimer -= WAVE_DURATION;
            wave++;
            // ускоряем дырки с каждой волной
            double interval = Math.max(0.5, 4.0 - wave * 0.25);
            for (ZombieHole h : holes) h.setSpawnInterval(interval);
        }

        // спавн зомби из дырок
        List<Player> playerList = new ArrayList<>(players.values());
        if (!playerList.isEmpty()) {
            for (ZombieHole h : holes) {
                if (h.update(dt)) {
                    int hp    = 1 + wave / 3;          // крепче с волнами
                    double spd = Math.min(SPEED_FOR_WAVE(wave), 150);
                    zombies.add(new Zombie(zombieIdSeq.getAndIncrement(),
                                          h.x, h.y, hp, spd));
                }
            }
        }

        // двигаем зомби
        for (Zombie z : zombies) z.tick(dt, playerList);
        zombies.removeIf(z -> !z.isAlive());

        // двигаем игроков → собираем пули
        for (Player p : players.values()) {
            List<Bullet> shots = p.tick(dt);
            bullets.addAll(shots);
        }

        // двигаем пули
        for (Bullet b : bullets) b.update(dt);
        bullets.removeIf(b -> !b.isAlive());

        // коллизии: пуля игрока → зомби
        List<Bullet> bSnap = new ArrayList<>(bullets);
        List<Zombie> zSnap = new ArrayList<>(zombies);
        for (Bullet b : bSnap) {
            if (b.ownerId < 0) continue;  // пуля зомби
            for (Zombie z : zSnap) {
                if (!b.isAlive() || !z.isAlive()) continue;
                if (b.collidesWith(z)) {
                    b.alive = false;
                    if (z.hit(1)) {
                        // зомби убит — засчитываем игроку
                        Player killer = players.get(b.ownerId);
                        if (killer != null) killer.addKill();
                    }
                }
            }
        }
        bullets.removeIf(b -> !b.isAlive());
        zombies.removeIf(z -> !z.isAlive());

        // коллизии: зомби → игрок (контактный урон)
        for (Zombie z : new ArrayList<>(zombies)) {
            for (Player p : playerList) {
                if (z.isAlive() && z.collidesWith(p)) {
                    // зомби кусает и немного отталкивается
                    if (p.hit()) respawn(p);
                    // откат зомби
                    z.x -= (p.getX() - z.x) * 0.3;
                    z.y -= (p.getY() - z.y) * 0.3;
                    z.x = Math.max(0, Math.min(z.x, WIDTH  - Zombie.SIZE));
                    z.y = Math.max(0, Math.min(z.y, HEIGHT - Zombie.SIZE));
                }
            }
        }

        // коллизии: пуля игрока → другой игрок (PvP)
        for (Bullet b : new ArrayList<>(bullets)) {
            if (b.ownerId < 0) continue;
            for (Player p : playerList) {
                if (p.getId() == b.ownerId) continue;
                if (b.isAlive() && b.collidesWith(p)) {
                    b.alive = false;
                    if (p.hit()) respawn(p);
                }
            }
        }
        bullets.removeIf(b -> !b.isAlive());
    }

    private static double SPEED_FOR_WAVE(int wave) {
        return 55 + wave * 8;
    }
}
