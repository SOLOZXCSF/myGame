package com.example.game.ecs.component;

/**
 * HP + анимация «мигания» при ударе.
 */
public class HealthComponent implements Component {

    public volatile int hp;
    public final    int maxHp;
    public          double hitFlash = 0;   // секунды оставшегося белого мигания

    public HealthComponent(int maxHp) {
        this.maxHp = maxHp;
        this.hp    = maxHp;
    }

    /** Нанести урон. Возвращает true если HP дошли до 0. */
    public boolean hit(int dmg) {
        hp       -= dmg;
        hitFlash  = 0.15;
        return hp <= 0;
    }

    public void heal()   { hp = maxHp; }
    public boolean dead() { return hp <= 0; }
}
