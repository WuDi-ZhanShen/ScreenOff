package com.tile.screenoff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

public class Set extends Activity {
    SharedPreferences sp;
    SeekBar sb, sc, sd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.set);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar1);
        sp = getSharedPreferences("s", 0);

        Switch s1, s2, s3, s4, s5, s6, s7;
        s1 = findViewById(R.id.s1);
        s2 = findViewById(R.id.s2);
        s3 = findViewById(R.id.s3);
        s4 = findViewById(R.id.s4);
        s5 = findViewById(R.id.s5);
        s6 = findViewById(R.id.s6);
        s7 = findViewById(R.id.s7);
        s1.setChecked(GlobalService.GlobalControl);
        s2.setChecked(sp.getBoolean("float", true));
        s3.setChecked(sp.getBoolean("land", false));
        s4.setChecked(!sp.getBoolean("canmove", true));
        s5.setChecked(sp.getBoolean("doubleTap", false));
        s6.setChecked(sp.getBoolean("shake", false));
        s7.setChecked(sp.getBoolean("volume", false));
        s3.setEnabled(s2.isChecked());
        s4.setEnabled(s2.isChecked());
        s5.setEnabled(s2.isChecked());
        s6.setEnabled(s2.isChecked());
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (Build.VERSION.SDK_INT >= 23) {
                    if (!((PowerManager) getSystemService(Service.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()))
                        startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                }

                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(Set.this)) {
                    startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())));
                }
                if (b)
                    startService(new Intent(Set.this, GlobalService.class));
                else
                    stopService(new Intent(Set.this, GlobalService.class));
            }
        });
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("float", b).apply();
                s3.setEnabled(b);
                s4.setEnabled(b);
                s5.setEnabled(b);
                s6.setEnabled(b);
            }
        });
        s3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("land", b).apply();
            }
        });
        s4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("canmove", !b).apply();
            }
        });
        s5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("doubleTap", b).apply();
            }
        });
        s6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("shake", b).apply();
            }
        });
        s7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("volume", b).apply();
            }
        });
        sb = findViewById(R.id.sb);
        sb.setProgress(sp.getInt("size", 150));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("size", sb.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 50) {
                    seekBar.setProgress(50);
                    Toast.makeText(Set.this, "太小啦！", Toast.LENGTH_SHORT).show();
                }

            }
        });
        sc = findViewById(R.id.sc);
        sc.setProgress(sp.getInt("tran", 90));
        sc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("tran", sc.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 10) {
                    seekBar.setProgress(10);
                    Toast.makeText(Set.this, "太小啦！", Toast.LENGTH_SHORT).show();
                }

            }
        });

        sd = findViewById(R.id.sd);
        sd.setProgress(sp.getInt("sensity", 10));
        sd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("sensity", sd.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 1) {
                    seekBar.setProgress(1);
                    Toast.makeText(Set.this, "太小啦！", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    public void e(View view) {

        new AlertDialog.Builder(this).setTitle("说明").setMessage("息屏运行期间，系统不会自动休眠，一切APP都会和亮屏时一样继续工作。直到您按下电源键才会真正的息屏。\n\n\n立起亮屏功能开启时，我会在息屏运行后监视设备方向是否改变。这将增加息屏运行后的极少量耗电，几乎可以忽略不计。\n\n\n注：大部分设备在息屏运行后会屏蔽触控，所以双击亮屏仅在少数设备(已知三星sPen平板、IQOO 9、红米Note1 LTE等)上有效。\n\n\n小部分设备在息屏运行之后会屏蔽按键，所以音量键亮屏在这些设备(已知华为鸿蒙的某些手机、Origin OS的某些手机)上无效。有这类设备的酷友可以联系我，一起研究使用getevent命令来检测音量按键的方法，很可能可行。\n\n\n部分LCD设备息屏运行之后依然有屏幕背光。跟屏蔽按键、屏蔽触控一样，这都是系统工程师决定的，我无法改变。但是您可以在息屏运行前先把屏幕亮度调到最小，以把背光降到最低。").show();
    }


    public void defaultsize(View view) {
        sb.setProgress(150);
    }

    public void defaulttran(View view) {
        sc.setProgress(90);
    }

    public void defaultsensity(View view) {
        sd.setProgress(10);
    }


}

