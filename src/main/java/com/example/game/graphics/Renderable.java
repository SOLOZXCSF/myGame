package com.example.game.graphics;

import java.awt.Graphics2D;

/**
 * Всё что умеет рисовать себя в координатах мира.
 * {@link Camera} применяет translate() до вызова render(),
 * поэтому реализации работают в мировых координатах.
 */
public interface Renderable {

    /**
     * Порядок отрисовки (меньше = рисуется раньше).
     * 0   — фон/дырки
     * 10  — пули
     * 20  — зомби
     * 30  — игроки
     * 100 — HUD (рисуется отдельно, без camera-translate)
     */
    int zIndex();

    void render(Graphics2D g);
}
