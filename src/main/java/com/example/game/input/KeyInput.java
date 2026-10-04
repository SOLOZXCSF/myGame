package com.example.game.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Биты маски:
 *   0  UP     1  DOWN     2  LEFT     3  RIGHT     4  SHOOT
 */
public class KeyInput extends KeyAdapter {

    public static final int UP       = 1;
    public static final int DOWN     = 2;
    public static final int LEFT     = 4;
    public static final int RIGHT    = 8;
    public static final int SHOOT    = 16;
    public static final int INTERACT = 32;   // F — взаимодействие с NPC

    private final Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    @Override public void keyPressed(KeyEvent e)  { pressed.add(e.getKeyCode()); }
    @Override public void keyReleased(KeyEvent e) { pressed.remove(e.getKeyCode()); }

    public boolean isDown(int keyCode) { return pressed.contains(keyCode); }

    public int getMask() {
        int m = 0;
        if (isDown(KeyEvent.VK_UP)    || isDown(KeyEvent.VK_W)) m |= UP;
        if (isDown(KeyEvent.VK_DOWN)  || isDown(KeyEvent.VK_S)) m |= DOWN;
        if (isDown(KeyEvent.VK_LEFT)  || isDown(KeyEvent.VK_A)) m |= LEFT;
        if (isDown(KeyEvent.VK_RIGHT) || isDown(KeyEvent.VK_D)) m |= RIGHT;
        if (isDown(KeyEvent.VK_SPACE) || isDown(KeyEvent.VK_ENTER)) m |= SHOOT;
        if (isDown(KeyEvent.VK_F)) m |= INTERACT;
        return m;
    }
}
