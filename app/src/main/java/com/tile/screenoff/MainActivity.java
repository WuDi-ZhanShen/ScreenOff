package com.tile.screenoff;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    public Switch s1;
    boolean b, c;
    Button B, C;
    int m;
    TextView t;
    public static IUserService userService;
    public static boolean isServiceOK = false;

    private final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = new Shizuku.OnBinderReceivedListener() {
        @Override
        public void onBinderReceived() {
            if (Shizuku.isPreV11()) {
                s1.setEnabled(false);
                t.setText("Shizuku pre-v11 is not supported");
                isServiceOK = false;
            }
        }
    };

    private final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = new Shizuku.OnBinderDeadListener() {
        @Override
        public void onBinderDead() {
            t.setText("Binder deaded");
            s1.setEnabled(false);
            isServiceOK = false;
        }
    };

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        s1 = findViewById(R.id.s1);
        B = findViewById(R.id.b);
        C = findViewById(R.id.c);
        m = B.getCurrentTextColor();
        t = findViewById(R.id.t);
        check();
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                try {
                    userService.ScreenOff(!isChecked);
                } catch (RemoteException ignored) {
                }
            }
        });

        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private void check() {
        b = true;
        c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else {
                if (!isServiceOK) bindUserService();
                c = true;
            }
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, "Shizuku未运行", Toast.LENGTH_SHORT).show();
            }
        }
        B.setText(b ? "Shizuku\n已运行" : "Shizuku\n未运行");
        B.setTextColor(b ? m : 0x77ff0000);
        C.setText(c ? "Shizuku\n已授权" : "Shizuku\n未授权");
        C.setTextColor(c ? m : 0x77ff0000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeBinderReceivedListener(BINDER_RECEIVED_LISTENER);
        Shizuku.removeBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        if (!KeyDetect.GlobalControl) unbindUserService();
    }

    private void onRequestPermissionsResult(int i, int i1) {
        check();
    }

    public static final ServiceConnection userServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            if (binder != null && binder.pingBinder()) {
                userService = IUserService.Stub.asInterface(binder);
                isServiceOK = true;

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceOK = false;
        }
    };

    public static final Shizuku.UserServiceArgs userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, UserService.class.getName())).processNameSuffix("service");

    private void bindUserService() {
        try {
            if (Shizuku.getVersion() < 10) {
                t.setText("requires Shizuku API 10");
                s1.setEnabled(false);
            } else
                Shizuku.bindUserService(userServiceArgs, userServiceConnection);
        } catch (Throwable ignored) {
        }
    }

    private void unbindUserService() {
        try {
            if (Shizuku.getVersion() < 10) {
                t.setText("requires Shizuku API 10");
                s1.setEnabled(false);
            } else
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true);
        } catch (Throwable ignored) {
        }
    }

    public void ch(View view) {
        check();
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (!isServiceOK) return true;
        if (keyCode ==25)
            s1.setChecked(true);
        if (keyCode ==24)
            s1.setChecked(false);
        return true;
    }


    public void e(View view) {

            startActivity(new Intent(MainActivity.this, Set.class));

    }
}
