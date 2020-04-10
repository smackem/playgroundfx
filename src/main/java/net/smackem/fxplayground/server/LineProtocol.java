package net.smackem.fxplayground.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LineProtocol implements Protocol {
    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    @Override
    public String readByte(byte b) {
        if (b == '\n') {
            final byte[] bytes = this.bos.toByteArray();
            this.bos.reset();
            return new String(bytes, StandardCharsets.UTF_8);
        }
        bos.write(b);
        return null;
    }

    @Override
    public ByteBuffer encodeMessage(String message) {
        return ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
    }
}
