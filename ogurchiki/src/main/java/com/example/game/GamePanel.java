package com.example.game;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

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

    // уведомление о новом оружии
    private String  weaponNotice     = "";
    private double  weaponNoticeTime = 0;
    private int     lastWeaponOrd    = 0;

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
        new Thread(this, "game-loop").start();
    }

    public synchronized void stop() {
        running = false;
        if (server != null) server.close();
        if (client != null) client.close();
    }

    private void leave() { stop(); SwingUtilities.invokeLater(onExit); }

    @Override
    public void run() {
        long   prev  = System.nanoTime();
        double accum = 0;
        long   fpsT  = prev;
        int    frames = 0;

        while (running) {
            long now = System.nanoTime();
            accum += (now - prev);
            prev = now;

            while (accum >= NANOS_PER_UPDATE) {
                update(1.0 / UPS);
                accum -= NANOS_PER_UPDATE;
            }
            if (input.isDown(KeyEvent.VK_ESCAPE)) { leave(); return; }
            repaint();
            frames++;
            if (now - fpsT >= 1_000_000_000L) { fps = frames; frames = 0; fpsT = now; }
            try { Thread.sleep(2); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    private void update(double dt) {
        if (server != null) {
            Player me = world.getPlayers().get(localId);
            if (me != null) me.setInputMask(input.getMask());
            world.updateHost(dt);
            server.broadcast();
            checkWeaponUpgrade();
        } else if (client != null) {
            client.sendInput(input.getMask());
            for (Player p : world.getPlayers().values()) p.update(dt);
            checkWeaponUpgrade();
        }
        if (weaponNoticeTime > 0) weaponNoticeTime -= dt;
    }

    private void checkWeaponUpgrade() {
        Player me = world.getPlayers().get(localId);
        if (me == null) return;
        if (me.weaponOrdinal > lastWeaponOrd) {
            lastWeaponOrd   = me.weaponOrdinal;
            weaponNotice    = "🔫 Новое оружие: " + me.getWeapon().name + "!";
            weaponNoticeTime = 3.0;
        }
    }

    // ── отрисовка ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);

        // дырки (рисуем до игроков, чтобы были под ними)
        for (ZombieHole h : world.getHoles()) h.render(g2);

        // зомби
        for (Zombie z : world.getZombies()) z.render(g2);

        // пули
        for (Bullet b : world.getBullets()) b.render(g2);

        // игроки
        for (Player p : world.getPlayers().values()) {
            p.render(g2);
            if (p.getId() == localId) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect((int) p.getX() - 3, (int) p.getY() - 3,
                        p.getWidth() + 5, p.getHeight() + 5, 10, 10);
                g2.setStroke(new BasicStroke(1));
            }
        }

        drawHud(g2);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(45, 45, 60));
        for (int x = 0; x < World.WIDTH;  x += 60) g.drawLine(x, 0, x, World.HEIGHT);
        for (int y = 0; y < World.HEIGHT; y += 60) g.drawLine(0, y, World.WIDTH, y);
    }

    private void drawHud(Graphics2D g) {
        int wave   = (client != null) ? client.wave : world.getWave();
        Player me  = world.getPlayers().get(localId);

        // верхняя полоска
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(200, 200, 200, 200));
        String role = server != null
                ? "ХОСТ  клиентов: " + server.getClientCount()
                : "КЛИЕНТ";
        g.drawString(role + "  |  FPS: " + fps
                + "  |  Волна: " + wave
                + "  |  WASD — движение   ПРОБЕЛ — огонь   Esc — меню",
                10, 20);

        // счётчик убийств + оружие (низ экрана)
        if (me != null) {
            WeaponType next = nextWeapon(me.getWeapon());
            String killStr = "Убийств: " + me.kills;
            if (next != null) killStr += "  /  " + next.killsNeeded + " → " + next.name;
            drawPill(g, killStr, World.WIDTH / 2, World.HEIGHT - 18);
        }

        // уведомление о новом оружии
        if (weaponNoticeTime > 0) {
            float alpha = (float) Math.min(1.0, weaponNoticeTime);
            g.setColor(new Color(1f, 0.9f, 0f, alpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            int tw = g.getFontMetrics().stringWidth(weaponNotice);
            g.drawString(weaponNotice, (World.WIDTH - tw) / 2, World.HEIGHT / 2 - 60);
        }

        // потеря связи
        if (client != null && !client.isConnected()) {
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("Соединение потеряно — нажмите Esc", 180, 300);
        }
    }

    private void drawPill(Graphics2D g, String text, int cx, int cy) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int tw = g.getFontMetrics().stringWidth(text);
        int pw = tw + 20, ph = 20;
        g.setColor(new Color(0, 0, 0, 160));
        g.fill(new RoundRectangle2D.Double(cx - pw / 2.0, cy - ph / 2.0, pw, ph, 10, 10));
        g.setColor(Color.WHITE);
        g.drawString(text, cx - tw / 2, cy + 5);
    }

    private WeaponType nextWeapon(WeaponType current) {
        WeaponType[] vals = WeaponType.values();
        int idx = current.ordinal() + 1;
        return idx < vals.length ? vals[idx] : null;
    }
}
