package com.example.game.ecs.system;

import com.example.game.ecs.world.World;

/** Двигает все активные пули. */
public class BulletMoveSystem implements System {
    @Override
    public void update(World world, double dt) {
        for (var b : world.getBullets()) {
            if (b.isAlive()) b.move(dt, world.getWidth(), world.getHeight());
        }
    }
}
