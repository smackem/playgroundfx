package net.smackem.fxplayground.server;

public final class Message {
    private Message() {
        throw new IllegalAccessError();
    }

    public interface Base {}

    public static record Chat(String text) implements Base {}
    public static record ClientConnected(String clientAddress) implements Base {}
    public static record ClientDisconnected(String clientAddress) implements Base {}
}
