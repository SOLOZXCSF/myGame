package com.example.game.ecs.system;

import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.ZombieEntity;

/** Зомби ищет ближайшего игрока и идёт к нему. */
public class ZombieAiSystem implements System {

    @Override
    public void update(World world, double dt) {
        for (ZombieEntity z : world.getZombies()) {
            if (!z.isAlive()) continue;
            TransformComponent zt = z.require(TransformComponent.class);

            PlayerEntity target = closest(world, zt);
            if (target == null) continue;

            TransformComponent pt = target.require(TransformComponent.class);
            double dx  = pt.getCenterX() - zt.getCenterX();
            double dy  = pt.getCenterY() - zt.getCenterY();
            double len = Math.hypot(dx, dy);
            if (len < 1) continue;

            double spd = z.getSpeed();
            zt.x = clamp(zt.x + (dx / len) * spd * dt, 0, world.getWidth()  - zt.width);
            zt.y = clamp(zt.y + (dy / len) * spd * dt, 0, world.getHeight() - zt.height);
        }
    }

    private PlayerEntity closest(World world, TransformComponent from) {
        PlayerEntity best = null;
        double minD = Double.MAX_VALUE;
        for (PlayerEntity p : world.getPlayers().values()) {
            TransformComponent pt = p.require(TransformComponent.class);
            double d = Math.hypot(pt.getCenterX() - from.getCenterX(),
                                  pt.getCenterY() - from.getCenterY());
            if (d < minD) { minD = d; best = p; }
        }
        return best;
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(v, hi)); }
}
