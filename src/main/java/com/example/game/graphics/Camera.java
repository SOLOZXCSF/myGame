package com.example.game.graphics;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Камера — смещает систему координат Graphics2D так,
 * чтобы точка (targetX, targetY) оказалась в центре экрана.
 *
 * Использование:
 *   camera.follow(player.getCenterX(), player.getCenterY());
 *   Graphics2D world = camera.begin(g);   // g теперь в мировых координатах
 *   // ... рисуем объекты ...
 *   camera.end(g, savedTransform);
 */
public class Camera {

    private final int viewW;   // ширина  панели (пикс)
    private final int viewH;   // высота  панели (пикс)
    private final int worldW;  // ширина  мира   (пикс)
    private final int worldH;  // высота  мира   (пикс)

    // текущее смещение (верхний-левый угол viewport в мировых координатах)
    private double offsetX;
    private double offsetY;

    // плавность следования (0 = жёстко, 1 = мгновенно)
    private static final double LERP = 0.12;

    public Camera(int viewW, int viewH, int worldW, int worldH) {
        this.viewW  = viewW;
        this.viewH  = viewH;
        this.worldW = worldW;
        this.worldH = worldH;
    }

    /**
     * Сообщить камере, куда следить. Обычно вызывается из update().
     * @param targetX  мировая X цели (обычно центр игрока)
     * @param targetY  мировая Y цели
     */
    public void follow(double targetX, double targetY) {
        double desiredX = targetX - viewW / 2.0;
        double desiredY = targetY - viewH / 2.0;

        // плавное следование
        offsetX += (desiredX - offsetX) * LERP;
        offsetY += (desiredY - offsetY) * LERP;

        // зажать в границах мира (не показываем чёрные полосы)
        offsetX = Math.max(0, Math.min(offsetX, worldW - viewW));
        offsetY = Math.max(0, Math.min(offsetY, worldH - viewH));
    }

    /**
     * Применить camera-translate. Возвращает сохранённый transform
     * (передайте его в end()).
     */
    public AffineTransform begin(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(-offsetX, -offsetY);
        return saved;
    }

    /** Откатить transform после отрисовки мира. */
    public void end(Graphics2D g, AffineTransform saved) {
        g.setTransform(saved);
    }

    /** Перевести экранные координаты в мировые (нужно для прицела мышью). */
    public double screenToWorldX(double screenX) { return screenX + offsetX; }
    public double screenToWorldY(double screenY) { return screenY + offsetY; }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
}
