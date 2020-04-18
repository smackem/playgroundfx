package net.smackem.fxplayground.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executor;

public class UdpListener implements AutoCloseable {

    private final DatagramChannel channel;

    public UdpListener(int port, Executor executor) throws IOException {
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
