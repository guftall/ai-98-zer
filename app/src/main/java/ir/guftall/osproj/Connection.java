package ir.guftall.osproj;

import android.util.Log;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;

public class Connection {

    private static final String URL = "http://192.168.8.100:3000";

    private static Connection connection;
    private Socket sock;

    public static Connection getConnection() {
        if (connection == null) {
            connection = new Connection();
        }

        return connection;
    }

    private Connection() {
        try {
            IO.Options options = new IO.Options();
            options.timeout = -1;
            sock = IO.socket(URL, options);
        } catch (URISyntaxException e) {
            Log.e("Omid", "socker initialize error", e);
        }

        sock.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MainActivity.addText("Socket io connected");
            }
        }).on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MainActivity.addText("Reconnecting..");
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                EngineIOException exception = (EngineIOException) args[0];
                MainActivity.addText("error:" + exception.getMessage());
            }
        });
    }

    public void addListener(String event, Emitter.Listener listener) {
        sock.on(event, listener);
    }

    public void connect() {
        sock.connect();
    }

    public void sendAudio(ByteBuffer buffer) {

        byte[] arr = buffer.array();
        sock.emit("br", arr);
    }

    public void sendListen() {
        sock.emit("listen");
    }
}
