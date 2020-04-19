package net.smackem.fxplayground.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class UdpListener implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(UdpListener.class);
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
                buffer.flip();
                final String message = StandardCharsets.UTF_8.decode(buffer)
                        .toString()
                        .toUpperCase();
                buffer.clear();
                log.debug("{} received {} from {}", this.channel.getLocalAddress(), message, remoteAddress);
                final ByteBuffer outBuffer = StandardCharsets.UTF_8.encode(message);
                this.channel.send(outBuffer, remoteAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
