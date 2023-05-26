package com.tile.screenoff;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class SimpleTcpServer {

    public interface TcpConnectionListener {
        void onReceive(byte[] data);
        void onResponseSent();
    }
    private ServerSocket serverSocket;
    private static final int CAPACITY = 1024 * 1024;

    private final TcpConnectionListener listener;
    private BufferedInputStream in;
    private OutputStream out;

    public SimpleTcpServer(TcpConnectionListener listener, int port) {
        this.listener = listener;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (serverSocket == null) {
            return;
        }

        new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                in = new BufferedInputStream(socket.getInputStream());
                out = new BufferedOutputStream(socket.getOutputStream());
                startInputThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startInputThread() {
        new Thread(() -> {
            try {
                while(true) {
                    byte[] buf = new byte[CAPACITY];
                    if (in == null) {
                        break;
                    }
                    int size = in.read(buf);
                    if (size > 0) {
                        byte[] chunk = Arrays.copyOfRange(buf, 0, size);
                        listener.onReceive(chunk);
                    } else {
                        restart();
                        break;
                    }
                }
            } catch (IOException e) {
                restart();
            }
        }).start();
    }

    public void output(String data) {
        output(data.getBytes());
    }

    public void output(final byte[] data) {
        new Thread(() -> {
            if (out != null) {
                try {
                    out.write(data);
                    out.flush();
                    listener.onResponseSent();
                } catch (IOException e) {
                    restart();
                }
            }
        }).start();
    }

    public void stop() {
        try {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in = null;
            out = null;
        }
    }

    public void restart() {
        stop();
        start();
    }
}
