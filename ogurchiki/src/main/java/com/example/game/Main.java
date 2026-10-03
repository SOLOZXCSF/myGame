package com.example.game;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;

/** Точка входа. Показывает меню, затем запускает игру в режиме хоста или клиента. */
public class Main {

    private final JFrame frame = new JFrame("Swing Game");
    private GamePanel currentGame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().show());
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        showMenu();
        frame.setVisible(true);
    }

    private void showMenu() {
        currentGame = null;
        setContent(new MenuPanel(this::hostGame, this::joinGame, this::error));
    }

    private void hostGame(int port) {
        World world = new World();
        GameServer server = new GameServer(port, world);
        try {
            server.start();
        } catch (IOException e) {
            error("Не удалось создать хост на порту " + port + ":\n" + e.getMessage());
            return;
        }
        world.spawn(0); // игрок-хост всегда имеет id 0
        startGame(new GamePanel(world, server, null, 0, this::showMenu));
    }

    private void joinGame(String host, int port) {
        World world = new World();
        GameClient client = new GameClient(world);
        try {
            client.connect(host, port);
        } catch (IOException e) {
            client.close();
            error("Не удалось подключиться к " + host + ":" + port + "\n" + e.getMessage());
            return;
        }
        startGame(new GamePanel(world, null, client, client.getLocalId(), this::showMenu));
    }

    private void startGame(GamePanel game) {
        currentGame = game;
        setContent(game);
        game.requestFocusInWindow();
        game.start();
    }

    private void setContent(javax.swing.JComponent content) {
        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.revalidate();
        frame.repaint();
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
