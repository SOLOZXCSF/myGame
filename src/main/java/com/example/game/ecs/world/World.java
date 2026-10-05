package com.example.game.ecs.world;

import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.InventoryComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.system.*;
import com.example.game.entity.*;
import com.example.game.map.GameMap;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class World {

    // Размер кратен TILE_SIZE=32: 50 * 32 = 1600, 38 * 32 = 1216
    public static final int WIDTH  = 1600;
    public static final int HEIGHT = 1216;

    private final Map<Integer, PlayerEntity>  players       = new ConcurrentHashMap<>();
    private final List<BulletEntity>          bullets       = new CopyOnWriteArrayList<>();
    private final List<ZombieEntity>          zombies       = new CopyOnWriteArrayList<>();
    private final List<ZombieHole>            holes         = new CopyOnWriteArrayList<>();
    private final List<ResourceNodeEntity>    resourceNodes = new CopyOnWriteArrayList<>();
    private ShopNpcEntity                     shop;

    private final GameMap gameMap;

    private final AtomicInteger zombieIdSeq = new AtomicInteger(0);
    private final AtomicInteger nodeIdSeq   = new AtomicInteger(1000);
    private volatile int wave = 1;

    private final List<com.example.game.ecs.system.System> systems = List.of(
            new MovementSystem(),
            new ShootingSystem(),
            new PickaxeSystem(),
            new ZombieAiSystem(),
            new BulletMoveSystem(),
            new CollisionSystem(),
            new ShopSystem(),
            new WaveSystem(),
            new CleanupSystem()
    );

    // Дырки — только на проходимых тайлах (дороги/трава)
    private static final double[][] HOLE_COORDS = {
        {160,  192}, {800,  160}, {1408,  192},   // верх
        { 96,  608}, {1472,  608},                 // середина
        {160, 1024}, {800, 1088}, {1408, 1024}    // низ
    };

    private static final int ROCK_COUNT = 22;
    private static final Random RNG = new Random();

    public World() {
        // Карта с фиксированным сидом — одинаковая у всех в сессии
        this.gameMap = new GameMap(WIDTH, HEIGHT, 42L);

        // Дырки
        for (double[] c : HOLE_COORDS)
            holes.add(new ZombieHole(c[0], c[1], 4.0));

        // Лавка — правая сторона, рядом с кольцевой дорогой
        shop = new ShopNpcEntity(9999, WIDTH - 130, 130);

        // Камни — только на проходимых тайлах
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
            } while (attempts < 30 && (tooClose(x, y) || gameMap.isSolid(x + 12, y + 12)));
            resourceNodes.add(new ResourceNodeEntity(nodeIdSeq.getAndIncrement(), x, y));
        }
    }

    private boolean tooClose(double x, double y) {
        for (ZombieHole h : holes)
            if (Math.hypot(x - h.x, y - h.y) < 100) return true;
        if (shop != null) {
            TransformComponent st = shop.require(TransformComponent.class);
            if (Math.hypot(x - st.getCenterX(), y - st.getCenterY()) < 130) return true;
        }
        return false;
    }

    public void updateHost(double dt) {
        if (shop != null) shop.update(dt);
        for (ResourceNodeEntity r : resourceNodes) r.update(dt);
        for (com.example.game.ecs.system.System s : systems) s.update(this, dt);
        resourceNodes.removeIf(r -> !r.isAlive());
        if (resourceNodes.size() < 8) spawnRocks(5);
    }

    // ── спавн ──────────────────────────────────────────────────────────────

    public PlayerEntity spawn(int id) {
        // Спавн на центральной дороге
        double baseX = WIDTH  / 2.0 + (id % 3 - 1) * 80;
        double baseY = HEIGHT / 2.0 + (id / 3)     * 80;
        double[] pos = gameMap.findSpawnNear(baseX, baseY);
        PlayerEntity p = EntityFactory.player(id, pos[0], pos[1]);
        players.put(id, p);
        return p;
    }

    public void respawn(PlayerEntity p) {
        TransformComponent tr = p.require(TransformComponent.class);
        HealthComponent    hp = p.require(HealthComponent.class);
        hp.heal();
        double baseX = WIDTH  / 2.0 + (p.getId() % 3 - 1) * 80 + 40;
        double baseY = HEIGHT / 2.0 + (p.getId() / 3)     * 80 + 40;
        double[] pos = gameMap.findSpawnNear(baseX, baseY);
        tr.x = pos[0]; tr.y = pos[1];
    }

    public boolean buyWeapon(int playerId, ItemType item, int price) {
        PlayerEntity p = players.get(playerId);
        if (p == null) return false;
        return p.require(InventoryComponent.class).buyWeapon(item, price);
    }

    // ── геттеры ────────────────────────────────────────────────────────────

    public Map<Integer, PlayerEntity>  getPlayers()       { return players; }
    public List<BulletEntity>          getBullets()        { return bullets; }
    public List<ZombieEntity>          getZombies()        { return zombies; }
    public List<ZombieHole>            getHoles()          { return holes; }
    public List<ResourceNodeEntity>    getResourceNodes()  { return resourceNodes; }
    public ShopNpcEntity               getShop()           { return shop; }
    public GameMap                     getGameMap()        { return gameMap; }
    public int                         getWave()           { return wave; }
    public int                         getWidth()          { return WIDTH; }
    public int                         getHeight()         { return HEIGHT; }

    public void setWave(int w)            { this.wave = w; }
    public void addBullet(BulletEntity b) { bullets.add(b); }
    public void addZombie(ZombieEntity z) { zombies.add(z); }
    public int  nextZombieId()            { return zombieIdSeq.getAndIncrement(); }
}
