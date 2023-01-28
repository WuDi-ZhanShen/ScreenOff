package com.tile.screenoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context mContext, Intent intent) {

        if (mContext.getSharedPreferences("s", 0).getBoolean("boot", true)) {
            mContext.startService(new Intent(mContext, GlobalService.class));
        }

    }
}

