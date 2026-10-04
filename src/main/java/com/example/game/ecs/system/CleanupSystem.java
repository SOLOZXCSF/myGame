package com.example.game.ecs.system;

import com.example.game.ecs.world.World;

/** Удаляет мёртвые сущности в конце каждого тика. */
public class CleanupSystem implements System {

    @Override
    public void update(World world, double dt) {
        world.getBullets().removeIf(b -> !b.isAlive());
        world.getZombies().removeIf(z -> !z.isAlive());
    }
}
