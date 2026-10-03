package com.example.game;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервер (хост). Игровая логика считается здесь.
 *
 * Протокол (DataStream, big-endian, TCP):
 *
 *   РУКОПОЖАТИЕ (один раз, сервер → клиент):
 *     int yourId
 *
 *   СНИМОК МИРА (каждый тик, сервер → клиент):
 *     int  playerCount
 *     per player: int id, float x, float y, int hp, float aimDX, float aimDY
 *     int  bulletCount
 *     per bullet: float x, float y
 *
 *   ВВОД (клиент → сервер, при изменении):
 *     int inputMask
 */
public class GameServer implements Closeable {

    private final int            port;
    private final World          world;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger  nextId  = new AtomicInteger(1);
    private volatile boolean     running;
    private ServerSocket         serverSocket;

    public GameServer(int port, World world) {
        this.port  = port;
        this.world = world;
    }

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
                if (running) System.err.println("accept error: " + e.getMessage());
            }
        }
    }

    /** Разослать снимок мира всем клиентам. */
    public void broadcast() {
        List<Player>  ps = new ArrayList<>(world.getPlayers().values());
        List<Bullet>  bs = new ArrayList<>(world.getBullets());
        for (ClientHandler c : clients) c.sendSnapshot(ps, bs);
    }

    public int getClientCount() { return clients.size(); }

    @Override
    public void close() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (ClientHandler c : clients) c.close();
    }

    // ── внутренний класс ─────────────────────────────────────────────────────

    private class ClientHandler extends Thread {
        private final Socket socket;
        private final int    id;
        private DataOutputStream out;
        private volatile boolean ready;

        ClientHandler(Socket socket, int id) {
            super("client-" + id);
            this.socket = socket;
            this.id     = id;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                DataInputStream  in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                Player player = world.spawn(id);
                synchronized (this) {
                    out.writeInt(id);
                    out.flush();
                    ready = true;
                }

                while (running) {
                    player.setInputMask(in.readInt());
                }
            } catch (IOException ignored) {
            } finally {
                world.getPlayers().remove(id);
                clients.remove(this);
                close();
            }
        }

        synchronized void sendSnapshot(List<Player> players, List<Bullet> bullets) {
            if (!ready) return;
            try {
                out.writeInt(players.size());
                for (Player p : players) {
                    out.writeInt(p.getId());
                    out.writeFloat((float) p.getX());
                    out.writeFloat((float) p.getY());
                    out.writeInt(p.hp);
                    out.writeFloat(p.aimDX);
                    out.writeFloat(p.aimDY);
                }
                out.writeInt(bullets.size());
                for (Bullet b : bullets) {
                    out.writeFloat((float) b.getX());
                    out.writeFloat((float) b.getY());
                }
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
