package net.smackem.fxplayground.server;

import net.smackem.fxplayground.events.EventPublisher;
import net.smackem.fxplayground.events.SimpleEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class LocalServer implements AutoCloseable {
    private final Logger log = LoggerFactory.getLogger(LocalServer.class);
    private final Object monitor = new Object();
    private final SimpleEventPublisher<Message.Base> messageReceived;
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel acceptChannel;
    private final ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
    private final Collection<RemoteClient> clients = new ArrayList<>();
    private final ExecutorService ioExecutorService;
    private volatile boolean closed;

    public LocalServer(int port) throws IOException {
        this.port = port;
        this.ioExecutorService = Executors.newSingleThreadExecutor();
        this.selector = Selector.open();
        this.acceptChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress(this.port));
        this.acceptChannel.configureBlocking(false)
                .register(this.selector, this.acceptChannel.validOps());
        this.messageReceived = new SimpleEventPublisher<>();
        this.ioExecutorService.submit(this::run);
    }

    public final EventPublisher<Message.Base> messageReceivedEvent() {
        return this.messageReceived;
    }

    private void run() {
        log.debug("enter RUN");
        try {
            next();
        } catch (IOException | ClosedSelectorException e) {
            log.info("I/O broke off", e);
            return;
        }
        if (this.closed) {
            log.info("I/O closed gracefully");
            return;
        }
        this.ioExecutorService.submit(this::run);
        log.debug("exit RUN");
    }

    private void next() throws IOException {
        if (this.selector.select() == 0) {
            // wakeup called or channel closed
            return;
        }
        final var iterator = this.selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            final var key = iterator.next();
            if (key.isAcceptable()) {
                assert key.channel() == this.acceptChannel;
                final SocketChannel clientChannel = this.acceptChannel.accept();
                log.info("accepted {}", clientChannel);
                addClient(clientChannel);
            } else if (key.isReadable()) {
                final RemoteClient client = (RemoteClient) key.attachment();
                assert client.channel() == key.channel();
                if (client.read(this.buffer)) {
                    log.debug("read {} bytes from {}", this.buffer.position(), client);
                    this.buffer.clear();
                } else {
                    log.info("I/O end from {}", client);
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
            log.error("error adding client", e);
            return;
        }
        synchronized (this.monitor) {
            this.clients.add(client);
        }
    }

    private void closeClient(RemoteClient client) {
        log.debug("close client {}", client);
        try {
            client.close();
        } catch (IOException e) {
            log.warn("error closing client", e);
        }
        synchronized (this.monitor) {
            this.clients.remove(client);
        }
        this.messageReceived.submit(new Message.ClientDisconnected(client.toString()));
    }

    void handleMessage(Message.Base message, RemoteClient origin) {
        this.messageReceived.submit(message);
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

    void writeMessage(Message.Base message, RemoteClient destination) {
        try {
            destination.write(message);
        } catch (IOException e) {
            closeClient(destination);
        }
    }

    @Override
    public void close() throws IOException {
        log.info("close server");
        this.closed = true;
        this.acceptChannel.close();
        synchronized (this.monitor) {
            for (final RemoteClient client : this.clients) {
                try {
                    client.close();
                } catch (IOException ignored) {
                    // nothing to do
                }
            }
            this.clients.clear();
        }
        this.selector.close();
        this.ioExecutorService.shutdown();
        try {
            this.ioExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "LocalServer{" +
               "port=" + port +
               ", client_count=" + clients.size() +
               '}';
    }
}
