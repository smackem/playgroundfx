package net.smackem.fxplayground.server;

import java.nio.ByteBuffer;

public interface Protocol<TMessage> {
    TMessage readByte(byte b);
    ByteBuffer encodeMessage(TMessage message);
}
