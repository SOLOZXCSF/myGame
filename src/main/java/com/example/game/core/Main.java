package com.example.game.core;

import com.example.game.ecs.world.World;
import com.example.game.network.GameClient;
import com.example.game.network.GameServer;
import com.example.game.ui.GameScreen;
import com.example.game.ui.MenuScreen;
import com.example.game.ui.ScreenManager;

import javax.swing.*;
import java.io.IOException;

/** Точка входа. Создаёт окно, ScreenManager, GameLoop и показывает меню. */
public class Main {

    public static final int VIEW_W = 800;
    public static final int VIEW_H = 600;

    private final JFrame        frame   = new JFrame("Swing Game");
    private final ScreenManager screens;
    private final GameLoop      loop;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().launch());
    }

    public Main() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        screens = new ScreenManager(frame, VIEW_W, VIEW_H);
        loop    = new GameLoop(screens);
    }

    private void launch() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        screens.switchTo(new MenuScreen(this::hostGame, this::joinGame, this::error));
        loop.start();
    }

    // ── навигация ─────────────────────────────────────────────────────────

    private void hostGame(int port) {
        World      world = new World();
        GameServer srv   = new GameServer(port, world);
        try { srv.start(); }
        catch (IOException e) {
            error("Не удалось создать хост на порту " + port + ":\n" + e.getMessage());
            return;
        }
        world.spawn(0);
        GameContext ctx = new GameContext(world, srv, null, 0);
        screens.switchTo(new GameScreen(ctx, screens, this::showMenu));
    }

    private void joinGame(String host, int port) {
        World      world = new World();
        GameClient cli   = new GameClient(world);
        try { cli.connect(host, port); }
        catch (IOException e) {
            cli.close();
            error("Не удалось подключиться к " + host + ":" + port + "\n" + e.getMessage());
            return;
        }
        GameContext ctx = new GameContext(world, null, cli, cli.getLocalId());
        screens.switchTo(new GameScreen(ctx, screens, this::showMenu));
    }

    private void showMenu() {
        screens.switchTo(new MenuScreen(this::hostGame, this::joinGame, this::error));
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
