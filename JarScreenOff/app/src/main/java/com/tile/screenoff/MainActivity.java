package com.tile.screenoff;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    Button button;
    Switch aSwitch;
    boolean isServiceOK = false;


    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ch(button);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        getWindow().getAttributes().width = (int) (getResources().getDisplayMetrics().density * 325);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Shizuku.addRequestPermissionResultListener(RL);


        String file1 = getExternalFilesDir(null).getPath() + "/starter.sh";
        try {
            InputStream is = getAssets().open("starter.sh");
            FileOutputStream fileOutputStream = new FileOutputStream(file1);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = is.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            is.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ignored) {
        }
        String file2 = getExternalFilesDir(null).getPath() + "/Socket.dex";
        try {
            InputStream is = getAssets().open("Socket.dex");
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = is.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            is.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ignored) {
        }
        button = findViewById(R.id.b);
        aSwitch = findViewById(R.id.s1);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket = new Socket("127.0.0.1", 8090);
                            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                            printWriter.println(b ? "on" : "off");
                            printWriter.close();
                            socket.close();
                        } catch (IOException ignored) {
                            ch(button);
                        }
                    }
                }).start();

            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                isServiceOK = checkService();
                button.setText(isServiceOK ? "息屏运行服务一切正常" : "息屏运行服务没有启动\n点我启动");
                aSwitch.setEnabled(isServiceOK);
            }
        }).start();
        registerReceiver(myReceiver, new IntentFilter("action.Activate.ScreenOff"));
        super.onCreate(savedInstanceState);
    }

    boolean checkService() {
        try {
            Socket socket = new Socket("127.0.0.1", 8090);
            socket.close();
        } catch (IOException e) {
            return false;
        }
        return true;

    }

    public void ch(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                isServiceOK = checkService();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText(isServiceOK ? "息屏运行服务一切正常" : "息屏运行服务没有启动\n点我启动");
                        aSwitch.setEnabled(isServiceOK);
                        if (!isServiceOK) {
                            String command = "sh " + getExternalFilesDir(null).getPath() + "/starter.sh";
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("息屏运行需要adb或root权限激活。\n\nadb激活的方法是：手机打开USB调试连接电脑，电脑在命令窗口执行以下命令：\nadb shell " + command + "\n\n安卓6.0及更高版本的设备，还可以选择使用Shizuku激活(本质上Shizuku激活和adb权限激活是一样的)。\n")
                                    .setTitle("需要启动息屏运行服务")
                                    .setNeutralButton("复制命令", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", "adb shell " + command));
                                            Toast.makeText(MainActivity.this, "adb命令已复制到剪切板：\nadb shell " + command, Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton("root激活", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialoginterface, int i) {
                                            Process p;
                                            try {
                                                p = Runtime.getRuntime().exec("su");
                                                DataOutputStream o = new DataOutputStream(p.getOutputStream());
                                                o.writeBytes(command);
                                                o.flush();
                                                o.close();
                                                p.waitFor();
                                                Toast.makeText(MainActivity.this, p.exitValue() == 0 ? "成功激活" : "激活失败", Toast.LENGTH_SHORT).show();
                                            } catch (IOException | InterruptedException ignored) {
                                                Toast.makeText(MainActivity.this, "激活失败", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                builder.setPositiveButton("Shizuku激活", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        check();
                                    }
                                });
                            builder.create().show();
                        }
                    }
                });


            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (!isServiceOK) return true;
        if (keyCode == 25)
            aSwitch.setChecked(true);
        if (keyCode == 24)
            aSwitch.setChecked(false);
        return true;
    }

    public void e(View view) {
        startActivity(new Intent(MainActivity.this, Set.class));
    }


    private final Shizuku.OnRequestPermissionResultListener RL = new Shizuku.OnRequestPermissionResultListener() {
        @Override
        public void onRequestPermissionResult(int requestCode, int grantResult) {
            check();
        }
    };


    //检查Shizuku权限，申请Shizuku权限的函数
    private void check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
            return;
        boolean b = true, c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else c = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, "shizuku未运行", Toast.LENGTH_SHORT).show();
            }

        }
        if (b && c) {
            try {
                Process p = Shizuku.newProcess(new String[]{"sh"}, null, null);
                OutputStream out = p.getOutputStream();
                out.write(("sh " + getExternalFilesDir(null).getPath() + "/starter.sh").getBytes());
                out.flush();
                out.close();
                p.waitFor();
                Toast.makeText(MainActivity.this, p.exitValue() == 0 ? "成功激活" : "激活失败", Toast.LENGTH_SHORT).show();
            } catch (IOException | InterruptedException ioException) {
                Toast.makeText(this, "激活失败", Toast.LENGTH_SHORT).show();
            }
        }

    }


    //一些收尾工作，取消注册监听器什么的
    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Shizuku.removeRequestPermissionResultListener(RL);
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

}