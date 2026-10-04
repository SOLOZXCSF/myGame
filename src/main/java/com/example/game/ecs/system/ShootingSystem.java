package com.example.game.ecs.system;

import com.example.game.ecs.component.InputComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.component.WeaponComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.BulletEntity;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.WeaponType;
import com.example.game.input.KeyInput;

/**
 * Создаёт пули когда игрок стреляет.
 * Прицел берётся из WeaponComponent.aimDX/DY,
 * которые обновляются из мышки в GameScreen.
 */
public class ShootingSystem implements System {

    private static final int GUN_LEN = 18;

    @Override
    public void update(World world, double dt) {
        for (PlayerEntity p : world.getPlayers().values()) {
            WeaponComponent    wp  = p.require(WeaponComponent.class);
            TransformComponent tr  = p.require(TransformComponent.class);
            InputComponent     inp = p.require(InputComponent.class);

            wp.cooldown -= dt;

            // прицел из мыши
            wp.setAim(inp.mouseWorldX, inp.mouseWorldY, tr.getCenterX(), tr.getCenterY());
            wp.firing = (inp.inputMask & KeyInput.SHOOT) != 0;

            if (wp.firing && wp.cooldown <= 0) {
                WeaponType w = WeaponType.forOrdinal(wp.weaponOrdinal);
                wp.cooldown = w.shootDelay;

                double ndx = wp.aimDX, ndy = wp.aimDY;
                double cx  = tr.getCenterX(), cy = tr.getCenterY();

                if (w == WeaponType.SHOTGUN) {
                    for (int deg : new int[]{-20, 0, 20}) {
                        double rad = Math.toRadians(deg);
                        double sdx = ndx * Math.cos(rad) - ndy * Math.sin(rad);
                        double sdy = ndx * Math.sin(rad) + ndy * Math.cos(rad);
                        world.addBullet(new BulletEntity(p.getId(), cx, cy, sdx, sdy, w));
                    }
                } else {
                    world.addBullet(new BulletEntity(p.getId(), cx, cy, ndx, ndy, w));
                }
            }
        }
    }
}
