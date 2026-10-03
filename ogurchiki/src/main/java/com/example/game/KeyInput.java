package com.example.game;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит зажатые клавиши и сворачивает их в битовую маску для сети.
 *
 * Биты:  0-UP  1-DOWN  2-LEFT  3-RIGHT  4-SHOOT
 */
public class KeyInput extends KeyAdapter {

    public static final int UP    = 1;
    public static final int DOWN  = 2;
    public static final int LEFT  = 4;
    public static final int RIGHT = 8;
    public static final int SHOOT = 16;   // пробел

    private final Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    @Override public void keyPressed(KeyEvent e)  { pressed.add(e.getKeyCode()); }
    @Override public void keyReleased(KeyEvent e) { pressed.remove(e.getKeyCode()); }

    public boolean isDown(int keyCode) { return pressed.contains(keyCode); }

    /** Текущий ввод одним числом (отправляется по сети). */
    public int getMask() {
        int mask = 0;
        if (isDown(KeyEvent.VK_UP)    || isDown(KeyEvent.VK_W))     mask |= UP;
        if (isDown(KeyEvent.VK_DOWN)  || isDown(KeyEvent.VK_S))     mask |= DOWN;
        if (isDown(KeyEvent.VK_LEFT)  || isDown(KeyEvent.VK_A))     mask |= LEFT;
        if (isDown(KeyEvent.VK_RIGHT) || isDown(KeyEvent.VK_D))     mask |= RIGHT;
        if (isDown(KeyEvent.VK_SPACE))                               mask |= SHOOT;
        return mask;
    }
}
