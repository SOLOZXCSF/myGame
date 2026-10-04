package com.example.game.ecs.component;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Всё что нужно для отрисовки: цвет/картинка + порядок отрисовки.
 * Если image != null — рисуется картинка, иначе fillRect цветом.
 */
public class RenderComponent implements Component {

    public Color         color;
    public BufferedImage image;    // null → рисовать цветом
    public int           zIndex;  // порядок: 0=фон, 10=пули, 20=зомби, 30=игроки
    public boolean       visible  = true;

    public RenderComponent(Color color, int zIndex) {
        this.color  = color;
        this.zIndex = zIndex;
    }
}
