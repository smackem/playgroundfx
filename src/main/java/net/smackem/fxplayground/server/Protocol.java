package net.smackem.fxplayground.server;

import java.nio.ByteBuffer;

public interface Protocol {
    String readByte(byte b);
    ByteBuffer encodeMessage(String message);
}
