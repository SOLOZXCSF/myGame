package com.example.game.ecs.component;

/**
 * Состояние ввода, привязанное к сущности.
 * Заполняется KeyInput + MouseAimInput и передаётся по сети как два числа:
 *   inputMask  — биты направления + огонь
 *   mouseWorldX, mouseWorldY — мировые координаты курсора
 */
public class InputComponent implements Component {

    public volatile int    inputMask   = 0;
    public volatile double mouseWorldX = 0;
    public volatile double mouseWorldY = 0;
}
