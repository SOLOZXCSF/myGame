package com.example.game.entity;

import java.awt.Color;

/**
 * Предметы инвентаря.
 * PICKAXE  — инструмент (слот 0, не расходуется).
 * STONE    — ресурс (добывается, продаётся).
 * Оружия   — покупаются в лавке, лежат в слотах.
 */
public enum ItemType {
    // ── инструмент ─────────────────────────────────────────────────────────
    PICKAXE     ("⛏", new Color(180, 140,  80), "Кирка",       false, true),

    // ── ресурсы ────────────────────────────────────────────────────────────
    STONE       ("◆", new Color(160, 160, 170), "Камень",      true,  false),

    // ── оружия (покупаются в лавке, активируют firearm-режим) ─────────────
    WPN_PISTOL  ("🔫", new Color(255, 220,  50), "Пистолет",   false, false),
    WPN_SHOTGUN ("⊞", new Color(255, 150,  30), "Дробовик",   false, false),
    WPN_RIFLE   ("╪", new Color(100, 255, 100), "Штурмовая",  false, false),
    WPN_ROCKET  ("✦", new Color(255,  80,  30), "Ракетница",  false, false);

    public final String  symbol;
    public final Color   color;
    public final String  name;
    /** true → предмет стакается (можно хранить несколько в одном слоте). */
    public final boolean stackable;
    /** true → предмет не исчезает при «использовании». */
    public final boolean persistent;

    ItemType(String symbol, Color color, String name,
             boolean stackable, boolean persistent) {
        this.symbol     = symbol;
        this.color      = color;
        this.name       = name;
        this.stackable  = stackable;
        this.persistent = persistent;
    }

    /** Соответствующий WeaponType для оружий-предметов (null если не оружие). */
    public WeaponType toWeaponType() {
        return switch (this) {
            case WPN_PISTOL  -> WeaponType.PISTOL;
            case WPN_SHOTGUN -> WeaponType.SHOTGUN;
            case WPN_RIFLE   -> WeaponType.RIFLE;
            case WPN_ROCKET  -> WeaponType.ROCKET;
            default          -> null;
        };
    }

    public boolean isWeapon() { return toWeaponType() != null; }

    public static ItemType fromWeaponType(WeaponType w) {
        return switch (w) {
            case PISTOL  -> WPN_PISTOL;
            case SHOTGUN -> WPN_SHOTGUN;
            case RIFLE   -> WPN_RIFLE;
            case ROCKET  -> WPN_ROCKET;
        };
    }
}
