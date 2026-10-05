package com.example.game.ecs.system;

import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.ZombieEntity;
import com.example.game.map.GameMap;

/** Зомби ищет ближайшего игрока и идёт к нему, огибая стены. */
public class ZombieAiSystem implements System {

    @Override
    public void update(World world, double dt) {
        GameMap map = world.getGameMap();

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
            double ndx = dx / len, ndy = dy / len;
            double nx = zt.x + ndx * spd * dt;
            double ny = zt.y + ndy * spd * dt;

            // Ограничение границами мира
            nx = clamp(nx, 0, world.getWidth()  - zt.width);
            ny = clamp(ny, 0, world.getHeight() - zt.height);

            if (map != null) {
                // Попробуем прямо, потом по X, потом по Y
                if (!solidAABB(map, nx, ny, zt.width, zt.height)) {
                    zt.x = nx; zt.y = ny;
                } else if (!solidAABB(map, nx, zt.y, zt.width, zt.height)) {
                    zt.x = nx;
                } else if (!solidAABB(map, zt.x, ny, zt.width, zt.height)) {
                    zt.y = ny;
                }
                // иначе стоим (упёрлись в стену)
            } else {
                zt.x = nx; zt.y = ny;
            }
        }
    }

    private boolean solidAABB(GameMap map, double x, double y, int w, int h) {
        int s = 4;
        return map.isSolid(x+s, y+s) || map.isSolid(x+w-s, y+s)
            || map.isSolid(x+s, y+h-s) || map.isSolid(x+w-s, y+h-s);
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
