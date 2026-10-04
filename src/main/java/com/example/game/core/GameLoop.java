package com.example.game.core;

import com.example.game.ui.ScreenManager;

/**
 * Игровой цикл с фиксированным шагом логики (UPS) и свободной отрисовкой.
 *
 * Цикл работает в отдельном потоке ("game-loop").
 * ScreenManager.tick(dt)      → логика активного экрана
 * ScreenManager.scheduleRepaint() → запрос отрисовки
 */
public class GameLoop implements Runnable {

    private static final double UPS              = 60.0;
    private static final double NANOS_PER_UPDATE = 1_000_000_000.0 / UPS;

    private final ScreenManager screenManager;
    private volatile boolean    running;
    private Thread              thread;

    public GameLoop(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        long   prev   = System.nanoTime();
        double accum  = 0;
        long   fpsT   = prev;
        int    frames = 0;

        while (running) {
            long now = System.nanoTime();
            accum += (now - prev);
            prev   = now;

            while (accum >= NANOS_PER_UPDATE) {
                screenManager.tick(1.0 / UPS);
                accum -= NANOS_PER_UPDATE;
            }

            screenManager.scheduleRepaint();
            frames++;

            if (now - fpsT >= 1_000_000_000L) {
                // fps можно кинуть в GameContext если нужен HUD
                frames = 0;
                fpsT   = now;
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public boolean isRunning() { return running; }
}
