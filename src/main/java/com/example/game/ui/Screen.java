package com.example.game.ui;

import java.awt.Graphics2D;

/**
 * Единый контракт для всех экранов.
 *
 *  MenuScreen   — главное меню
 *  GameScreen   — игровое поле
 *  (в будущем)  — PauseScreen, LobbyScreen, …
 *
 * ScreenManager вызывает методы активного экрана.
 * Экраны не знают о JFrame и не управляют циклом.
 */
public interface Screen {

    /** Вызывается когда экран становится активным. */
    void onEnable();

    /** Вызывается когда экран деактивируется (переключение или закрытие). */
    void onDisable();

    /**
     * Логический tick. dt — секунды с прошлого тика (обычно 1/60).
     * Вызывается из игрового потока, НЕ из EDT.
     */
    void update(double dt);

    /**
     * Отрисовка. Вызывается из paintComponent → тоже НЕ EDT в большинстве случаев.
     * @param g Graphics2D панели (без camera-translate; экран сам решает)
     * @param width  ширина панели в пикселях
     * @param height высота панели в пикселях
     */
    void render(Graphics2D g, int width, int height);

    /**
     * Возвращает курсор, который должен показываться на этом экране.
     * Null = системный курсор по умолчанию.
     */
    default java.awt.Cursor preferredCursor() { return null; }
}
