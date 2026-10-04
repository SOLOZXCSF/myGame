package com.example.game.ecs.world;

import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.system.*;
import com.example.game.entity.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Хранилище всех сущностей + вызов систем.
 *
 * Мир 1600×1200: больше экрана (800×600), камера следит за игроком.
 */
public class World {

    public static final int WIDTH  = 1600;
    public static final int HEIGHT = 1200;

    private final Map<Integer, PlayerEntity> players = new ConcurrentHashMap<>();
    private final List<BulletEntity>         bullets = new CopyOnWriteArrayList<>();
    private final List<ZombieEntity>         zombies = new CopyOnWriteArrayList<>();
    private final List<ZombieHole>           holes   = new CopyOnWriteArrayList<>();

    private final AtomicInteger zombieIdSeq = new AtomicInteger(0);
    private volatile int wave = 1;

    // Порядок систем строго задан здесь
    private final List<com.example.game.ecs.system.System> systems = List.of(
            new MovementSystem(),
            new ShootingSystem(),
            new ZombieAiSystem(),
            new BulletMoveSystem(),
            new CollisionSystem(),
            new WaveSystem(),
            new CleanupSystem()
    );

    private static final double[][] HOLE_COORDS = {
        {160,  160}, {800,   80}, {1440,  160},
        { 80,  600}, {1520,  600},
        {160, 1040}, {800,  1120}, {1440, 1040}
    };

    public World() {
        for (double[] c : HOLE_COORDS)
            holes.add(new ZombieHole(c[0], c[1], 4.0));
    }

    // ── главный update ────────────────────────────────────────────────────

    public void updateHost(double dt) {
        for (com.example.game.ecs.system.System s : systems) s.update(this, dt);
    }

    // ── спавн ────────────────────────────────────────────────────────────

    public PlayerEntity spawn(int id) {
        double x = 600 + (id * 200) % (WIDTH  - 700);
        double y = 400 + (id * 150) % (HEIGHT - 500);
        PlayerEntity p = EntityFactory.player(id, x, y);
        players.put(id, p);
        return p;
    }

    public void respawn(PlayerEntity p) {
        TransformComponent tr = p.require(TransformComponent.class);
        HealthComponent    hp = p.require(HealthComponent.class);
        hp.heal();
        tr.x = 600 + (p.getId() * 200 + 80) % (WIDTH  - 700);
        tr.y = 400 + (p.getId() * 150 + 80) % (HEIGHT - 500);
    }

    // ── геттеры ──────────────────────────────────────────────────────────

    public Map<Integer, PlayerEntity> getPlayers() { return players; }
    public List<BulletEntity>         getBullets() { return bullets; }
    public List<ZombieEntity>         getZombies() { return zombies; }
    public List<ZombieHole>           getHoles()   { return holes; }
    public int                        getWave()    { return wave; }
    public int                        getWidth()   { return WIDTH; }
    public int                        getHeight()  { return HEIGHT; }

    public void setWave(int w)            { this.wave = w; }
    public void addBullet(BulletEntity b) { bullets.add(b); }
    public void addZombie(ZombieEntity z) { zombies.add(z); }
    public int  nextZombieId()            { return zombieIdSeq.getAndIncrement(); }
}
