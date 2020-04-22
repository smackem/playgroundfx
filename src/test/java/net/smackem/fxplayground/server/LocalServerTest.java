package net.smackem.fxplayground.server;

import net.smackem.fxplayground.events.SimpleEventSubscriber;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

public class LocalServerTest {

    private static final int PORT = 55555;

    @Test
    public void testLocalServerSubscription() throws IOException, InterruptedException {
        final Collection<Message.Base> receivedMessages = new ArrayList<>();
        final Collection<Message.Base> messagesToSend = generateMessages(10_000);
        final CountDownLatch latch = new CountDownLatch(1);
        try (final LocalServer server = new LocalServer(PORT)) {
            server.messageReceivedEvent().subscribe(item -> {
                if (item instanceof Message.ClientDisconnected) {
                    latch.countDown();
                    return;
                }
                receivedMessages.add(item);
                if (receivedMessages.size() % 500 == 0) {
                    System.out.printf("%d messages received\n", receivedMessages.size());
                }
            });
            connectAndWrite(messagesToSend);
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(receivedMessages)
                .hasSize(messagesToSend.size())
                .containsExactly(messagesToSend.toArray(Message.Base[]::new));
    }

    @Test
    public void testLocalServerDispatchToClient() throws IOException, InterruptedException {
        final Collection<Message.Base> messagesToSend = generateMessages(10_000);
        Collection<Message.Base> receivedMessages;
        try (final LocalServer ignored = new LocalServer(PORT)) {
            final var future = connectAndRead(messagesToSend.size());
            connectAndWrite(messagesToSend);
            try {
                receivedMessages = future.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                receivedMessages = null;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        assertThat(receivedMessages)
                .isNotNull()
                .hasSize(messagesToSend.size())
                .containsExactly(messagesToSend.toArray(Message.Base[]::new));
    }

    // on a MacBook pro 2016, this takes ~4 sec with loglevel info
    @Test
    public void testLocalServerTiming() throws IOException, InterruptedException {
        for (int i = 0; i < 50; i++) {
            testLocalServerSubscription();
        }
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

    private CompletableFuture<Collection<Message.Base>> connectAndRead(int messageCount) throws IOException {
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        channel.connect(new InetSocketAddress("localhost", PORT));
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Collection<Message.Base> messages = new ArrayList<>();
                final Protocol protocol = new LineProtocol();
                final ByteBuffer buffer = ByteBuffer.allocate(1024);
                loop: while (true) {
                    final int byteCount = channel.read(buffer);
                    for (int i = 0; i < byteCount; i++) {
                        final Message.Base message = protocol.readByte(buffer.get(i));
                        if (message != null) {
                            messages.add(message);
                            if (messages.size() >= messageCount) {
                                break loop;
                            }
                        }
                    }
                    buffer.clear();
                }
                channel.close();
                return messages;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}