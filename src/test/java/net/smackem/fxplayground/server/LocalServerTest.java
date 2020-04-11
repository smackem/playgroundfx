package net.smackem.fxplayground.server;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

public class LocalServerTest {

    private static final int PORT = 55555;

    @Test
    public void testLocalServer() throws IOException, InterruptedException {
        final Collection<Message.Base> receivedMessages = new ArrayList<>();
        final Collection<Message.Base> messagesToSend = generateMessages(100_000);
        final CountDownLatch latch = new CountDownLatch(messagesToSend.size());
        try (final LocalServer server = new LocalServer(PORT, ForkJoinPool.commonPool())) {
            server.subscribe(new UnboundedSubscriber<>(item -> {
                receivedMessages.add(item);
                if (receivedMessages.size() % 500 == 0) {
                    System.out.printf("%d messages received\n", receivedMessages.size());
                }
                latch.countDown();
            }));
            connectAndWrite(messagesToSend);
            latch.await(1, TimeUnit.SECONDS);
            Thread.sleep(100); // give client some time to disconnect
        }
        assertThat(receivedMessages).hasSize(messagesToSend.size());
        assertThat(receivedMessages).containsExactly(
                messagesToSend.toArray(Message.Base[]::new));
    }

    private Collection<Message.Base> generateMessages(int count) {
        final Collection<Message.Base> messages = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            messages.add(new Message.Chat(String.valueOf(i)));
        }
        return messages;
    }

    private void connectAndWrite(Collection<Message.Base> messages) throws IOException {
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        channel.connect(new InetSocketAddress("localhost", PORT));
        final Protocol protocol = new LineProtocol();
        int count = 0;
        for (final var message : messages) {
            final ByteBuffer bytes = protocol.encodeMessage(message);
            channel.write(bytes);
            count++;
            if (count % 500 == 0) {
                System.out.printf("%d messages sent\n", count);
            }
        }
        channel.close();
    }
}