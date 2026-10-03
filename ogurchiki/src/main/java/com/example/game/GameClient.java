package com.example.game;

import java.awt.Color;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameClient implements Closeable {

    private final World world;
    private Socket           socket;
    private DataOutputStream out;
    private int              localId;
    private int              lastSentMask = -1;
    private volatile boolean connected;

    // волна (клиент только отображает)
    public volatile int wave = 1;

    public GameClient(World world) { this.world = world; }

    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);
        socket.setTcpNoDelay(true);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        localId = in.readInt();
        connected = true;
        Thread t = new Thread(() -> readLoop(in), "client-reader");
        t.setDaemon(true);
        t.start();
    }

    private void readLoop(DataInputStream in) {
        try {
            while (connected) {
                // ── игроки ──
                int pCount = in.readInt();
                Set<Integer> seenP = new HashSet<>();
                for (int i = 0; i < pCount; i++) {
                    int   id    = in.readInt();
                    float x     = in.readFloat();
                    float y     = in.readFloat();
                    int   hp    = in.readInt();
                    float aimDX = in.readFloat();
                    float aimDY = in.readFloat();
                    int   kills = in.readInt();
                    int   wOrd  = in.readInt();
                    seenP.add(id);
                    Player p = world.getPlayers()
                        .computeIfAbsent(id, k -> new Player(k, x, y, World.WIDTH, World.HEIGHT));
                    int oldHp = p.hp;
                    p.setPosition(x, y);
                    p.hp            = hp;
                    p.aimDX         = aimDX;
                    p.aimDY         = aimDY;
                    p.kills         = kills;
                    p.weaponOrdinal = wOrd;
                    if (hp < oldHp) p.markHit();
                }
                world.getPlayers().keySet().retainAll(seenP);

                // ── пули ──
                int bCount = in.readInt();
                List<Bullet> freshB = new ArrayList<>(bCount);
                for (int i = 0; i < bCount; i++) {
                    float bx     = in.readFloat();
                    float by     = in.readFloat();
                    int   radius = in.readInt();
                    Color color  = new Color(in.readInt(), true);
                    freshB.add(new Bullet(-1, bx + radius, by + radius, 0, 0, radius, color));
                }
                world.getBullets().clear();
                world.getBullets().addAll(freshB);

                // ── зомби ──
                int zCount = in.readInt();
                List<Zombie> freshZ = new ArrayList<>(zCount);
                for (int i = 0; i < zCount; i++) {
                    float zx    = in.readFloat();
                    float zy    = in.readFloat();
                    int   hp    = in.readInt();
                    int   maxHp = in.readInt();
                    Zombie z = new Zombie(i, zx + Zombie.SIZE / 2.0, zy + Zombie.SIZE / 2.0,
                                          maxHp, 0);
                    // вручную задаём hp (он может быть частично убит)
                    while (z.getHp() > hp) z.hit(1);
                    freshZ.add(z);
                }
                world.getZombies().clear();
                world.getZombies().addAll(freshZ);

                // ── волна ──
                wave = in.readInt();
            }
        } catch (IOException ignored) {
        } finally { connected = false; }
    }

    public synchronized void sendInput(int mask) {
        if (!connected || mask == lastSentMask) return;
        try {
            out.writeInt(mask);
            out.flush();
            lastSentMask = mask;
        } catch (IOException e) { connected = false; }
    }

    public int     getLocalId()   { return localId; }
    public boolean isConnected()  { return connected; }

    @Override
    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
