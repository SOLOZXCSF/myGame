package com.example.game.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Управляет переключением экранов.
 *
 * Внутри — один JLayeredPane:
 *   слой DEFAULT (0)  — Canvas (рендер через Screen.render)
 *   слой PALETTE (10) — overlay JPanel с Swing-виджетами (кнопки меню)
 *
 * Переключение через switchTo() безопасно вызывать из любого потока.
 */
public class ScreenManager {

    private final JFrame       frame;
    private final JLayeredPane layered;
    private final Canvas       canvas;

    private volatile Screen active;
    private          JPanel  activeOverlay;

    public ScreenManager(JFrame frame, int width, int height) {
        this.frame = frame;

        canvas  = new Canvas(width, height);
        layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(width, height));

        canvas.setBounds(0, 0, width, height);
        layered.add(canvas,  JLayeredPane.DEFAULT_LAYER);

        frame.setContentPane(layered);
    }

    // ── публичное API ─────────────────────────────────────────────────────

    public void switchTo(Screen next) {
        Screen prev = active;
        active = next;

        SwingUtilities.invokeLater(() -> {
            // снять старый overlay
            if (activeOverlay != null) {
                layered.remove(activeOverlay);
                activeOverlay = null;
            }
            if (prev != null) prev.onDisable();

            // поставить новый overlay (если есть)
            if (next instanceof MenuScreen ms) {
                next.onEnable();                          // создаёт overlay
                JPanel ov = ms.getOverlay();
                if (ov != null) {
                    ov.setBounds(0, 0,
                            layered.getWidth(), layered.getHeight());
                    layered.add(ov, JLayeredPane.PALETTE_LAYER);
                    activeOverlay = ov;
                }
            } else {
                next.onEnable();
            }

            Cursor c = next.preferredCursor();
            frame.setCursor(c != null ? c : Cursor.getDefaultCursor());

            layered.revalidate();
            layered.repaint();
            canvas.requestFocusInWindow();
        });
    }

    /** Логический тик — вызывается игровым потоком. */
    public void tick(double dt) {
        Screen s = active;
        if (s != null) s.update(dt);
    }

    public void scheduleRepaint() { canvas.repaint(); }

    public Canvas getCanvas() { return canvas; }

    // ── внутренняя рисующая панель ────────────────────────────────────────

    public class Canvas extends JPanel {

        Canvas(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            setBackground(new Color(30, 30, 40));
            setFocusable(true);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Screen s = active;
            if (s != null) s.render((Graphics2D) g, getWidth(), getHeight());
        }
    }
}
