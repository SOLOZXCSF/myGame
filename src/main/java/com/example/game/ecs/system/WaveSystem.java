package com.example.game.ecs.system;

import com.example.game.ecs.world.World;
import com.example.game.ecs.world.ZombieHole;
import com.example.game.entity.ZombieEntity;

/** Управляет волнами: ускоряет спавн, создаёт зомби из дырок. */
public class WaveSystem implements System {

    private static final double WAVE_DURATION = 30.0;

    private int    wave  = 1;
    private double timer = 0;

    @Override
    public void update(World world, double dt) {
        timer += dt;
        if (timer >= WAVE_DURATION) {
            timer -= WAVE_DURATION;
            wave++;
            double interval = Math.max(0.5, 4.0 - wave * 0.25);
            for (ZombieHole h : world.getHoles()) h.setSpawnInterval(interval);
        }

        if (world.getPlayers().isEmpty()) return;

        for (ZombieHole h : world.getHoles()) {
            if (h.update(dt)) {
                int    hp  = 1 + wave / 3;
                double spd = Math.min(55 + wave * 8, 160);
                world.addZombie(new ZombieEntity(world.nextZombieId(), h.x, h.y, hp, spd));
            }
        }

        world.setWave(wave);
    }
}
