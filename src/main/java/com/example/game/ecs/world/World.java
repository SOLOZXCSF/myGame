package com.example.game.ecs.world;

import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.InventoryComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.system.*;
import com.example.game.entity.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class World {

    public static final int WIDTH  = 1600;
    public static final int HEIGHT = 1200;

    private final Map<Integer, PlayerEntity>  players      = new ConcurrentHashMap<>();
    private final List<BulletEntity>          bullets      = new CopyOnWriteArrayList<>();
    private final List<ZombieEntity>          zombies      = new CopyOnWriteArrayList<>();
    private final List<ZombieHole>            holes        = new CopyOnWriteArrayList<>();
    private final List<ResourceNodeEntity>    resourceNodes= new CopyOnWriteArrayList<>();
    private ShopNpcEntity                     shop;

    private final AtomicInteger zombieIdSeq = new AtomicInteger(0);
    private final AtomicInteger nodeIdSeq   = new AtomicInteger(1000);
    private volatile int wave = 1;

    private final List<com.example.game.ecs.system.System> systems = List.of(
            new MovementSystem(),
            new ShootingSystem(),
            new PickaxeSystem(),       // ← NEW
            new ZombieAiSystem(),
            new BulletMoveSystem(),
            new CollisionSystem(),
            new ShopSystem(),          // ← NEW
            new WaveSystem(),
            new CleanupSystem()
    );

    private static final double[][] HOLE_COORDS = {
        {160,  160}, {800,   80}, {1440,  160},
        { 80,  600}, {1520,  600},
        {160, 1040}, {800,  1120}, {1440, 1040}
    };

    private static final int ROCK_COUNT = 20;
    private static final Random RNG = new Random();

    public World() {
        for (double[] c : HOLE_COORDS)
            holes.add(new ZombieHole(c[0], c[1], 4.0));

        // Лавка — верхний правый угол
        shop = new ShopNpcEntity(9999, WIDTH - 120, 120);

        // Камни — случайные позиции, не перекрываются с дырками и лавкой
        spawnRocks(ROCK_COUNT);
    }

    private void spawnRocks(int count) {
        for (int i = 0; i < count; i++) {
            double x, y;
            int attempts = 0;
            do {
                x = 100 + RNG.nextDouble() * (WIDTH  - 200);
                y = 100 + RNG.nextDouble() * (HEIGHT - 200);
                attempts++;
            } while (attempts < 20 && tooCloseToHoleOrShop(x, y));
            resourceNodes.add(new ResourceNodeEntity(nodeIdSeq.getAndIncrement(), x, y));
        }
    }

    private boolean tooCloseToHoleOrShop(double x, double y) {
        for (ZombieHole h : holes)
            if (Math.hypot(x - h.x, y - h.y) < 100) return true;
        if (shop != null) {
            TransformComponent st = shop.require(TransformComponent.class);
            if (Math.hypot(x - st.getCenterX(), y - st.getCenterY()) < 120) return true;
        }
        return false;
    }

    public void updateHost(double dt) {
        for (ResourceNodeEntity r : resourceNodes) r.update(dt);
        for (com.example.game.ecs.system.System s : systems) s.update(this, dt);
        // Респавн камней если осталось мало
        resourceNodes.removeIf(r -> !r.isAlive());
        if (resourceNodes.size() < 8) spawnRocks(5);
    }

    // ── спавн игрока ──────────────────────────────────────────────────────

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

    /** Покупка оружия: вызывается из GameScreen по кнопке UI. */
    public boolean buyWeapon(int playerId, ItemType item, int price) {
        PlayerEntity p = players.get(playerId);
        if (p == null) return false;
        return p.require(InventoryComponent.class).buyWeapon(item, price);
    }

    // ── геттеры ───────────────────────────────────────────────────────────

    public Map<Integer, PlayerEntity>  getPlayers()       { return players; }
    public List<BulletEntity>          getBullets()        { return bullets; }
    public List<ZombieEntity>          getZombies()        { return zombies; }
    public List<ZombieHole>            getHoles()          { return holes; }
    public List<ResourceNodeEntity>    getResourceNodes()  { return resourceNodes; }
    public ShopNpcEntity               getShop()           { return shop; }
    public int                         getWave()           { return wave; }
    public int                         getWidth()          { return WIDTH; }
    public int                         getHeight()         { return HEIGHT; }

    public void setWave(int w)            { this.wave = w; }
    public void addBullet(BulletEntity b) { bullets.add(b); }
    public void addZombie(ZombieEntity z) { zombies.add(z); }
    public int  nextZombieId()            { return zombieIdSeq.getAndIncrement(); }
}
