package com.example.game.ecs.component;

/**
 * Оружие и прицел.
 * pickaxeMode = true → ShootingSystem не стреляет; PickaxeSystem атакует.
 */
public class WeaponComponent implements Component {

    public volatile float   aimDX         = 1f;
    public volatile float   aimDY         = 0f;
    public volatile int     weaponOrdinal = 0;   // WeaponType.ordinal()
    public          double  cooldown      = 0;
    public volatile boolean firing        = false;
    public volatile boolean pickaxeMode   = false;  // ← NEW

    public void setAim(double worldMouseX, double worldMouseY,
                       double ownerCX,     double ownerCY) {
        double dx  = worldMouseX - ownerCX;
        double dy  = worldMouseY - ownerCY;
        double len = Math.hypot(dx, dy);
        if (len > 1) { aimDX = (float)(dx/len); aimDY = (float)(dy/len); }
    }
}
