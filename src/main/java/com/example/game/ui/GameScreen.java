package com.example.game.ui;

import com.example.game.core.GameContext;
import com.example.game.ecs.component.*;
import com.example.game.entity.*;
import com.example.game.graphics.Camera;
import com.example.game.input.KeyInput;
import com.example.game.input.MouseAimInput;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * Игровой экран.
 *
 * Хост:   обновляет мир через World.updateHost() → рассылает снимок.
 * Клиент: отправляет ввод (mask + mouseWorld) → рисует снимок от хоста.
 *
 * Управление: WASD — движение, мышь — прицел, ЛКМ / ПРОБЕЛ — огонь, ESC — меню.
 */
public class GameScreen implements Screen {

    private final GameContext  ctx;
    private final ScreenManager screens;
    private final Runnable     onExit;

    private final KeyInput      keys  = new KeyInput();
    private final MouseAimInput mouse = new MouseAimInput();
    private final Camera        camera;

    private int  fps        = 0;
    private int  frameCount = 0;
    private long fpsTimer   = System.nanoTime();

    private String weaponNotice     = "";
    private double weaponNoticeTime = 0;
    private int    lastWeaponOrd    = 0;

    public GameScreen(GameContext ctx, ScreenManager screens, Runnable onExit) {
        this.ctx     = ctx;
        this.screens = screens;
        this.onExit  = onExit;
        this.camera  = new Camera(
                com.example.game.core.Main.VIEW_W,
                com.example.game.core.Main.VIEW_H,
                ctx.world.getWidth(),
                ctx.world.getHeight());
    }

    @Override
    public void onEnable() {
        ScreenManager.Canvas c = screens.getCanvas();
        c.addKeyListener(keys);
        c.addMouseListener(mouse);
        c.addMouseMotionListener(mouse);
    }

    @Override
    public void onDisable() {
        ScreenManager.Canvas c = screens.getCanvas();
        c.removeKeyListener(keys);
        c.removeMouseListener(mouse);
        c.removeMouseMotionListener(mouse);
        ctx.shutdown();
    }

    @Override
    public Cursor preferredCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    // ── логика ───────────────────────────────────────────────────────────

    @Override
    public void update(double dt) {
        if (keys.isDown(KeyEvent.VK_ESCAPE)) { onExit.run(); return; }

        PlayerEntity me = ctx.world.getPlayers().get(ctx.localId);
        if (me != null) {
            InputComponent inp = me.require(InputComponent.class);

            // клавиши (ПРОБЕЛ или ЛКМ = SHOOT)
            int mask = keys.getMask();
            if (mouse.isLeftDown()) mask |= KeyInput.SHOOT;
            inp.inputMask = mask;

            // мышь → мировые координаты
            inp.mouseWorldX = camera.screenToWorldX(mouse.getScreenX());
            inp.mouseWorldY = camera.screenToWorldY(mouse.getScreenY());

            // обновить прицел сразу для рендера (даже на клиенте)
            TransformComponent tr = me.require(TransformComponent.class);
            me.require(WeaponComponent.class)
              .setAim(inp.mouseWorldX, inp.mouseWorldY, tr.getCenterX(), tr.getCenterY());

            // камера следит за игроком
            camera.follow(tr.getCenterX(), tr.getCenterY());
        }

        if (ctx.isHost()) {
            ctx.world.updateHost(dt);
            ctx.server.broadcast();
        } else if (ctx.client != null) {
            if (me != null) {
                InputComponent inp = me.require(InputComponent.class);
                ctx.client.sendInput(inp.inputMask, inp.mouseWorldX, inp.mouseWorldY);
            }
            // hitFlash анимация на клиенте
            for (PlayerEntity p : ctx.world.getPlayers().values()) {
                p.get(HealthComponent.class).ifPresent(h -> { if (h.hitFlash > 0) h.hitFlash -= dt; });
            }
        }

        checkWeaponUpgrade(me);
        if (weaponNoticeTime > 0) weaponNoticeTime -= dt;

        // FPS counter
        frameCount++;
        long now = System.nanoTime();
        if (now - fpsTimer >= 1_000_000_000L) {
            fps = frameCount; frameCount = 0; fpsTimer = now;
        }
    }

    // ── рендер ───────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // мировые объекты — через камеру
        AffineTransform saved = camera.begin(g);
        drawGrid(g);
        ctx.world.getHoles()  .forEach(hole   -> hole.render(g));
        ctx.world.getZombies().forEach(zombie  -> zombie.render(g));
        ctx.world.getBullets().forEach(bullet  -> bullet.render(g));
        ctx.world.getPlayers().values().forEach(player -> {
            player.render(g);
            // белая рамка вокруг своего персонажа
            if (player.getId() == ctx.localId) {
                TransformComponent tr = player.require(TransformComponent.class);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect((int)tr.x - 3, (int)tr.y - 3,
                                tr.width + 5, tr.height + 5, 10, 10);
                g.setStroke(new BasicStroke(1));
            }
        });
        camera.end(g, saved);

        // HUD — в экранных координатах (поверх камеры)
        drawHud(g, w, h);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(45, 45, 60));
        int ww = ctx.world.getWidth(), wh = ctx.world.getHeight();
        for (int x = 0; x < ww; x += 60) g.drawLine(x, 0, x, wh);
        for (int y = 0; y < wh; y += 60) g.drawLine(0, y, ww, y);
    }

    private void drawHud(Graphics2D g, int w, int h) {
        PlayerEntity me = ctx.world.getPlayers().get(ctx.localId);
        int wave = (ctx.client != null) ? ctx.client.wave : ctx.world.getWave();

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(200, 200, 200, 200));
        String role = ctx.isHost()
                ? "ХОСТ  клиентов: " + ctx.server.getClientCount()
                : "КЛИЕНТ";
        g.drawString(role + "  |  FPS: " + fps + "  |  Волна: " + wave
                + "  |  WASD+мышь — движение/прицел   ЛКМ — огонь   ESC — меню",
                10, 20);

        if (me != null) {
            WeaponComponent wp   = me.require(WeaponComponent.class);
            WeaponType      cur  = WeaponType.forOrdinal(wp.weaponOrdinal);
            WeaponType[]    vals = WeaponType.values();
            int             next = cur.ordinal() + 1;
            String killStr = "Убийств: " + me.getKills();
            if (next < vals.length)
                killStr += "  /  " + vals[next].killsNeeded + " → " + vals[next].name;
            drawPill(g, killStr, w / 2, h - 18);
        }

        if (weaponNoticeTime > 0) {
            float alpha = (float) Math.min(1.0, weaponNoticeTime);
            g.setColor(new Color(1f, 0.9f, 0f, alpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            String notice = "Новое оружие: " + weaponNotice + "!";
            int tw = g.getFontMetrics().stringWidth(notice);
            g.drawString(notice, (w - tw) / 2, h / 2 - 60);
        }

        if (ctx.client != null && !ctx.client.isConnected()) {
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String msg = "Соединение потеряно — нажмите ESC";
            int tw = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (w - tw) / 2, h / 2);
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

    private void checkWeaponUpgrade(PlayerEntity me) {
        if (me == null) return;
        int ord = me.require(WeaponComponent.class).weaponOrdinal;
        if (ord > lastWeaponOrd) {
            lastWeaponOrd    = ord;
            weaponNotice     = WeaponType.forOrdinal(ord).name;
            weaponNoticeTime = 3.0;
        }
    }
}
