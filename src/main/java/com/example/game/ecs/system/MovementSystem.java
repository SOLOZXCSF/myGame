package com.example.game.ecs.system;

import com.example.game.ecs.component.InputComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.PlayerEntity;
import com.example.game.input.KeyInput;

/** Двигает игроков по inputMask, зажимает в границах мира. */
public class MovementSystem implements System {

    private static final double SPEED = 250; // пикс/с

    @Override
    public void update(World world, double dt) {
        for (PlayerEntity p : world.getPlayers().values()) {
            InputComponent     inp = p.require(InputComponent.class);
            TransformComponent tr  = p.require(TransformComponent.class);

            int mask = inp.inputMask;
            double dx = bit(mask, KeyInput.RIGHT) - bit(mask, KeyInput.LEFT);
            double dy = bit(mask, KeyInput.DOWN)  - bit(mask, KeyInput.UP);

            if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }

            tr.x = clamp(tr.x + dx * SPEED * dt, 0, world.getWidth()  - tr.width);
            tr.y = clamp(tr.y + dy * SPEED * dt, 0, world.getHeight() - tr.height);
        }
    }

    private int    bit(int mask, int flag)             { return (mask & flag) != 0 ? 1 : 0; }
    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(v, hi)); }
}
