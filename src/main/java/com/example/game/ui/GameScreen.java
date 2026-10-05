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

public class GameScreen implements Screen {

    private final GameContext   ctx;
    private final ScreenManager screens;
    private final Runnable      onExit;
    private final KeyInput      keys  = new KeyInput();
    private final MouseAimInput mouse = new MouseAimInput();
    private final Camera        camera;

    private int  fps = 0, frameCount = 0;
    private long fpsTimer = System.nanoTime();

    private String notice     = "";
    private double noticeTime = 0;
    private boolean shopMenuOpen = false;

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

    @Override public void onEnable() {
        ScreenManager.Canvas c = screens.getCanvas();
        c.addKeyListener(keys);
        c.addMouseListener(mouse);
        c.addMouseMotionListener(mouse);
    }

    @Override public void onDisable() {
        ScreenManager.Canvas c = screens.getCanvas();
        c.removeKeyListener(keys);
        c.removeMouseListener(mouse);
        c.removeMouseMotionListener(mouse);
        ctx.shutdown();
    }

    @Override public Cursor preferredCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    // ── логика ───────────────────────────────────────────────────────────

    @Override
    public void update(double dt) {
        if (keys.isDown(KeyEvent.VK_ESCAPE)) { onExit.run(); return; }

        PlayerEntity me = ctx.world.getPlayers().get(ctx.localId);

        if (me != null) {
            InventoryComponent inv = me.require(InventoryComponent.class);
            for (int i = 0; i < InventoryComponent.SLOTS; i++) {
                if (keys.isDown(KeyEvent.VK_1 + i)) {
                    inv.setActive(i);
                    syncWeaponSlot(me, inv, i);
                }
            }
            if (inv.shopMenuOpen) handleShopBuy(me, inv);
            shopMenuOpen = inv.shopMenuOpen;

            InputComponent inp = me.require(InputComponent.class);
            int mask = keys.getMask();
            if (mouse.isLeftDown()) mask |= KeyInput.SHOOT;
            inp.inputMask   = mask;
            inp.mouseWorldX = camera.screenToWorldX(mouse.getScreenX());
            inp.mouseWorldY = camera.screenToWorldY(mouse.getScreenY());

            TransformComponent tr = me.require(TransformComponent.class);
            me.require(WeaponComponent.class)
              .setAim(inp.mouseWorldX, inp.mouseWorldY, tr.getCenterX(), tr.getCenterY());
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
            for (PlayerEntity p : ctx.world.getPlayers().values())
                p.get(HealthComponent.class)
                 .ifPresent(h -> { if (h.hitFlash > 0) h.hitFlash -= dt; });
        }

        if (noticeTime > 0) noticeTime -= dt;

        frameCount++;
        long now = System.nanoTime();
        if (now - fpsTimer >= 1_000_000_000L) {
            fps = frameCount; frameCount = 0; fpsTimer = now;
        }
    }

    private void syncWeaponSlot(PlayerEntity p, InventoryComponent inv, int slot) {
        ItemType item = inv.get(slot);
        WeaponComponent wp = p.require(WeaponComponent.class);
        if (slot == 0) {
            wp.pickaxeMode = true;
        } else if (item != null && item.isWeapon()) {
            wp.pickaxeMode   = false;
            wp.weaponOrdinal = item.toWeaponType().ordinal();
        } else {
            wp.pickaxeMode = false;
        }
    }

    private void handleShopBuy(PlayerEntity me, InventoryComponent inv) {
        if (keys.isDown(KeyEvent.VK_Z)) tryBuy(me, inv, ItemType.WPN_SHOTGUN, ShopNpcEntity.PRICE_SHOTGUN);
        if (keys.isDown(KeyEvent.VK_X)) tryBuy(me, inv, ItemType.WPN_RIFLE,   ShopNpcEntity.PRICE_RIFLE);
        if (keys.isDown(KeyEvent.VK_V)) tryBuy(me, inv, ItemType.WPN_ROCKET,  ShopNpcEntity.PRICE_ROCKET);
    }

