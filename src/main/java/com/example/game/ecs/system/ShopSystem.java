package com.example.game.ecs.system;

import com.example.game.ecs.component.InventoryComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.*;
import com.example.game.input.KeyInput;

/**
 * При нажатии F рядом с торговцем:
 *   1. Продаёт все камни → gold.
 *   2. Открывает флаг shopOpen на игроке → GameScreen рисует меню.
 *
 * Покупка оружия вызывается через World.buyWeapon(playerId, item).
 */
public class ShopSystem implements System {

    @Override
    public void update(World world, double dt) {
        ShopNpcEntity shop = world.getShop();
        if (shop == null) return;

        shop.update(dt);

        for (PlayerEntity p : world.getPlayers().values()) {
            TransformComponent pt = p.require(TransformComponent.class);
            InventoryComponent inv = p.require(InventoryComponent.class);

            boolean near = shop.isNear(pt.getCenterX(), pt.getCenterY());
            int mask = p.require(com.example.game.ecs.component.InputComponent.class).inputMask;
            boolean fPressed = (mask & KeyInput.INTERACT) != 0;

            if (near && fPressed) {
                // Продать камни
                int earned = inv.sellAll(ItemType.STONE, ShopNpcEntity.STONE_SELL_PRICE);
                // флаг для UI (GameScreen покажет меню)
                inv.shopMenuOpen = true;
            } else if (!near) {
                inv.shopMenuOpen = false;
            }
        }
    }
}
