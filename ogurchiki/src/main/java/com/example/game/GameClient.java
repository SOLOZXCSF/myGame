package com.example.game;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/** Клиент: отправляет свой ввод хосту и применяет присылаемые позиции к {@link World}. */
public class GameClient implements Closeable {

    private final World world;
    private Socket socket;
    private DataOutputStream out;
    private int localId;
    private int lastSentMask = -1;
    private volatile boolean connected;

    public GameClient(World world) {
        this.world = world;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);
        socket.setTcpNoDelay(true);

        DataInputStream in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        localId = in.readInt(); // хост сообщает наш id
        connected = true;

        Thread reader = new Thread(() -> readLoop(in), "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(DataInputStream in) {
        try {
            while (connected) {
                int count = in.readInt();
                Set<Integer> seen = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    int id = in.readInt();
                    float x = in.readFloat();
                    float y = in.readFloat();
                    seen.add(id);
                    world.getPlayers()
                         .computeIfAbsent(id, k -> new Player(k, x, y, World.WIDTH, World.HEIGHT))
                         .setPosition(x, y);
                }
                world.getPlayers().keySet().retainAll(seen); // убрать отключившихся
            }
        } catch (IOException ignored) {
            // соединение закрыто
        } finally {
            connected = false;
        }
    }

    /** Отправляет ввод, только если он изменился (TCP надёжный, повторять не нужно). */
    public synchronized void sendInput(int mask) {
        if (!connected || mask == lastSentMask) return;
        try {
            out.writeInt(mask);
            out.flush();
            lastSentMask = mask;
        } catch (IOException e) {
            connected = false;
        }
    }

    public int getLocalId() { return localId; }

    public boolean isConnected() { return connected; }

    @Override
    public void close() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }
}
