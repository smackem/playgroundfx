package net.smackem.fxplayground.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class LocalServer implements Flow.Publisher<String>, AutoCloseable {
    private final Object monitor = new Object();
    private final SubmissionPublisher<String> publisher;
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel acceptChannel;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final Collection<RemoteClient> clients = new ArrayList<>();
    private final Executor executor;

    public LocalServer(int port, Executor executor) throws IOException {
        this.port = port;
        this.executor = Objects.requireNonNull(executor);
        this.selector = Selector.open();
        this.acceptChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress(this.port));
        this.acceptChannel.configureBlocking(false)
                .register(this.selector, this.acceptChannel.validOps());
        this.publisher = new SubmissionPublisher<>(executor, Flow.defaultBufferSize());
        executor.execute(this::run);
    }

    private void run() {
        try {
            selectNextOp();
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        this.executor.execute(this::run);
    }

    private void selectNextOp() throws IOException {
        if (this.selector.select() == 0) {
            return;
        }
        final var iterator = this.selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            final var key = iterator.next();
            if (key.isAcceptable()) {
                assert key.channel() == this.acceptChannel;
                final SocketChannel clientChannel = this.acceptChannel.accept();
                addClient(clientChannel);
            } else if (key.isReadable()) {
                final RemoteClient client = (RemoteClient) key.attachment();
                assert client.channel() == key.channel();
                if (client.read(this.buffer)) {
                    this.buffer.rewind();
                } else {
                    closeClient(client);
                }
            }
            iterator.remove();
        }
    }

    private void addClient(SocketChannel channel) {
        final RemoteClient client = new RemoteClient(channel, new LineProtocol(), this);
        try {
            channel.configureBlocking(false)
                    .register(this.selector, SelectionKey.OP_READ)
                    .attach(client);
        } catch (IOException e) {
            // TODO: logging
            e.printStackTrace();
            return;
        }
        synchronized(this.monitor) {
            this.clients.add(client);
        }
    }

    private void closeClient(RemoteClient client) {
        try {
            client.close();
        } catch (IOException e) {
            // TODO: logging
            e.printStackTrace();
        }
        synchronized (this.monitor) {
            this.clients.remove(client);
        }
    }

    void handleMessage(String message, RemoteClient origin) {
        this.publisher.submit(message);
        final Collection<RemoteClient> clients;
        synchronized (this.monitor) {
            clients = List.copyOf(this.clients);
        }
        for (final RemoteClient client : clients) {
            if (client != origin) {
                writeMessage(message, client);
            }
        }
    }

    void writeMessage(String message, RemoteClient destination) {
        try {
            destination.write(message);
        } catch (IOException e) {
            closeClient(destination);
        }
    }

    @Override
    public void close() throws IOException {
        this.selector.close();
        this.acceptChannel.close();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super String> subscriber) {
        this.publisher.subscribe(subscriber);
    }

    @Override
    public String toString() {
        return "LocalServer{" +
               "port=" + port +
               ", clients=" + clients +
               '}';
    }
}
