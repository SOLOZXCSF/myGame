package com.example.game.ecs.system;

import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.BulletEntity;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.ZombieEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Все коллизии за один проход.
 * Порядок:
 *   1. пуля игрока  → зомби
 *   2. пуля игрока  → другой игрок (PvP)
 *   3. зомби        → игрок (контакт)
 */
public class CollisionSystem implements System {

    @Override
    public void update(World world, double dt) {
        List<PlayerEntity> players = new ArrayList<>(world.getPlayers().values());
        List<ZombieEntity> zombies = new ArrayList<>(world.getZombies());
        List<BulletEntity> bullets = new ArrayList<>(world.getBullets());

        // 1. пуля → зомби
        for (BulletEntity b : bullets) {
            if (!b.isAlive() || b.getOwnerId() < 0) continue;
            for (ZombieEntity z : zombies) {
                if (!z.isAlive()) continue;
                if (overlaps(b, z)) {
                    b.kill();
                    HealthComponent zh = z.require(HealthComponent.class);
                    if (zh.hit(1)) {
                        z.kill();
                        PlayerEntity killer = world.getPlayers().get(b.getOwnerId());
                        if (killer != null) killer.addKill();
                    }
                }
            }
        }

        // 2. пуля → другой игрок (PvP)
        for (BulletEntity b : bullets) {
            if (!b.isAlive() || b.getOwnerId() < 0) continue;
            for (PlayerEntity p : players) {
                if (p.getId() == b.getOwnerId()) continue;
                if (overlaps(b, p)) {
                    b.kill();
                    HealthComponent ph = p.require(HealthComponent.class);
                    if (ph.hit(1)) world.respawn(p);
                }
            }
        }

        // 3. зомби → игрок
        for (ZombieEntity z : zombies) {
            if (!z.isAlive()) continue;
            for (PlayerEntity p : players) {
                if (overlaps(z, p)) {
                    HealthComponent ph = p.require(HealthComponent.class);
                    if (ph.hit(1)) world.respawn(p);
                    // отбросить зомби
                    TransformComponent zt = z.require(TransformComponent.class);
                    TransformComponent pt = p.require(TransformComponent.class);
                    zt.x -= (pt.x - zt.x) * 0.3;
                    zt.y -= (pt.y - zt.y) * 0.3;
                    zt.x = Math.max(0, Math.min(zt.x, world.getWidth()  - zt.width));
                    zt.y = Math.max(0, Math.min(zt.y, world.getHeight() - zt.height));
                }
            }
        }
    }

    private boolean overlaps(com.example.game.ecs.Entity a, com.example.game.ecs.Entity b) {
        TransformComponent ta = a.require(TransformComponent.class);
        TransformComponent tb = b.require(TransformComponent.class);
        return ta.getBounds().intersects(tb.getBounds());
    }
}
