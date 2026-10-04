package com.example.game.entity;

/**
 * Единая точка создания сущностей.
 * Добавить новый тип = добавить один метод здесь.
 */
public final class EntityFactory {

    private EntityFactory() {}

    public static PlayerEntity player(int id, double x, double y) {
        return new PlayerEntity(id, x, y);
    }

    public static ZombieEntity zombie(int id, double cx, double cy, int hp, double speed) {
        return new ZombieEntity(id, cx, cy, hp, speed);
    }

    public static BulletEntity bullet(int ownerId, double cx, double cy,
                                      double ndx, double ndy, WeaponType w) {
        return new BulletEntity(ownerId, cx, cy, ndx, ndy, w);
    }

    public static ResourceNodeEntity resourceNode(int id, double x, double y) {
        return new ResourceNodeEntity(id, x, y);
    }

    public static ShopNpcEntity shopNpc(int id, double cx, double cy) {
        return new ShopNpcEntity(id, cx, cy);
    }
}
