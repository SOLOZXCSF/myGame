package com.example.game.core;

import com.example.game.ecs.world.World;
import com.example.game.network.GameClient;
import com.example.game.network.GameServer;

/**
 * «Шина данных» текущей игровой сессии.
 * Передаётся в GameScreen при старте; при возврате в меню обнуляется.
 *
 * server == null  → мы клиент
 * client == null  → мы хост
 */
public class GameContext {

    public final World      world;
    public final GameServer server;   // nullable
    public final GameClient client;   // nullable
    public final int        localId;

    public GameContext(World world, GameServer server, GameClient client, int localId) {
        this.world   = world;
        this.server  = server;
        this.client  = client;
        this.localId = localId;
    }

    public boolean isHost() { return server != null; }

    public void shutdown() {
        if (server != null) server.close();
        if (client != null) client.close();
    }
}
