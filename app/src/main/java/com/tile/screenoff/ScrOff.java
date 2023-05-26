package com.tile.screenoff;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

public class ScrOff extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        final String setting = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (setting != null && setting.contains(getPackageName())) {
            sendBroadcast(new Intent("action.ScrOff").putExtra("state", true));
        } else {
            final String serviceName = new ComponentName(getPackageName(), GlobalService.class.getName()).flattenToString();
            final String oldSetting = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            final String newSetting = oldSetting == null ? serviceName : serviceName + ":" + oldSetting;
            try {
                Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newSetting);
            } catch (Exception e) {
                Bundle bundle = new Bundle();
                bundle.putString(":settings:fragment_args_key", serviceName);
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).putExtra(":settings:fragment_args_key", serviceName).putExtra(":settings:show_fragment_args", bundle));
            }
        }
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}