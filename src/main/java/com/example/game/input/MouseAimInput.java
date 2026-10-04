package com.example.game.input;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Отслеживает позицию мыши на панели (экранные координаты).
 * Перевод в мировые координаты делает Camera.screenToWorldX/Y().
 */
public class MouseAimInput extends MouseAdapter {

    private volatile int screenX = 0;
    private volatile int screenY = 0;
    private volatile boolean leftDown = false;

    @Override
    public void mouseMoved(MouseEvent e)   { screenX = e.getX(); screenY = e.getY(); }
    @Override
    public void mouseDragged(MouseEvent e) { screenX = e.getX(); screenY = e.getY(); }
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) leftDown = true;
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) leftDown = false;
    }

    public int     getScreenX()   { return screenX; }
    public int     getScreenY()   { return screenY; }
    public boolean isLeftDown()   { return leftDown; }
}
