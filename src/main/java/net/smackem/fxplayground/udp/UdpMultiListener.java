package net.smackem.fxplayground.udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UdpMultiListener implements AutoCloseable {

    private final ExecutorService executorService;
    private final Collection<UdpListener> listeners = new ArrayList<>();

    public UdpMultiListener(int... ports) throws IOException {
        this.executorService = Executors.newCachedThreadPool();
        for (final int port : ports) {
            final UdpListener listener;
            try {
                listener = new UdpListener(port, this.executorService);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            listeners.add(listener);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final int firstPort = Integer.parseInt(args[0]);
        final int count = Integer.parseInt(args[1]);
        final int[] ports = new int[count];
        for (int i = 0; i < count; i++) {
            ports[i] = firstPort + i;
        }
        final var listener = new UdpMultiListener(ports);
        try (final var reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        }
        listener.close();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        for (final var listener : listeners) {
            listener.close();
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
