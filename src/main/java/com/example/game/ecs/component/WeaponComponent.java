package com.example.game.ecs.component;

/**
 * Оружие и прицел.
 * aimDX/aimDY — нормализованный вектор направления стрельбы.
 * Обновляется мышью (MouseAimInput) или по WASD-направлению.
 */
public class WeaponComponent implements Component {

    public volatile float aimDX = 1f;
    public volatile float aimDY = 0f;
    public volatile int   weaponOrdinal = 0;   // WeaponType.ordinal()

    public double cooldown = 0;  // секунд до следующего выстрела

    /** true если в этом тике нажата кнопка огня. */
    public volatile boolean firing = false;

    public void setAim(double worldMouseX, double worldMouseY,
                       double ownerCX,     double ownerCY) {
        double dx  = worldMouseX - ownerCX;
        double dy  = worldMouseY - ownerCY;
        double len = Math.hypot(dx, dy);
        if (len > 1) { aimDX = (float)(dx/len); aimDY = (float)(dy/len); }
    }
}
