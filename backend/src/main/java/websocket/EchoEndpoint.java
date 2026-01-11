package websocket;

import java.nio.ByteBuffer;
import java.time.Duration;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.Session.Listener;

public class EchoEndpoint extends Listener.Abstract {
    private volatile Session session;

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        session.setIdleTimeout(Duration.ZERO);
        System.out.println("Open: " + session.getRemoteSocketAddress());
        session.demand(); // required when you implement Listener directly
    }

    @Override
    public void onWebSocketText(String message) {
        System.out.println("Text: " + message);
        session.sendText("echo: " + message, Callback.NOOP);
        session.demand();
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            ByteBuffer copy = ByteBuffer.allocate(payload.remaining());
            copy.put(payload).flip();

            System.out.println("Binary: " + copy.remaining() + " bytes");
            session.sendBinary(copy, Callback.NOOP);

            callback.succeed();
        } catch (Throwable t) {
            callback.fail(t);
        } finally {
            session.demand();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("Close: " + statusCode + " " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace();
    }

    public void send(byte[] data) {
        Session s = this.session;
        if (s != null && s.isOpen()) {
            ByteBuffer bb = ByteBuffer.wrap(data).asReadOnlyBuffer();
            s.sendBinary(bb, Callback.NOOP);
        }
    }

    public void send(ByteBuffer bb) {
        Session s = this.session;
        if (s != null && s.isOpen()) {
            s.sendBinary(bb.asReadOnlyBuffer(), Callback.NOOP);
        }
    }
}
