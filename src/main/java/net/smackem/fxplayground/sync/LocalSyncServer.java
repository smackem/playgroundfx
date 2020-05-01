package net.smackem.fxplayground.sync;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class LocalSyncServer implements AutoCloseable {

    private final ServerSocketChannel listenChannel;
    private final Selector selector;

    public LocalSyncServer(int port) throws IOException {
        this.selector = Selector.open();
        this.listenChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress(port));
        this.listenChannel.configureBlocking(false)
                .register(this.selector, this.listenChannel.validOps());
    }

    @Override
    public void close() throws IOException {
        this.listenChannel.close();
        this.selector.close();
    }
}
