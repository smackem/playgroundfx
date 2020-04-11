package net.smackem.fxplayground.server;

import java.nio.ByteBuffer;

public interface Protocol {
    Message.Base readByte(byte b);
    ByteBuffer encodeMessage(Message.Base message);
}
