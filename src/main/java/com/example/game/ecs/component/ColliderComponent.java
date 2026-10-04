package com.example.game.ecs.component;

/**
 * Участие в коллизиях.
 * PLAYER_BULLET бьёт ZOMBIE; ZOMBIE касается PLAYER.
 * CollisionSystem проверяет пары по группам.
 */
public class ColliderComponent implements Component {

    public enum Group { PLAYER, ZOMBIE, PLAYER_BULLET, ZOMBIE_BULLET, NONE }

    public final Group group;

    /** Процент уменьшения хитбокса (0 = совпадает с transform, 0.2 = на 20% меньше). */
    public final double shrink;

    public ColliderComponent(Group group) {
        this(group, 0.0);
    }

    public ColliderComponent(Group group, double shrink) {
        this.group  = group;
        this.shrink = shrink;
    }
}
