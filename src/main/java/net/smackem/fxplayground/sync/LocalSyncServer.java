package net.smackem.fxplayground.sync;

import net.smackem.fxplayground.server.LocalServer;

import java.io.IOException;

public class LocalSyncServer implements AutoCloseable {

    private static final int PORT = 6666;
    private final LocalServer<String> tcpServer;

    public LocalSyncServer() throws IOException {
        this.tcpServer = new LocalServer<>(PORT, SyncProtocol::new, true);
        this.tcpServer.messageReceivedEvent().subscribe(this::onMessageReceived);
    }

    private void onMessageReceived(String s) {
    }

    @Override
    public void close() throws IOException {
        this.tcpServer.close();
    }
}
