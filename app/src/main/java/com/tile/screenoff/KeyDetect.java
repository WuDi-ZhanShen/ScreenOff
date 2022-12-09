package com.tile.screenoff;

import static java.lang.Math.abs;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class KeyDetect extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    long lastup = 0, lastdown = 0;
    public static boolean GlobalControl = false, isScrOff = false;
    private WindowManager a;
    private WindowManager.LayoutParams e;
    LinearLayout b;
    boolean moved = false;
    SharedPreferences sp;
    private boolean exist, canmove;
    int size = 150;
    private int SCREEN_WIDTH, SCREEN_HEIGHT;
    private PowerManager.WakeLock wakeLock = null;

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            releaseCaffeine();
            isScrOff = false;
        }
    };


    private void releaseCaffeine() {
        if (wakeLock.isHeld())
            wakeLock.release();
    }


    private void acquireCaffeine() {
        if (!wakeLock.isHeld())
            wakeLock.acquire();
    }

    @Override
    protected void onServiceConnected() {

        GlobalControl = true;
        GetWidthHeight();
        sp = getSharedPreferences("s", 0);
        sp.registerOnSharedPreferenceChangeListener(this);
        a = (WindowManager) getSystemService(Service.WINDOW_SERVICE);
        e = new WindowManager.LayoutParams();
        size = sp.getInt("size", 150);
        e.height = size;
        e.width = size;
        e.alpha = sp.getInt("tran", 90) * 0.01f;
        e.format = PixelFormat.TRANSLUCENT;
        e.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        e.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        boolean isLand = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        e.x = sp.getInt("x" + (isLand ? "1" : "2"), 0);
        e.y = sp.getInt("y" + (isLand ? "1" : "2"), 0);
        b = (LinearLayout) LayoutInflater.from(getApplication()).inflate(R.layout.fw, null);
        canmove = sp.getBoolean("canmove", true);
        b.setOnTouchListener(new View.OnTouchListener() {
            int lastX = 0;
            int lastY = 0;
            int paramX = 0;
            int paramY = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!isScrOff) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            moved = false;
                            lastX = (int) motionEvent.getRawX();
                            lastY = (int) motionEvent.getRawY();
                            paramX = e.x;
                            paramY = e.y;

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (System.currentTimeMillis() - lastup >= 400 && !moved) {
                                        screenoff(true);
                                    }
                                }
                            }, 400);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (!canmove) return true;
                            int dx = (int) motionEvent.getRawX() - lastX;
                            int dy = (int) motionEvent.getRawY() - lastY;
                            if (abs(dx) > 4 || abs(dy) > 4)
                                moved = true;
                            e.x = paramX + dx;
                            e.y = paramY + dy;
                            a.updateViewLayout(b, e);

                            break;
                        case MotionEvent.ACTION_UP:

                            if (e.x > (SCREEN_WIDTH - size) * 0.43)
                                e.x = (SCREEN_WIDTH - size) / 2;
                            if (e.x < (SCREEN_WIDTH - size) * -0.43)
                                e.x = -(SCREEN_WIDTH - size) / 2;

                            a.updateViewLayout(b, e);
                            lastup = System.currentTimeMillis();
                            e.x = e.x > (SCREEN_WIDTH - size) / 2 ? (SCREEN_WIDTH - size) / 2 : e.x;
                            e.x = e.x < -(SCREEN_WIDTH - size) / 2 ? -(SCREEN_WIDTH - size) / 2 : e.x;
                            e.y = e.y > (SCREEN_HEIGHT - size) / 2 ? (SCREEN_HEIGHT - size) / 2 : e.y;
                            e.y = e.y < -(SCREEN_HEIGHT - size) / 2 ? -(SCREEN_HEIGHT - size) / 2 : e.y;
                            boolean isLand = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                            sp.edit().putInt("x" + (isLand ? "1" : "2"), e.x).putInt("y" + (isLand ? "1" : "2"), e.y).apply();
                    }
                    return true;
                } else {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_OUTSIDE:
                            if (System.currentTimeMillis() - lastdown <= 400)
                                screenoff(false);
                            lastdown = System.currentTimeMillis();
                            break;
                    }
                    return false;
                }

            }
        });
        b.setVisibility(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && sp.getBoolean("land", false) ? View.GONE : View.VISIBLE);
        View c = b.findViewById(R.id.i);
        c.setBackgroundResource(R.mipmap.ic);
        floatwindow();
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "1:1");
        registerReceiver(this.myReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
        checkService();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        if (!isScrOff) return false;

        if (event.getKeyCode() != 24 && event.getKeyCode() != 25)
            return false;
        if (event.getAction() == KeyEvent.ACTION_UP)
            screenoff(false);
        super.onKeyEvent(event);
        return true;


    }


    void checkService() {
        if (!MainActivity.isServiceOK)
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(KeyDetect.this, "shizuku未授权", Toast.LENGTH_SHORT).show();
                else
                    Shizuku.bindUserService(MainActivity.userServiceArgs, MainActivity.userServiceConnection);
            } catch (Exception e) {
                Toast.makeText(KeyDetect.this, "shizuku未运行", Toast.LENGTH_SHORT).show();
            }

    }

    void screenoff(Boolean b) {
        if (MainActivity.isServiceOK) {
            try {
                MainActivity.userService.ScreenOff(b);
                isScrOff = b;
            } catch (RemoteException ignored) {
            }
            if (b)
                acquireCaffeine();
            else
                releaseCaffeine();
        } else
            checkService();

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        floatwindow();
        b.setVisibility(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && sp.getBoolean("land", false) ? View.GONE : View.VISIBLE);
        size = sp.getInt("size", 150);
        e.height = size;
        e.width = size;
        e.alpha = sp.getInt("tran", 90) * 0.01f;
        canmove = sp.getBoolean("canmove", true);
        if (exist) {
            a.updateViewLayout(b, e);
        }
    }

    void floatwindow() {
        if (sp.getBoolean("float", true)) {
            if (!exist) {
                a.addView(b, e);
                exist = true;
            } else {
                a.updateViewLayout(b, e);
            }
        } else {
            if (b != null) {
                try {
                    a.removeView(b);
                } catch (Exception ignored) {
                }
                exist = false;
            }
        }
    }


    void GetWidthHeight() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        SCREEN_WIDTH = metrics.widthPixels;
        SCREEN_HEIGHT = metrics.heightPixels;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isLand = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        if (sp.getBoolean("land", false))
            b.setVisibility(isLand ? View.GONE : View.VISIBLE);
        GetWidthHeight();
        e.x = sp.getInt("x" + (isLand ? "1" : "2"), 0);
        e.y = sp.getInt("y" + (isLand ? "1" : "2"), 0);

        if (exist) a.updateViewLayout(b, e);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(myReceiver);
        releaseCaffeine();
        GlobalControl = false;
        sp.unregisterOnSharedPreferenceChangeListener(this);
        Shizuku.unbindUserService(MainActivity.userServiceArgs, MainActivity.userServiceConnection,true);
        super.onDestroy();
    }

}

