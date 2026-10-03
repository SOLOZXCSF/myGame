package com.example.game;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class GamePanel extends JPanel implements Runnable {

    private final World      world;
    private final GameServer server;
    private final GameClient client;
    private final int        myPlayerId;
    private final Runnable   onExit;

    private Thread  gameThread;
    private boolean running;

    private int inputMask = 0;
    private int mouseX    = 0;
    private int mouseY    = 0;

    // Смерть и Респавн
    private double respawnCooldown = 0;
    private static final double RESPAWN_DELAY = 5.0; // 5 секунд задержки респавна
    private final Rectangle respawnButtonBounds = new Rectangle(World.WIDTH / 2 - 100, World.HEIGHT / 2 + 50, 200, 50);

    public GamePanel(World world, GameServer server, GameClient client, int myPlayerId, Runnable onExit) {
        this.world      = world;
        this.server     = server;
        this.client     = client;
        this.myPlayerId = myPlayerId;
        this.onExit     = onExit;

        setPreferredSize(new Dimension(World.WIDTH, World.HEIGHT));
        setBackground(new Color(30, 30, 35));
        setFocusable(true);

        setupControls();
    }

    private void setupControls() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updateKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updateKey(e.getKeyCode(), false);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Player me = world.getPlayers().get(myPlayerId);

                    // Обработка клика по кнопке Респавна
                    if (me != null && me.hp <= 0 && respawnCooldown <= 0) {
                        if (respawnButtonBounds.contains(e.getPoint())) {
                            world.respawnPlayer(myPlayerId);
                            respawnCooldown = 0;
                            return;
                        }
                    }

                    inputMask |= KeyInput.SHOOT;
                    sendInputWithAim();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    inputMask &= ~KeyInput.SHOOT;
                    sendInputWithAim();
                }
            }
        });

        MouseMotionAdapter mouseAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateMousePos(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateMousePos(e.getX(), e.getY());
            }
        };
        addMouseMotionListener(mouseAdapter);
    }

    private void updateKey(int keyCode, boolean pressed) {
        int bit = 0;
        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP)    bit = KeyInput.UP;
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN)  bit = KeyInput.DOWN;
        if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT)  bit = KeyInput.LEFT;
        if (keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) bit = KeyInput.RIGHT;

        if (bit != 0) {
            if (pressed) inputMask |= bit;
            else         inputMask &= ~bit;
            sendInputWithAim();
        }
    }

    private void updateMousePos(int x, int y) {
        this.mouseX = x;
        this.mouseY = y;
        sendInputWithAim();
    }

    private void sendInputWithAim() {
        Player p = world.getPlayers().get(myPlayerId);
        float aimDX = 1f, aimDY = 0f;

        if (p != null) {
            double cx = p.getX() + p.getWidth() / 2.0;
            double cy = p.getY() + p.getHeight() / 2.0;
            double dirX = mouseX - cx;
            double dirY = mouseY - cy;
            double len = Math.hypot(dirX, dirY);

            if (len > 0.001) {
                aimDX = (float) (dirX / len);
                aimDY = (float) (dirY / len);
            }
            p.setAim(mouseX, mouseY);
        }

        if (client != null) {
            client.sendInput(inputMask, aimDX, aimDY);
        } else if (server != null) {
            server.setPlayerInput(myPlayerId, inputMask, aimDX, aimDY);
        }
    }

    public void start() {
        running = true;
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1e9;
            lastTime = now;

            // Обновление таймера респавна
            Player me = world.getPlayers().get(myPlayerId);
            if (me != null && me.hp <= 0) {
                if (respawnCooldown > 0) {
                    respawnCooldown -= dt;
                }
            } else {
                respawnCooldown = RESPAWN_DELAY; // Сброс таймера на случай смерти
            }

            if (server != null) {
                world.update(dt);
            } else {
                for (Player p : world.getPlayers().values()) {
                    p.update(dt);
                }
            }

            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Отрисовка луж крови под всеми объектами
        for (BloodPuddle bp : world.getBloodPuddles()) {
            bp.render(g2d);
        }

        // 2. Отрисовка стен
        for (Wall wall : world.getWalls()) {
            wall.draw(g2d);
        }

        // 3. Зомби
        for (Zombie z : world.getZombies()) {
            z.render(g2d);
        }

        // 4. Пули
        for (Bullet b : world.getBullets()) {
            b.render(g2d);
        }

        // 5. Игроки
        for (Player p : world.getPlayers().values()) {
            if (p.hp > 0) {
                p.render(g2d);
            }
        }

        // 6. HUD
        drawHUD(g2d);

        // 7. Экран смерти (если погиб)
        Player me = world.getPlayers().get(myPlayerId);
        if (me != null && me.hp <= 0) {
            drawDeathMenu(g2d);
        } else {
            drawCrosshair(g2d, mouseX, mouseY);
        }
    }

    private void drawHUD(Graphics2D g) {
        Player me = world.getPlayers().get(myPlayerId);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 280, 75);
        g.setColor(new Color(100, 100, 100));
        g.drawRect(10, 10, 280, 75);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));

        if (me != null) {
            g.setColor(Color.WHITE);
            g.drawString("Здоровье: ", 20, 32);
            for (int i = 0; i < Player.MAX_HP; i++) {
                g.setColor(i < me.hp ? Color.RED : Color.DARK_GRAY);
                g.fillRect(100 + i * 22, 20, 18, 14);
                g.setColor(Color.BLACK);
                g.drawRect(100 + i * 22, 20, 18, 14);
            }

            g.setColor(Color.YELLOW);
            g.drawString("Убийств: " + me.kills, 20, 52);

            g.setColor(Color.CYAN);
            g.drawString("Оружие: " + me.getWeapon().name, 20, 72);
        }

        int waveNum = (client != null) ? client.wave : 1;
        String infoStr = "Волна: " + waveNum;
        if (server != null) {
            infoStr += " | Игроков: " + (server.getClientCount() + 1);
        }

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(World.WIDTH - 180, 10, 170, 30);
        g.setColor(Color.WHITE);
        g.drawString(infoStr, World.WIDTH - 170, 30);
    }

    private void drawDeathMenu(Graphics2D g) {
        // Затемнение экрана
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, World.WIDTH, World.HEIGHT);

        // Текст "ВЫ ПОГИБЛИ"
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.setColor(Color.RED);
        String deathText = "ВЫ ПОГИБЛИ";
        int textWidth = g.getFontMetrics().stringWidth(deathText);
        g.drawString(deathText, (World.WIDTH - textWidth) / 2, World.HEIGHT / 2 - 40);

        // Кнопка возрождения или таймер
        if (respawnCooldown > 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.setColor(Color.LIGHT_GRAY);
            String cdText = String.format("Возрождение через: %.1f сек", respawnCooldown);
            int cdWidth = g.getFontMetrics().stringWidth(cdText);
            g.drawString(cdText, (World.WIDTH - cdWidth) / 2, World.HEIGHT / 2 + 20);
        } else {
            // Кнопка Респавна
            boolean hover = respawnButtonBounds.contains(mouseX, mouseY);
            g.setColor(hover ? new Color(180, 40, 40) : new Color(120, 20, 20));
            g.fillRect(respawnButtonBounds.x, respawnButtonBounds.y, respawnButtonBounds.width, respawnButtonBounds.height);

            g.setColor(Color.WHITE);
            g.drawRect(respawnButtonBounds.x, respawnButtonBounds.y, respawnButtonBounds.width, respawnButtonBounds.height);

            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String btnText = "ВОЗРОДИТЬСЯ";
            int btnTextWidth = g.getFontMetrics().stringWidth(btnText);
            g.drawString(btnText, respawnButtonBounds.x + (respawnButtonBounds.width - btnTextWidth) / 2, respawnButtonBounds.y + 32);
        }
    }

    private void drawCrosshair(Graphics2D g, int x, int y) {
        g.setColor(Color.RED);
        int size = 8;
        int gap = 3;

        g.drawLine(x - size, y, x - gap, y);
        g.drawLine(x + size, y, x + gap, y);
        g.drawLine(x, y - size, x, y - gap);
        g.drawLine(x, y + gap, x, y + size);

        g.fillRect(x - 1, y - 1, 2, 2);
    }
}