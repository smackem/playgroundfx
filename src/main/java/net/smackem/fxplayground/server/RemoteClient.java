package net.smackem.fxplayground.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

public final class RemoteClient implements AutoCloseable {
    private final SocketChannel channel;
    private final Protocol protocol;
    private final LocalServer server;

    RemoteClient(SocketChannel channel, Protocol protocol, LocalServer server) {
        this.channel = channel;
        this.protocol = protocol;
        this.server = server;
    }

    Channel channel() {
        return this.channel;
    }

    boolean read(ByteBuffer buffer) {
        final int count;
        try {
            count = this.channel.read(buffer);
            if (count < 0) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            final String message = this.protocol.readByte(buffer.get(i));
            if (message != null) {
                this.server.handleMessage(message, this);
            }
        }
        return true;
    }

    void write(String message) throws IOException {
        final ByteBuffer buffer = this.protocol.encodeMessage(message);
        this.channel.write(buffer);
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }

    @Override
    public String toString() {
        return "RemoteClient{" +
               "channel=" + channel +
               '}';
    }
}
