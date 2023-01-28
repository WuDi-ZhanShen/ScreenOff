package com.tile.screenoff;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ScrOff extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        sendBroadcast(new Intent("action.ScrOff").putExtra("state", true));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 8090);
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    printWriter.println("off");
                    printWriter.close();
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}