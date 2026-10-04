package com.example.game.network;

import com.example.game.ecs.component.HealthComponent;
import com.example.game.ecs.component.TransformComponent;
import com.example.game.ecs.component.WeaponComponent;
import com.example.game.ecs.world.World;
import com.example.game.entity.BulletEntity;
import com.example.game.entity.PlayerEntity;
import com.example.game.entity.ZombieEntity;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Протокол (DataStream, big-endian, TCP):
 *
 *   Рукопожатие (один раз сервер→клиент):  int yourId
 *
 *   Снимок (каждый тик, сервер→клиент):
 *     int playerCount
 *       per player: int id, float x,y, int hp, float aimDX,aimDY,
 *                   int kills, int weaponOrdinal
 *     int bulletCount
 *       per bullet: float x,y, int radius, int colorRGB
 *     int zombieCount
 *       per zombie: float x,y, int hp, int maxHp
 *     int wave
 *
 *   Ввод (клиент→сервер, при изменении):
 *     int inputMask, float mouseWorldX, float mouseWorldY
 */
public class GameServer implements Closeable {

    private final int    port;
    private final World  world;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId  = new AtomicInteger(1);
    private volatile boolean    running;
    private ServerSocket        serverSocket;

    public GameServer(int port, World world) { this.port = port; this.world = world; }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread t = new Thread(this::acceptLoop, "server-accept");
        t.setDaemon(true);
        t.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                ClientHandler h = new ClientHandler(s, nextId.getAndIncrement());
                clients.add(h);
                h.start();
            } catch (IOException e) {
                if (running) System.err.println("accept: " + e.getMessage());
            }
        }
    }

    public void broadcast() {
        List<PlayerEntity> ps = new ArrayList<>(world.getPlayers().values());
        List<BulletEntity>  bs = new ArrayList<>(world.getBullets());
        List<ZombieEntity>  zs = new ArrayList<>(world.getZombies());
        int wave = world.getWave();
        for (ClientHandler c : clients) c.sendSnapshot(ps, bs, zs, wave);
    }

    public int getClientCount() { return clients.size(); }

    @Override
    public void close() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (ClientHandler c : clients) c.close();
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private final int    id;
        private DataOutputStream out;
        private volatile boolean ready;

        ClientHandler(Socket s, int id) {
            super("client-" + id);
            socket = s; this.id = id; setDaemon(true);
        }

        @Override
        public void run() {
            try {
                DataInputStream  in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                PlayerEntity player = world.spawn(id);
                synchronized (this) { out.writeInt(id); out.flush(); ready = true; }
                while (running) {
                    int   mask = in.readInt();
                    float mx   = in.readFloat();
                    float my   = in.readFloat();
                    var inp = player.require(com.example.game.ecs.component.InputComponent.class);
                    inp.inputMask   = mask;
                    inp.mouseWorldX = mx;
                    inp.mouseWorldY = my;
                }
            } catch (IOException ignored) {
            } finally {
                world.getPlayers().remove(id);
                clients.remove(this);
                close();
            }
        }

        synchronized void sendSnapshot(List<PlayerEntity> players,
                                        List<BulletEntity> bullets,
                                        List<ZombieEntity> zombies, int wave) {
            if (!ready) return;
            try {
                out.writeInt(players.size());
                for (PlayerEntity p : players) {
                    TransformComponent tr = p.require(TransformComponent.class);
                    HealthComponent    hp = p.require(HealthComponent.class);
                    WeaponComponent    wp = p.require(WeaponComponent.class);
                    out.writeInt(p.getId());
                    out.writeFloat((float) tr.x);  out.writeFloat((float) tr.y);
                    out.writeInt(hp.hp);
                    out.writeFloat(wp.aimDX);       out.writeFloat(wp.aimDY);
                    out.writeInt(p.getKills());
                    out.writeInt(wp.weaponOrdinal);
                }
                out.writeInt(bullets.size());
                for (BulletEntity b : bullets) {
                    TransformComponent tr = b.require(TransformComponent.class);
                    var rc = b.require(com.example.game.ecs.component.RenderComponent.class);
                    out.writeFloat((float) tr.x);  out.writeFloat((float) tr.y);
                    out.writeInt(tr.width / 2);
                    out.writeInt(rc.color.getRGB());
                }
                out.writeInt(zombies.size());
                for (ZombieEntity z : zombies) {
                    TransformComponent tr = z.require(TransformComponent.class);
                    HealthComponent    hp = z.require(HealthComponent.class);
                    out.writeFloat((float) tr.x);  out.writeFloat((float) tr.y);
                    out.writeInt(hp.hp);            out.writeInt(hp.maxHp);
                }
                out.writeInt(wave);
                out.flush();
            } catch (IOException e) { close(); }
        }

        void close() { try { socket.close(); } catch (IOException ignored) {} }
    }
}
