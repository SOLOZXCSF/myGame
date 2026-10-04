package com.example.game.ecs.system;

import com.example.game.ecs.component.InventoryComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.component.WeaponComponent;
import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.ItemType;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.ResourceNodeEntity;
import com.example.game.input.KeyInput;

/**
 * Обрабатывает удар киркой:
 * – только когда pickaxeMode == true
 * – ЛКМ/ПРОБЕЛ + камень в радиусе REACH → -1 HP камню → drop Stone
 * – cooldown SWING_TIME секунд между ударами
 */
public class PickaxeSystem implements System {

    private static final double REACH      = 72;   // пикс от центра игрока
    private static final double SWING_TIME = 0.45;

    @Override
    public void update(World world, double dt) {
        for (PlayerEntity p : world.getPlayers().values()) {
            WeaponComponent    wp  = p.require(WeaponComponent.class);
            InventoryComponent inv = p.require(InventoryComponent.class);

            // обновить режим по активному слоту
            wp.pickaxeMode = inv.isPickaxeMode();
            if (!wp.pickaxeMode) continue;

            wp.cooldown -= dt;
            boolean swing = (p.require(com.example.game.ecs.component.InputComponent.class)
                              .inputMask & KeyInput.SHOOT) != 0;
            if (!swing || wp.cooldown > 0) continue;

            TransformComponent pt = p.require(TransformComponent.class);
            double cx = pt.getCenterX(), cy = pt.getCenterY();

            for (ResourceNodeEntity rock : world.getResourceNodes()) {
                if (!rock.isAlive()) continue;
                TransformComponent rt = rock.require(TransformComponent.class);
                double dx = rt.getCenterX() - cx, dy = rt.getCenterY() - cy;
                if (Math.hypot(dx, dy) > REACH) continue;

                rock.onHit();
                HealthComponent hp = rock.require(HealthComponent.class);
                if (hp.hit(1)) {
                    rock.kill();
                    inv.add(ItemType.STONE);
                }
                wp.cooldown = SWING_TIME;
                break;   // один удар — один камень за тик
            }
        }
    }
}
