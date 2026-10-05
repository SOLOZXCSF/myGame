package com.example.game.ecs.system;

import com.example.game.ecs.component.InputComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.PlayerEntity;
import com.example.game.input.KeyInput;
import com.example.game.map.GameMap;

/**
 * Двигает игроков + разрешает коллизии со стенами карты.
 * Движение по осям независимо — можно скользить вдоль стены.
 */
public class MovementSystem implements System {

    private static final double SPEED = 250;

    @Override
    public void update(World world, double dt) {
        GameMap map = world.getGameMap();

        for (PlayerEntity p : world.getPlayers().values()) {
            InputComponent     inp = p.require(InputComponent.class);
            TransformComponent tr  = p.require(TransformComponent.class);

            int mask = inp.inputMask;
            double dx = bit(mask, KeyInput.RIGHT) - bit(mask, KeyInput.LEFT);
            double dy = bit(mask, KeyInput.DOWN)  - bit(mask, KeyInput.UP);
            if (dx != 0 && dy != 0) { dx *= 0.7071; dy *= 0.7071; }

            double newX = tr.x + dx * SPEED * dt;
            double newY = tr.y + dy * SPEED * dt;

            // Зажать в мировых границах
            newX = clamp(newX, 0, world.getWidth()  - tr.width);
            newY = clamp(newY, 0, world.getHeight() - tr.height);

            if (map != null) {
                // Движение по X независимо от Y — скольжение вдоль стен
                if (!collidesWithMap(map, newX, tr.y, tr.width, tr.height)) {
                    tr.x = newX;
                } else {
                    // попробуем хотя бы частично сдвинуться
                    double slide = dx * SPEED * dt * 0.3;
                    if (!collidesWithMap(map, tr.x + slide, tr.y, tr.width, tr.height))
                        tr.x += slide;
                }

                if (!collidesWithMap(map, tr.x, newY, tr.width, tr.height)) {
                    tr.y = newY;
                } else {
                    double slide = dy * SPEED * dt * 0.3;
                    if (!collidesWithMap(map, tr.x, tr.y + slide, tr.width, tr.height))
                        tr.y += slide;
                }
            } else {
                tr.x = newX;
                tr.y = newY;
            }
        }
    }

    /** Проверить AABB (x, y, w, h) на попадание в непроходимый тайл. */
    private boolean collidesWithMap(GameMap map, double x, double y, int w, int h) {
        // Слегка уменьшаем хитбокс (на 4px с каждой стороны) для удобства прохода
        int shrink = 4;
        double x0 = x + shrink,   y0 = y + shrink;
        double x1 = x + w - shrink, y1 = y + h - shrink;
        return map.isSolid(x0, y0) || map.isSolid(x1, y0)
            || map.isSolid(x0, y1) || map.isSolid(x1, y1);
    }

    private int    bit(int mask, int flag)              { return (mask & flag) != 0 ? 1 : 0; }
    private double clamp(double v, double lo, double hi){ return Math.max(lo, Math.min(v, hi)); }
}
