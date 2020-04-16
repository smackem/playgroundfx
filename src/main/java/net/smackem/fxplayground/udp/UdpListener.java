package net.smackem.fxplayground.udp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UdpListener implements AutoCloseable {
    private final DatagramChannel channel;

    public static void main(String[] args) throws IOException, InterruptedException {
        final int firstPort = Integer.parseInt(args[0]);
        final int lastPort = firstPort + Integer.parseInt(args[1]);
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Collection<UdpListener> listeners = new ArrayList<>();
        for (int port = firstPort; port < lastPort; port++) {
            final UdpListener listener;
            try {
                listener = new UdpListener(port, executorService);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            listeners.add(listener);
        }
        try (final var reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        }
        for (final var listener : listeners) {
            listener.close();
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    private UdpListener(int port, Executor executor) throws IOException {
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .bind(new InetSocketAddress(port));
        this.channel.configureBlocking(true);
        executor.execute(this::run);
    }

    private void run() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                final SocketAddress remoteAddress = this.channel.receive(buffer);
                final String message = buffer.flip().asCharBuffer().toString();
                buffer.clear().asCharBuffer().put(message.toUpperCase());
                this.channel.send(buffer, remoteAddress);
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
