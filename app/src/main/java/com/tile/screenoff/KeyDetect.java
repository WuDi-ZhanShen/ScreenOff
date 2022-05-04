package com.tile.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class KeyDetect extends AccessibilityService {

    long lastDown, lastUp;
    public static boolean GlobalControl=false;

    @Override
    protected void onServiceConnected() {
        lastUp = System.currentTimeMillis();
        GlobalControl=true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_VOLUME_DOWN && event.getKeyCode() != KeyEvent.KEYCODE_VOLUME_UP) return false;
        if (!MainActivity.isServiceOK) {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(KeyDetect.this, "shizuku未授权", Toast.LENGTH_SHORT).show();
                else
                    Shizuku.bindUserService(MainActivity.userServiceArgs, MainActivity.userServiceConnection);
            } catch (Exception e) {
                Toast.makeText(KeyDetect.this, "shizuku未运行", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            lastDown = System.currentTimeMillis();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - lastUp >= 400)
                        if (MainActivity.isServiceOK)
                            try {
                                MainActivity.userService.ScreenOff(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP);
                            } catch (RemoteException ignored) {
                            }
                }
            }, 400);
        }
        if (event.getAction() == KeyEvent.ACTION_UP) lastUp = System.currentTimeMillis();
        super.onKeyEvent(event);
        return false;
    }

}
