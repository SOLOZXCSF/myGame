package com.example.game.ecs.component;

import com.example.game.entity.ItemType;

/**
 * Инвентарь игрока: 6 слотов + кошелёк (gold).
 *
 * Слот 0  — всегда Кирка (PICKAXE, persistent).
 * Слоты 1-5 — свободные (оружия, камни и т.д.).
 *
 * activeSlot:
 *   == 0          → режим кирки  (pickaxeMode = true)
 *   == 1..5, оружие → режим firearm
 */
public class InventoryComponent implements Component {

    public static final int SLOTS = 6;

    private final ItemType[] items  = new ItemType[SLOTS];
    private final int[]      counts = new int[SLOTS];   // кол-во для стакающихся
    public  volatile int     activeSlot   = 0;
    public  volatile int     gold         = 0;
    public  volatile boolean shopMenuOpen = false;

    public InventoryComponent() {
        // Слот 0 — кирка, всегда
        items[0]  = ItemType.PICKAXE;
        counts[0] = 1;
    }

    // ── режим ────────────────────────────────────────────────────────────

    public boolean isPickaxeMode() { return activeSlot == 0; }

    public void setActive(int slot) {
        if (slot >= 0 && slot < SLOTS) activeSlot = slot;
    }

    // ── добавление ───────────────────────────────────────────────────────

    /**
     * Положить предмет. Стакающиеся ищут уже занятый слот того же типа.
     * Возвращает true если успешно.
     */
    public boolean add(ItemType item) {
        if (item.stackable) {
            for (int i = 0; i < SLOTS; i++) {
                if (items[i] == item) { counts[i]++; return true; }
            }
        }
        // свободный слот (кроме 0 — там кирка)
        for (int i = 1; i < SLOTS; i++) {
            if (items[i] == null) { items[i] = item; counts[i] = 1; return true; }
        }
        return false;
    }

    // ── удаление / использование ─────────────────────────────────────────

    /** Использовать/взять из слота. Persistent-предметы не удаляются. */
    public ItemType use(int idx) {
        if (idx < 0 || idx >= SLOTS || items[idx] == null) return null;
        ItemType it = items[idx];
        if (it.persistent) return it;        // кирка остаётся
        counts[idx]--;
        if (counts[idx] <= 0) { items[idx] = null; counts[idx] = 0; }
        return it;
    }

    public ItemType useActive() { return use(activeSlot); }

    // ── запрос ───────────────────────────────────────────────────────────

    public ItemType get(int idx)   { return (idx >= 0 && idx < SLOTS) ? items[idx]  : null; }
    public int      count(int idx) { return (idx >= 0 && idx < SLOTS) ? counts[idx] : 0;    }

    /** Суммарное кол-во предметов данного типа по всем слотам. */
    public int countOf(ItemType type) {
        int total = 0;
        for (int i = 0; i < SLOTS; i++)
            if (items[i] == type) total += counts[i];
        return total;
    }

    /**
     * Купить оружие за золото. Возвращает true если успешно.
     * Проверяет наличие денег и свободного слота.
     */
    public boolean buyWeapon(ItemType weaponItem, int price) {
        if (gold < price) return false;
        // уже есть такое оружие?
        for (int i = 1; i < SLOTS; i++) if (items[i] == weaponItem) return false;
        if (!add(weaponItem)) return false;
        gold -= price;
        return true;
    }

    /** Продать все камни. Возвращает вырученное золото. */
    public int sellAll(ItemType type, int priceEach) {
        int earned = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (items[i] == type) {
                earned    += counts[i] * priceEach;
                items[i]   = null;
                counts[i]  = 0;
            }
        }
        gold += earned;
        return earned;
    }
}
