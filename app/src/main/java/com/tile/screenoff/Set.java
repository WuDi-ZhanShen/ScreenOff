package com.tile.screenoff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import rikka.shizuku.Shizuku;

public class Set extends Activity {
    SharedPreferences sp;
    SeekBar sb, sc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.set);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar1);
        sp = getSharedPreferences("s", 0);
        startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));

        Switch s1, s2, s3, s4;
        s1 = findViewById(R.id.s1);
        s2 = findViewById(R.id.s2);
        s3 = findViewById(R.id.s3);
        s4 = findViewById(R.id.s4);
        s1.setChecked(KeyDetect.GlobalControl);
        s2.setChecked(sp.getBoolean("float", true));
        s3.setChecked(sp.getBoolean("land", false));
        s3.setEnabled(s2.isChecked());
        s4.setChecked(!sp.getBoolean("canmove", true));
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    openAS();
                } else {
                    stopAS();
                }
            }
        });
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("float", b).apply();
                s3.setEnabled(b);
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
                if (seekBar.getProgress() < 50){
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
                if (seekBar.getProgress() < 10){
                    seekBar.setProgress(10);
                    Toast.makeText(Set.this, "太小啦！", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    public void e(View view) {
        TextView v = new TextView(this);
        v.setAutoLinkMask(Linkify.ALL);
        v.setGravity(Gravity.CENTER);
        v.setPadding(20, 30, 20, 30);
        v.setTextSize(16f);
        v.setTextIsSelectable(true);
        v.setText("其他息屏方式：\n1.添加息屏磁帖，点击磁帖会立即息屏并收起通知面板。\n2.启动本应用中名为ScrOff的活动来立即息屏。\n\n\n注：大部分设备在息屏运行后会屏蔽触控，所以双击亮屏功能无法使用。\n小部分设备在息屏运行之后会屏蔽按键，所以按键亮屏也无法使用。\n\n\n注：如果您的LCD设备息屏运行之后依然有屏幕背光，那么不要怪本APP，这是系统内核或者FrameWork问题。");
        new AlertDialog.Builder(this).setTitle("说明").setView(v).show();
    }


    public void defaultsize(View view) {
        sb.setProgress(150);
    }

    public void defaulttran(View view) {
        sc.setProgress(90);
    }

    void openAS() {
        if (!KeyDetect.GlobalControl) {
            try {
                Process p = Shizuku.newProcess(new String[]{"sh"}, null, null);
                OutputStream out = p.getOutputStream();
                out.write(("settings put secure enabled_accessibility_services \"com.tile.screenoff/.KeyDetect:$(settings get secure enabled_accessibility_services)\"\nexit\n").getBytes());
                out.flush();
                out.close();
                p.waitFor();
                if (p.exitValue() == 0)
                    Toast.makeText(this, "成功开启无障碍服务", Toast.LENGTH_SHORT).show();
            } catch (IOException | InterruptedException ioException) {
                Toast.makeText(this, "开启失败，请手动开启无障碍服务", Toast.LENGTH_SHORT).show();
                Bundle bundle = new Bundle();
                bundle.putString(":settings:fragment_args_key", new ComponentName(getPackageName(), KeyDetect.class.getName()).flattenToString());
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).putExtra(":settings:fragment_args_key", new ComponentName(getPackageName(), KeyDetect.class.getName()).flattenToString()).putExtra(":settings:show_fragment_args", bundle));
            }
        }
    }

    private void stopAS() {
        if (KeyDetect.GlobalControl) {

            Toast.makeText(this, "请手动关闭无障碍服务", Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putString(":settings:fragment_args_key", new ComponentName(getPackageName(), KeyDetect.class.getName()).flattenToString());
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).putExtra(":settings:fragment_args_key", new ComponentName(getPackageName(), KeyDetect.class.getName()).flattenToString()).putExtra(":settings:show_fragment_args", bundle));

        }
    }

}
