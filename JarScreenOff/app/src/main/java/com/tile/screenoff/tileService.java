package com.tile.screenoff;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

@TargetApi(Build.VERSION_CODES.N)
public class tileService extends TileService {

    @Override
    public void onClick() {
        if (getQsTile() == null) return;
        startActivityAndCollapse(new Intent(tileService.this,ScrOff.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        super.onClick();
    }

}
