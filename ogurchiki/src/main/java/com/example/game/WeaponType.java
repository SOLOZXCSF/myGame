package com.example.game;

import java.awt.Color;

/**
 * Виды оружия, открываемых по убийствам зомби.
 *
 *  PISTOL   — стартовое (пистолет)
 *  SHOTGUN  — дробовик,  3 пули веером      (10 убийств)
 *  RIFLE    — штурмовая, быстрый огонь      (100 убийств)
 *  ROCKET   — ракетница, большая пуля       (200 убийств)
 */
public enum WeaponType {
    //          name         delay  bulletSpeed  bulletRadius  color               killsNeeded
    PISTOL  ("Пистолет",    0.35,  520,          5,  Color.YELLOW,           0),
    SHOTGUN ("Дробовик",    0.55,  440,          5,  Color.ORANGE,          10),
    RIFLE   ("Штурмовая",   0.10,  680,          4,  new Color(100,255,100), 100),
    ROCKET  ("Пулемет",   0.05,  680,         4,  new Color(255, 80, 30), 200);

    public final String name;
    public final double shootDelay;
    public final double bulletSpeed;
    public final int    bulletRadius;
    public final Color  bulletColor;
    public final int    killsNeeded;

    WeaponType(String name, double shootDelay, double bulletSpeed,
               int bulletRadius, Color bulletColor, int killsNeeded) {
        this.name         = name;
        this.shootDelay   = shootDelay;
        this.bulletSpeed  = bulletSpeed;
        this.bulletRadius = bulletRadius;
        this.bulletColor  = bulletColor;
        this.killsNeeded  = killsNeeded;
    }

    /** Лучшее оружие, доступное при данном числе убийств. */
    public static WeaponType forKills(int kills) {
        WeaponType best = PISTOL;
        for (WeaponType w : values()) {
            if (kills >= w.killsNeeded) best = w;
        }
        return best;
    }
}
