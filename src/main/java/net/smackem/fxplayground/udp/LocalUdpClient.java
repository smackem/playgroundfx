package net.smackem.fxplayground.udp;

import net.smackem.fxplayground.events.EventPublisher;
import net.smackem.fxplayground.events.SimpleEventPublisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalUdpClient implements AutoCloseable {

    private final Selector selector;
    private final DatagramChannel channel;
    private final Collection<InetSocketAddress> remoteAddresses;
    private final ExecutorService executorService;
    private final ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
    private final SimpleEventPublisher<Message> messageReceived;

    public LocalUdpClient(int... ports) throws IOException {
        this.executorService = Executors.newSingleThreadExecutor();
        this.selector = Selector.open();
        this.channel = DatagramChannel.open();
        channel.configureBlocking(false)
                .register(this.selector, SelectionKey.OP_READ);
        this.remoteAddresses = IntStream.of(ports)
                .mapToObj(port -> new InetSocketAddress("localhost", port))
                .collect(Collectors.toList());
        this.messageReceived = new SimpleEventPublisher<>();
        this.executorService.submit(this::run);
    }

    public final EventPublisher<Message> messageReceivedEvent() {
        return this.messageReceived;
    }

    public static record Message(String remoteAddress, String text) {}

    public void sendToAll(String message) {
        final ByteBuffer buffer = StandardCharsets.UTF_8.encode(message);
        for (final var remoteAddress : this.remoteAddresses) {
            try {
                this.channel.send(buffer, remoteAddress);
                buffer.rewind();
            } catch (IOException ignored) { }
        }
    }

    private void run() {
        try {
            this.next();
        } catch (IOException e) {
            return;
        }
        this.executorService.submit(this::run);
    }

    private void next() throws IOException {
        if (this.selector.select() == 0) {
            return;
        }
        for (final var iter = this.selector.selectedKeys().iterator(); iter.hasNext(); iter.remove()) {
            final var key = iter.next();
            if (key.isReadable()) {
                final var channel = (DatagramChannel) key.channel();
                final var address = channel.receive(this.buffer);
                this.buffer.flip();
                this.messageReceived.submit(new Message(address.toString(),
                        StandardCharsets.UTF_8.decode(this.buffer).toString()));
                this.buffer.clear();
            }
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        try {
            this.channel.close();
        } catch (IOException ignored) { }
        this.selector.close();
        this.executorService.shutdown();
        this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
