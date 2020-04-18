package net.smackem.fxplayground.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class LocalUdpClient implements AutoCloseable, Flow.Publisher<LocalUdpClient.Message> {

    private final Object monitor = new Object();
    private final Selector selector;
    private final Collection<DatagramChannel> channels = new ArrayList<>();
    private final ExecutorService executorService;
    private final ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
    private final SubmissionPublisher<LocalUdpClient.Message> publisher;

    public LocalUdpClient(int... ports) throws IOException {
        this.executorService = Executors.newSingleThreadExecutor();
        this.selector = Selector.open();
        for (final int port : ports) {
            final var channel = DatagramChannel.open()
                    .connect(new InetSocketAddress("localhost", port));
            channel.configureBlocking(false)
                    .register(this.selector, channel.validOps());
            this.channels.add(channel);
        }
        this.publisher = new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize());
        this.executorService.submit(this::run);
    }

    public static record Message(String remoteAddress, String text) {}

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
        for (final var iter = this.selector.selectedKeys().iterator(); iter.hasNext(); ) {
            final var key = iter.next();
            if (key.isReadable()) {
                final var channel = (DatagramChannel) key.channel();
                final var address = channel.receive(this.buffer);
                this.publisher.submit(new Message(address.toString(),
                        StandardCharsets.UTF_8.decode(this.buffer).toString()));
            }
            iter.remove();
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.publisher.close();
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
        this.executorService.shutdown();
        this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Message> subscriber) {
        this.publisher.subscribe(subscriber);
    }
}
