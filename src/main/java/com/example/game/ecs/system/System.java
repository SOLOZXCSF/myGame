package com.example.game.ecs.system;

import com.example.game.ecs.world.World;

/**
 * Интерфейс игровой системы.
 * Системы вызываются в World.updateHost() строго по порядку.
 *
 * Порядок имеет значение:
 *   1. MovementSystem   — двигаем игроков
 *   2. ShootingSystem   — создаём пули
 *   3. BulletSystem     — двигаем пули
 *   4. ZombieAiSystem   — двигаем зомби
 *   5. CollisionSystem  — все столкновения
 *   6. WaveSystem       — спавн новых зомби
 *   7. CleanupSystem    — removeIf(!alive)
 */
public interface System {
    void update(World world, double dt);
}
