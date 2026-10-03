package com.example.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Игровой мир: размеры и список игроков (доступен из разных потоков). */
public class World {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private final Map<Integer, Player> players = new ConcurrentHashMap<>();

    public Map<Integer, Player> getPlayers() {
        return players;
    }

    /** Создаёт игрока в точке спавна, зависящей от id. */
    public Player spawn(int id) {
        double x = 100 + (id * 70) % (WIDTH - 200);
        double y = 100 + (id * 50) % (HEIGHT - 200);
        Player p = new Player(id, x, y, WIDTH, HEIGHT);
        players.put(id, p);
        return p;
    }
}
