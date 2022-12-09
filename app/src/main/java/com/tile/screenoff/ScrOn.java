package com.tile.screenoff;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;

import rikka.shizuku.Shizuku;

public class ScrOn extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        if (!MainActivity.isServiceOK)
            try {
                    Shizuku.bindUserService(MainActivity.userServiceArgs, MainActivity.userServiceConnection);
            } catch (Exception ignored) {
            }
        else
            try {
                MainActivity.userService.ScreenOff(false);
                KeyDetect.isScrOff = false;
            } catch (RemoteException ignored) {
            }
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}
