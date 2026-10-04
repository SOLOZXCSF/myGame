package com.example.game.network;

import com.example.game.ecs.component.*;
import com.example.game.ecs.world.World;
import com.example.game.entity.*;

import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Подключается к хосту, отправляет ввод (mask + mouseWorldX/Y),
 * получает снимок мира и применяет его к локальному World.
 */
public class GameClient implements Closeable {

    private final World          world;
    private Socket               socket;
    private DataOutputStream     out;
    private int                  localId;
    private int                  lastMask = -1;
    private float                lastMX, lastMY;
    private volatile boolean     connected;
    public  volatile int         wave = 1;

    public GameClient(World world) { this.world = world; }

    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);
        socket.setTcpNoDelay(true);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        localId   = in.readInt();
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
                    int   id  = in.readInt();
                    float x   = in.readFloat(), y   = in.readFloat();
                    int   hp  = in.readInt();
                    float adx = in.readFloat(), ady = in.readFloat();
                    int   k   = in.readInt(),   wo  = in.readInt();
                    seenP.add(id);

                    PlayerEntity p = world.getPlayers()
                        .computeIfAbsent(id, kid -> new PlayerEntity(kid, x, y));
                    TransformComponent tr = p.require(TransformComponent.class);
                    HealthComponent    ph = p.require(HealthComponent.class);
                    WeaponComponent    wp = p.require(WeaponComponent.class);

                    int oldHp = ph.hp;
                    tr.set(x, y);
                    ph.hp            = hp;
                    wp.aimDX         = adx;
                    wp.aimDY         = ady;
                    wp.weaponOrdinal = wo;
                    if (hp < oldHp) ph.hitFlash = 0.15;
                }
                world.getPlayers().keySet().retainAll(seenP);

                // ── пули (только визуал) ──
                int bCount = in.readInt();
                List<BulletEntity> freshB = new ArrayList<>(bCount);
                for (int i = 0; i < bCount; i++) {
                    float bx = in.readFloat(), by = in.readFloat();
                    int   r  = in.readInt();
                    Color c  = new Color(in.readInt(), true);
                    freshB.add(new BulletEntity(bx, by, r, c));
                }
                world.getBullets().clear();
                world.getBullets().addAll(freshB);

                // ── зомби ──
                int zCount = in.readInt();
                List<ZombieEntity> freshZ = new ArrayList<>(zCount);
                for (int i = 0; i < zCount; i++) {
                    float zx = in.readFloat(), zy = in.readFloat();
                    int   zh = in.readInt(),   zm = in.readInt();
                    ZombieEntity z = new ZombieEntity(
                            i, zx + ZombieEntity.SIZE / 2.0,
                               zy + ZombieEntity.SIZE / 2.0, zm, 0);
                    z.require(HealthComponent.class).hp = zh;
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

    /** Отправить ввод если что-то изменилось. */
    public synchronized void sendInput(int mask, double mx, double my) {
        if (!connected) return;
        boolean changed = mask != lastMask
                || (float) mx != lastMX
                || (float) my != lastMY;
        if (!changed) return;
        try {
            out.writeInt(mask);
            out.writeFloat((float) mx);
            out.writeFloat((float) my);
            out.flush();
            lastMask = mask;
            lastMX   = (float) mx;
            lastMY   = (float) my;
        } catch (IOException e) { connected = false; }
    }

    public int     getLocalId()  { return localId; }
    public boolean isConnected() { return connected; }

    @Override
    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