    private void tryBuy(PlayerEntity me, InventoryComponent inv, ItemType item, int price) {
        if (inv.buyWeapon(item, price)) showNotice("Куплено: " + item.name);
        else showNotice("Недостаточно золота! (" + price + "g)");
    }

    private void showNotice(String text) { notice = text; noticeTime = 2.5; }

    // ── рендер ───────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        AffineTransform saved = camera.begin(g);

        // 1. Карта (самый нижний слой)
        if (ctx.world.getGameMap() != null)
            ctx.world.getGameMap().render(g,
                    camera.getOffsetX(), camera.getOffsetY(), w, h);

        // 2. Дырки зомби поверх карты
        ctx.world.getHoles().forEach(e -> e.render(g));

        // 3. Камни / ресурсы
        ctx.world.getResourceNodes().forEach(e -> e.render(g));

        // 4. Лавка
        ShopNpcEntity shop = ctx.world.getShop();
        if (shop != null) shop.render(g);

        // 5. Зомби
        ctx.world.getZombies().forEach(e -> e.render(g));

        // 6. Пули
        ctx.world.getBullets().forEach(e -> e.render(g));

        // 7. Игроки
        ctx.world.getPlayers().values().forEach(p -> {
            p.render(g);
            if (p.getId() == ctx.localId) {
                TransformComponent tr = p.require(TransformComponent.class);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect((int)tr.x - 3, (int)tr.y - 3, tr.width+5, tr.height+5, 10, 10);
                g.setStroke(new BasicStroke(1));
            }
        });

        camera.end(g, saved);

