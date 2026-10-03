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
 * Сервер (хост). Игровая логика считается на хосте.
 * Клиенты присылают только маску ввода (int), сервер рассылает позиции всех игроков.
 *
 * Протокол (DataStream, TCP):
 *   сервер -> клиент: int yourId                                    (один раз)
 *   сервер -> клиент: int count, затем count * (int id, float x, float y)  (каждый тик)
 *   клиент -> сервер: int inputMask                                 (при изменении)
 */
public class GameServer implements Closeable {

    private final int port;
    private final World world;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1); // 0 — это сам хост
    private volatile boolean running;
    private ServerSocket serverSocket;

    public GameServer(int port, World world) {
        this.port = port;
        this.world = world;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread accept = new Thread(this::acceptLoop, "server-accept");
        accept.setDaemon(true);
        accept.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientHandler handler = new ClientHandler(socket, nextId.getAndIncrement());
                clients.add(handler);
                handler.start();
            } catch (IOException e) {
                if (running) System.err.println("Ошибка accept: " + e.getMessage());
            }
        }
    }

    /** Разослать всем клиентам текущее состояние мира. Вызывается из игрового цикла. */
    public void broadcast() {
        List<Player> snapshot = new ArrayList<>(world.getPlayers().values());
        for (ClientHandler c : clients) {
            c.sendSnapshot(snapshot);
        }
    }

    public int getClientCount() {
        return clients.size();
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) { }
        for (ClientHandler c : clients) c.close();
    }

    /** Обслуживает одного подключённого клиента. */
    private class ClientHandler extends Thread {
        private final Socket socket;
        private final int id;
        private DataOutputStream out;
        private volatile boolean ready;

        ClientHandler(Socket socket, int id) {
            super("client-" + id);
            this.socket = socket;
            this.id = id;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
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
                // клиент отключился
            } finally {
                world.getPlayers().remove(id);
                clients.remove(this);
                close();
            }
        }

        synchronized void sendSnapshot(List<Player> players) {
            if (!ready) return;
            try {
                out.writeInt(players.size());
                for (Player p : players) {
                    out.writeInt(p.getId());
                    out.writeFloat((float) p.getX());
                    out.writeFloat((float) p.getY());
                }
                out.flush();
            } catch (IOException e) {
                close(); // читающий поток сам уберёт игрока
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }
}
