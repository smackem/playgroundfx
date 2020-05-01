package net.smackem.fxplayground.sync;

import net.smackem.fxplayground.server.Protocol;

import java.nio.ByteBuffer;

public class SyncProtocol implements Protocol<String> {
    private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);

    @Override
    public String readByte(byte b) {
        return null;
    }

    @Override
    public ByteBuffer encodeMessage(String s) {
        return EMPTY;
    }
}