        // 8. HUD (экранные координаты)
        drawHud(g, w, h);
    }

    private void drawHud(Graphics2D g, int w, int h) {
        PlayerEntity me = ctx.world.getPlayers().get(ctx.localId);
        int wave = (ctx.client != null) ? ctx.client.wave : ctx.world.getWave();

        // Статус-бар сверху
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, w, 26);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(220, 220, 220));
        String role = ctx.isHost() ? "ХОСТ  клиентов: " + ctx.server.getClientCount() : "КЛИЕНТ";
        g.drawString(role + "  |  FPS: " + fps + "  |  Волна: " + wave
                + "  |  WASD — движение   мышь — прицел   ЛКМ/Пробел — атака   F — лавка",
                10, 17);

        if (me != null) {
            InventoryComponent inv = me.require(InventoryComponent.class);
            int stones = inv.countOf(ItemType.STONE);
            drawPill(g, "⛏ " + stones + "  💰 " + inv.gold + "g  ☠ " + me.getKills(),
                     w / 2, h - 75);
            drawInventory(g, w, h, inv);
            if (shopMenuOpen) drawShopMenu(g, w, h, inv);
        }

        if (noticeTime > 0) {
            float alpha = (float) Math.min(1.0, noticeTime);
            g.setColor(new Color(1f, 0.95f, 0.2f, alpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            int tw = g.getFontMetrics().stringWidth(notice);
            g.drawString(notice, (w - tw) / 2, h / 2 - 80);
        }

        if (ctx.client != null && !ctx.client.isConnected()) {
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String msg = "Соединение потеряно — нажмите ESC";
            int tw = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (w - tw) / 2, h / 2);
        }
    }

    // ── инвентарь ────────────────────────────────────────────────────────

    private static final int SLOT_SIZE   = 52;
    private static final int SLOT_GAP    = 6;
    private static final int SLOT_RADIUS = 8;

    private void drawInventory(Graphics2D g, int w, int h, InventoryComponent inv) {
        int total  = InventoryComponent.SLOTS;
        int barW   = total * SLOT_SIZE + (total - 1) * SLOT_GAP;
        int startX = (w - barW) / 2;
        int baseY  = h - SLOT_SIZE - 12;

        for (int i = 0; i < total; i++) {
            boolean active = (i == inv.activeSlot);
            int sx = startX + i * (SLOT_SIZE + SLOT_GAP);
            int sy = baseY  - (active ? 6 : 0);

            g.setColor(active ? new Color(40,40,60,220) : new Color(20,20,35,180));
            g.fill(new RoundRectangle2D.Double(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, SLOT_RADIUS));

            g.setColor(active ? new Color(200,200,255,230) : new Color(80,80,110,180));
            g.setStroke(new BasicStroke(active ? 2 : 1));
            g.draw(new RoundRectangle2D.Double(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, SLOT_RADIUS));
            g.setStroke(new BasicStroke(1));

            ItemType item = inv.get(i);
            if (item != null) {
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                g.setColor(active ? item.color.brighter() : item.color);
                String sym = item.symbol;
                int symW = g.getFontMetrics().stringWidth(sym);
                g.drawString(sym, sx + (SLOT_SIZE - symW) / 2, sy + SLOT_SIZE / 2 + 5);

                int cnt = inv.count(i);
                if (item.stackable && cnt > 0) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g.setColor(Color.WHITE);
                    String cs = String.valueOf(cnt);
                    g.drawString(cs, sx + SLOT_SIZE - g.getFontMetrics().stringWidth(cs) - 3, sy + SLOT_SIZE - 4);
                }

                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.setColor(new Color(200,200,200, active ? 220 : 150));
                int nw = g.getFontMetrics().stringWidth(item.name);
                g.drawString(item.name, sx + (SLOT_SIZE - nw) / 2, sy + SLOT_SIZE - 5);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.setColor(new Color(120,120,160, active ? 220 : 130));
            g.drawString(String.valueOf(i + 1), sx + 4, sy + 12);
        }
    }

    // ── меню магазина ─────────────────────────────────────────────────────

    private void drawShopMenu(Graphics2D g, int w, int h, InventoryComponent inv) {
        int mw = 320, mh = 220;
        int mx = (w - mw) / 2, my = h / 2 - mh - 20;

        g.setColor(new Color(15, 12, 8, 230));
        g.fill(new RoundRectangle2D.Double(mx, my, mw, mh, 12, 12));
        g.setColor(new Color(255, 220, 50, 180));
        g.setStroke(new BasicStroke(2));
        g.draw(new RoundRectangle2D.Double(mx, my, mw, mh, 12, 12));
        g.setStroke(new BasicStroke(1));

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(255, 220, 50));
        g.drawString("⚒  ЛАВКА СКУПЩИКА", mx + 12, my + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 180, 140));
        g.drawString("Золото: " + inv.gold, mx + 12, my + 42);

        Object[][] items = {
            {"Z", ItemType.WPN_SHOTGUN, ShopNpcEntity.PRICE_SHOTGUN},
            {"X", ItemType.WPN_RIFLE,   ShopNpcEntity.PRICE_RIFLE},
            {"V", ItemType.WPN_ROCKET,  ShopNpcEntity.PRICE_ROCKET},
        };
        for (int i = 0; i < items.length; i++) {
            String   key   = (String)   items[i][0];
            ItemType item  = (ItemType) items[i][1];
            int      price = (int)      items[i][2];
            boolean  can   = inv.gold >= price;
            int ry = my + 62 + i * 46;

            g.setColor(can ? new Color(40,50,30,180) : new Color(40,30,30,150));
            g.fill(new RoundRectangle2D.Double(mx+8, ry, mw-16, 38, 6, 6));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(100,120,80));
            g.drawString("[" + key + "]", mx+16, ry+24);
            g.setColor(can ? item.color : item.color.darker());
            g.drawString(item.symbol + " " + item.name, mx+50, ry+24);
            g.setColor(can ? new Color(255,220,50) : new Color(150,120,50));
            g.drawString(price + "g", mx+mw-40, ry+24);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(140,130,110));
        g.drawString("Отойди от лавки чтобы закрыть", mx+12, my+mh-8);
    }

    private void drawPill(Graphics2D g, String text, int cx, int cy) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int tw = g.getFontMetrics().stringWidth(text);
        int pw = tw + 20, ph = 20;
        g.setColor(new Color(0,0,0,160));
        g.fill(new RoundRectangle2D.Double(cx - pw/2.0, cy - ph/2.0, pw, ph, 10, 10));
        g.setColor(Color.WHITE);
        g.drawString(text, cx - tw/2, cy+5);
    }
}
