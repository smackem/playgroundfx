package net.smackem.fxplayground.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocalUdpClient implements AutoCloseable {

    private final Object monitor = new Object();
    private final Selector selector;
    private final Collection<DatagramChannel> channels = new ArrayList<>();

    public LocalUdpClient(int... ports) throws IOException {
        this.selector = Selector.open();
        for (final int port : ports) {
            final var channel = DatagramChannel.open();
            channel.connect(new InetSocketAddress("localhost", port));
            this.channels.add(channel);
        }
    }

    @Override
    public void close() throws IOException {
        final Collection<DatagramChannel> channels;
        synchronized (this.monitor) {
            channels = List.copyOf(this.channels);
        }
        for (final var channel : channels) {
            try {
                channel.close();
            } catch (IOException ignored) { }
        }
        this.selector.close();
    }
}
