package com.example.game;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

/**
 * Игровая панель и игровой цикл (фиксированный шаг 60 UPS).
 * Режим хоста: считает физику всех игроков и рассылает состояние.
 * Режим клиента: только шлёт ввод и рисует то, что прислал хост.
 */
public class GamePanel extends JPanel implements Runnable {

    private static final double UPDATES_PER_SECOND = 60.0;
    private static final double NANOS_PER_UPDATE = 1_000_000_000.0 / UPDATES_PER_SECOND;

    private final World world;
    private final GameServer server;   // != null только у хоста
    private final GameClient client;   // != null только у клиента
    private final int localId;
    private final Runnable onExit;
    private final KeyInput input = new KeyInput();

    private volatile boolean running;
    private int fps;

    public GamePanel(World world, GameServer server, GameClient client, int localId, Runnable onExit) {
        this.world = world;
        this.server = server;
        this.client = client;
        this.localId = localId;
        this.onExit = onExit;

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

    /** Остановить игру и закрыть сеть (без возврата в меню). */
    public synchronized void stop() {
        running = false;
        if (server != null) server.close();
        if (client != null) client.close();
    }

    private void leave() {
        stop();
        SwingUtilities.invokeLater(onExit);
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        double accumulator = 0;
        long fpsTimer = previous;
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            accumulator += (now - previous);
            previous = now;

            while (accumulator >= NANOS_PER_UPDATE) {
                update(1.0 / UPDATES_PER_SECOND);
                accumulator -= NANOS_PER_UPDATE;
            }

            if (input.isDown(KeyEvent.VK_ESCAPE)) {
                leave();
                return;
            }

            repaint();
            frames++;

            if (now - fpsTimer >= 1_000_000_000L) {
                fps = frames;
                frames = 0;
                fpsTimer = now;
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void update(double dt) {
        int mask = input.getMask();

        if (server != null) {
            // Хост: считаем всех игроков и рассылаем результат
            Player me = world.getPlayers().get(localId);
            if (me != null) me.setInputMask(mask);
            for (Player p : world.getPlayers().values()) {
                p.update(dt);
            }
            server.broadcast();
        } else if (client != null) {
            // Клиент: только отправляем ввод
            client.sendInput(mask);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Player p : world.getPlayers().values()) {
            p.render(g2);
            if (p.getId() == localId) { // белая рамка вокруг своего игрока
                g2.setColor(Color.WHITE);
                g2.drawRect((int) p.getX() - 2, (int) p.getY() - 2, p.getWidth() + 3, p.getHeight() + 3);
            }
        }

        g2.setColor(Color.WHITE);
        String role = server != null ? "ХОСТ (подключено клиентов: " + server.getClientCount() + ")" : "КЛИЕНТ";
        g2.drawString(role + "  |  FPS: " + fps + "  |  WASD / стрелки — движение, Esc — меню", 10, 20);

        if (client != null && !client.isConnected()) {
            g2.setColor(Color.RED);
            g2.drawString("Соединение потеряно. Нажмите Esc, чтобы вернуться в меню.", 10, 40);
        }
    }
}
