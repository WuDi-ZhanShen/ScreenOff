package com.tile.screenoff;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;

import rikka.shizuku.Shizuku;

public class ScrOff extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        if (!MainActivity.isServiceOK)
            try {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                    Shizuku.bindUserService(MainActivity.userServiceArgs, MainActivity.userServiceConnection);
            } catch (Exception ignored) {
            }
        else
            try {
                MainActivity.userService.ScreenOff(true);
            } catch (RemoteException ignored) {
            }
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}