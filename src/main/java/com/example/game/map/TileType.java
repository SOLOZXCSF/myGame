package com.example.game.map;

import java.awt.*;

/**
 * Тип тайла. Каждый тайл знает свой цвет, можно ли пройти и как рисоваться.
 */
public enum TileType {

    // Базовые поверхности
    GRASS       (true,  new Color(62,  110,  55)),   // трава — основной фон
    GRASS_DARK  (true,  new Color(48,   90,  42)),   // тёмная трава — разнообразие
    DIRT        (true,  new Color(120,  88,  60)),   // земля / тропинка
    DIRT_DARK   (true,  new Color(100,  72,  48)),
    ROAD        (true,  new Color(90,   85,  80)),   // мощёная дорога
    ROAD_MARK   (true,  new Color(105, 100,  95)),   // светлая плитка на дороге

    // Непроходимые
    WALL        (false, new Color(65,   60,  55)),   // каменная стена
    WALL_TOP    (false, new Color(85,   80,  75)),   // верхушка стены (светлее)
    WALL_MOSS   (false, new Color(55,   72,  48)),   // стена с мхом
    WATER       (false, new Color(30,   70, 130)),   // вода
    WATER_FOAM  (false, new Color(70,  120, 180));   // пена воды

    public final boolean passable;
    public final Color   baseColor;

    TileType(boolean passable, Color baseColor) {
        this.passable  = passable;
        this.baseColor = baseColor;
    }
}
