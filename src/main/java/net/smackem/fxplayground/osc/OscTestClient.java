package net.smackem.fxplayground.osc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class OscTestClient implements AutoCloseable {

    private final OSCPortOut outboundPort;

    private OscTestClient() throws IOException {
        this.outboundPort = new OSCPortOut(new InetSocketAddress(7770));
        this.outboundPort.connect();
    }

    void send(OSCPacket packet) throws IOException, OSCSerializeException {
        this.outboundPort.send(packet);
    }

    public static void main(String[] args) throws IOException {
        try (final OscTestClient client = new OscTestClient()) {
            client.send(new OSCMessage("/bowmore/figure/begin", List.of(0, 0)));
            client.send(new OSCMessage("/bowmore/figure/point", List.of(10, 20)));
            client.send(new OSCMessage("/bowmore/figure/point", List.of(100, 0)));
            client.send(new OSCMessage("/bowmore/figure/end", List.of(100, 100)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        this.outboundPort.disconnect();
        this.outboundPort.close();
    }
}
