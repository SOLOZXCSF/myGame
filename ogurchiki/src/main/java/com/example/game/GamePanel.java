package com.example.game;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

/**
 * Игровая панель и цикл (60 UPS, фиксированный шаг).
 *
 * Хост: считает физику (World.updateHost), рассылает снимок.
 * Клиент: шлёт ввод, рисует снимок.
 */
public class GamePanel extends JPanel implements Runnable {

    private static final double UPS              = 60.0;
    private static final double NANOS_PER_UPDATE = 1_000_000_000.0 / UPS;

    private final World      world;
    private final GameServer server;
    private final GameClient client;
    private final int        localId;
    private final Runnable   onExit;
    private final KeyInput   input = new KeyInput();

    private volatile boolean running;
    private int fps;

    public GamePanel(World world, GameServer server, GameClient client,
                     int localId, Runnable onExit) {
        this.world   = world;
        this.server  = server;
        this.client  = client;
        this.localId = localId;
        this.onExit  = onExit;

        setPreferredSize(new Dimension(World.WIDTH, World.HEIGHT));
        setBackground(new Color(30, 30, 40));
        setFocusable(true);
        addKeyListener(input);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this, "game-loop");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        running = false;
        if (server != null) server.close();
        if (client != null) client.close();
    }

    private void leave() {
        stop();
        SwingUtilities.invokeLater(onExit);
    }

    // ── игровой цикл ─────────────────────────────────────────────────────────

    @Override
    public void run() {
        long   prev       = System.nanoTime();
        double accumulator = 0;
        long   fpsTimer   = prev;
        int    frames     = 0;

        while (running) {
            long now = System.nanoTime();
            accumulator += (now - prev);
            prev = now;

            while (accumulator >= NANOS_PER_UPDATE) {
                update(1.0 / UPS);
                accumulator -= NANOS_PER_UPDATE;
            }

            if (input.isDown(KeyEvent.VK_ESCAPE)) { leave(); return; }

            repaint();
            frames++;
            if (now - fpsTimer >= 1_000_000_000L) {
                fps = frames; frames = 0; fpsTimer = now;
            }

            try { Thread.sleep(2); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    private void update(double dt) {
        if (server != null) {
            // ── ХОСТ ──
            Player me = world.getPlayers().get(localId);
            if (me != null) me.setInputMask(input.getMask());
            world.updateHost(dt);
            server.broadcast();

        } else if (client != null) {
            // ── КЛИЕНТ ── только отправить ввод; мир придёт от сервера
            client.sendInput(input.getMask());
            // анимация флеша на клиенте
            for (Player p : world.getPlayers().values()) p.update(dt);
            for (Bullet  b : world.getBullets())         b.update(0); // нет dt на клиенте
        }
    }

    // ── отрисовка ────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // сетка фона
        drawGrid(g2);

        // пули
        for (Bullet b : world.getBullets()) b.render(g2);

        // игроки
        for (Player p : world.getPlayers().values()) {
            p.render(g2);
            // белая рамка вокруг своего персонажа
            if (p.getId() == localId) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawRoundRect((int) p.getX() - 3, (int) p.getY() - 3,
                                 p.getWidth() + 5, p.getHeight() + 5, 10, 10);
                g2.setStroke(new java.awt.BasicStroke(1));
            }
        }

        // HUD
        drawHud(g2);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(45, 45, 60));
        for (int x = 0; x < World.WIDTH;  x += 60) g.drawLine(x, 0, x, World.HEIGHT);
        for (int y = 0; y < World.HEIGHT; y += 60) g.drawLine(0, y, World.WIDTH, y);
    }

    private void drawHud(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(200, 200, 200, 200));

        String role = server != null
                ? "ХОСТ  |  клиентов: " + server.getClientCount()
                : "КЛИЕНТ";
        g.drawString(role + "  |  FPS: " + fps
                + "  |  WASD — движение   ПРОБЕЛ — стрельба   Esc — меню",
                10, 20);

        if (client != null && !client.isConnected()) {
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("Соединение потеряно — нажмите Esc", 180, 300);
        }
    }
}
