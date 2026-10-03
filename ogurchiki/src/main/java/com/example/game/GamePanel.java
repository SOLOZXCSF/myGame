package com.example.game;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
        // --- КЛАВИАТУРА ---
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

        // --- МЫШЬ (Стрельба) ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
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

        // --- МЫШЬ (Прицеливание) ---
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

        // 1. Отрисовка стен
        for (Wall wall : world.getWalls()) {
            wall.draw(g2d);
        }

        // 2. Зомби
        for (Zombie z : world.getZombies()) {
            z.render(g2d);
        }

        // 3. Пули
        for (Bullet b : world.getBullets()) {
            b.render(g2d);
        }

        // 4. Игроки (каждый игрок сам рисует свои ХП под своими ногами)
        for (Player p : world.getPlayers().values()) {
            p.render(g2d);
        }

        // 5. Отрисовка HUD (Интерфейса сверху)
        drawHUD(g2d);

        // 6. Прицел мыши
        drawCrosshair(g2d, mouseX, mouseY);
    }

    private void drawHUD(Graphics2D g) {
        Player me = world.getPlayers().get(myPlayerId);

        // Плашка HUD на верху экрана
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 280, 75);
        g.setColor(new Color(100, 100, 100));
        g.drawRect(10, 10, 280, 75);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));

        if (me != null) {
            // Здоровье
            g.setColor(Color.WHITE);
            g.drawString("Здоровье: ", 20, 32);
            for (int i = 0; i < Player.MAX_HP; i++) {
                g.setColor(i < me.hp ? Color.RED : Color.DARK_GRAY);
                g.fillRect(100 + i * 22, 20, 18, 14);
                g.setColor(Color.BLACK);
                g.drawRect(100 + i * 22, 20, 18, 14);
            }

            // Убийства
            g.setColor(Color.YELLOW);
            g.drawString("Убийств: " + me.kills, 20, 52);

            // Оружие
            g.setColor(Color.CYAN);
            g.drawString("Оружие: " + me.getWeapon().name, 20, 72);
        } else {
            g.setColor(Color.RED);
            g.drawString("ВЫ ПОГИБЛИ", 20, 45);
        }

        // Информация о волне / клиентах
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