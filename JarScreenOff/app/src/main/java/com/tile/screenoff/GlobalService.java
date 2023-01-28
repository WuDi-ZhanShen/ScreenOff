package com.tile.screenoff;

import static java.lang.Math.abs;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GlobalService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    long lastup = 0, lastdown = 0;
    public static boolean GlobalControl = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    LinearLayout layout;
    ImageView view;
    boolean moved = false, isScrOff = false;
    SharedPreferences sp;
    private boolean exist = false, canmove, doubleTap, shake, volume;
    int size, sensity;
    private int SCREEN_WIDTH, SCREEN_HEIGHT;
    OrientationEventListener listener;

    BroadcastReceiver myReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "android.intent.action.SCREEN_OFF":
                    isScrOff = false;
                    layout.setKeepScreenOn(false);
                    listener.disable();
                    view.setFocusableInTouchMode(false);
                    view.clearFocus();
                    view.setOnKeyListener(null);
                    params.flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    if (exist)
                        windowManager.updateViewLayout(layout, params);
                    break;
                case "action.ScrOff":
                    screenoff(intent.getBooleanExtra("state", true));
                    break;
            }

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        floatwindow();
        layout.setVisibility(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && sharedPreferences.getBoolean("land", false) ? View.GONE : View.VISIBLE);
        size = sharedPreferences.getInt("size", 150);
        params.height = size;
        params.width = size;
        params.alpha = sharedPreferences.getInt("tran", 90) * 0.01f;
        canmove = sharedPreferences.getBoolean("canmove", true);
        doubleTap = sharedPreferences.getBoolean("doubleTap", false);
        shake = sharedPreferences.getBoolean("shake", false);
        sensity = sharedPreferences.getInt("sensity", 10);
        volume = sharedPreferences.getBoolean("volume", false);
        if (exist) {
            windowManager.updateViewLayout(layout, params);
        }
    }

    @Override
    public void onCreate() {

        GlobalControl = true;

        sp = getSharedPreferences("s", 0);
        sp.registerOnSharedPreferenceChangeListener(this);
        sensity = sp.getInt("sensity", 10);
        size = sp.getInt("size", 150);
        listener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
                boolean wake = false;
                //下面是手机旋转准确角度与四个方向角度（0 90 180 270）的转换
                if (orientation >= 360 - sensity || orientation <= sensity) {
                    wake = true;
                } else if (orientation >= 90 - sensity && orientation <= 90 + sensity) {
                    wake = true;
                } else if (orientation >= 180 - sensity && orientation <= 180 + sensity) {
                    wake = true;
                } else if (orientation >= 270 - sensity && orientation <= 270 + sensity) {
                    wake = true;
                }
                if (wake) {
                    screenoff(false);
                    this.disable();
                }
            }

        };
        windowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);
        GetWidthHeight();
        params = new WindowManager.LayoutParams(size, size, Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, 1);

        params.alpha = sp.getInt("tran", 90) * 0.01f;
        boolean isLand = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        params.x = sp.getInt("x" + (isLand ? "1" : "2"), 0);
        params.y = sp.getInt("y" + (isLand ? "1" : "2"), 0);
        layout = new LinearLayout(this);
        canmove = sp.getBoolean("canmove", true);
        doubleTap = sp.getBoolean("doubleTap", false);
        shake = sp.getBoolean("shake", false);
        volume = sp.getBoolean("volume", false);
        layout.setOnTouchListener(new View.OnTouchListener() {
            int lastX = 0;
            int lastY = 0;
            int paramX = 0;
            int paramY = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        moved = false;
                        lastX = (int) motionEvent.getRawX();
                        lastY = (int) motionEvent.getRawY();
                        paramX = params.x;
                        paramY = params.y;

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
                        params.x = paramX + dx;
                        params.y = paramY + dy;
                        windowManager.updateViewLayout(layout, params);

                        break;
                    case MotionEvent.ACTION_UP:

                        if (params.x > (SCREEN_WIDTH - size) * 0.43)
                            params.x = (SCREEN_WIDTH - size) / 2;
                        if (params.x < (SCREEN_WIDTH - size) * -0.43)
                            params.x = -(SCREEN_WIDTH - size) / 2;

                        windowManager.updateViewLayout(layout, params);
                        lastup = System.currentTimeMillis();
                        params.x = Math.min(params.x, (SCREEN_WIDTH - size) / 2);
                        params.x = Math.max(params.x, -(SCREEN_WIDTH - size) / 2);
                        params.y = Math.min(params.y, (SCREEN_HEIGHT - size) / 2);
                        params.y = Math.max(params.y, -(SCREEN_HEIGHT - size) / 2);
                        boolean isLand = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                        sp.edit().putInt("x" + (isLand ? "1" : "2"), params.x).putInt("y" + (isLand ? "1" : "2"), params.y).apply();
                }

                if (isScrOff) {
                    if (!doubleTap) return false;
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
                return true;
            }
        });
        layout.setVisibility(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && sp.getBoolean("land", false) ? View.GONE : View.VISIBLE);

        view = new ImageView(this);
        ShapeDrawable oval = new ShapeDrawable(new OvalShape());
        oval.getPaint().setColor(Color.DKGRAY);
        view.setBackground(oval);
        view.setImageResource(R.mipmap.ic);

        layout.addView(view);
        floatwindow();

        registerReceiver(myReceiver, new IntentFilter("action.ScrOff"));
        registerReceiver(myReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
        super.onCreate();
    }


    void screenoff(Boolean bb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 8090);
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    printWriter.println(bb ? "off" : "on");
                    printWriter.close();
                    socket.close();
                } catch (IOException ignored) {

                }
            }
        }).start();
        isScrOff = bb;
        layout.setKeepScreenOn(bb);

        if (shake && bb) listener.enable();
        if (volume) {
            if (bb) {
                params.flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                if (exist)
                    windowManager.updateViewLayout(layout, params);
                view.setFocusableInTouchMode(true);
                view.requestFocus();
                view.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View view, int i, KeyEvent keyEvent) {
                        int keyCode = keyEvent.getKeyCode();
                        if (keyCode != 24 && keyCode != 25) return false;
                        if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                            screenoff(false);
                        return isScrOff;
                    }
                });

            } else {
                params.flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                if (exist)
                    windowManager.updateViewLayout(layout, params);
                view.setFocusableInTouchMode(false);
                view.clearFocus();
                view.setOnKeyListener(null);

            }
        }

    }

    void floatwindow() {
        if (sp.getBoolean("float", true)) {
            if (!exist) {
                windowManager.addView(layout, params);
                exist = true;
            } else {
                windowManager.updateViewLayout(layout, params);
            }
        } else {
            if (layout != null) {
                try {
                    windowManager.removeView(layout);
                } catch (Exception ignored) {
                }
                exist = false;
            }
        }
    }


    void GetWidthHeight() {
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
            layout.setVisibility(isLand ? View.GONE : View.VISIBLE);
        GetWidthHeight();
        params.x = sp.getInt("x" + (isLand ? "1" : "2"), 0);
        params.y = sp.getInt("y" + (isLand ? "1" : "2"), 0);

        if (exist) windowManager.updateViewLayout(layout, params);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(myReceiver);
        try {
            windowManager.removeView(layout);
        } catch (Exception ignored) {
        }
        exist = false;
        GlobalControl = false;
        listener.disable();
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }


}
