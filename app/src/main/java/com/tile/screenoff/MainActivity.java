package com.tile.screenoff;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private boolean isExpand = false, isServiceOK = false, isPermissionResultListenerRegistered = false;
    private int scrOffKey, scrOnKey;
    public IScreenOff iScreenOff = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BinderContainer binderContainer = intent.getParcelableExtra("binder");
            IBinder binder = binderContainer.getBinder();
            //如果binder已经失去活性了，则不再继续解析
            if (!binder.pingBinder()) return;
            iScreenOff = IScreenOff.Stub.asInterface(binder);
            enableScreenOffFunctions();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.getAttributes().dimAmount = 0.5f;
        setContentView(R.layout.main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.setNavigationBarContrastEnforced(false);
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        window.setNavigationBarColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ? Color.TRANSPARENT : getColor(isNight ? R.color.bgBlack : R.color.bgWhite) : (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ? Color.TRANSPARENT : (isNight ? 0xff303034 : 0xffe4e2e6));
        window.setStatusBarColor(Color.TRANSPARENT);

        SharedPreferences sp = getSharedPreferences("s", 0);
        if (sp.getBoolean("first", true)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.privacy)
                    .setMessage(R.string.privacypolicy)
                    .setNegativeButton(R.string.agree, (dialogInterface, i) -> {
                        help();
                        sp.edit().putBoolean("first", false).apply();
                    })
                    .setCancelable(false)
                    .setPositiveButton(R.string.disagree, (dialogInterface, i) -> finish())
                    .show();


        }

        setButtonsOnclick(isNight, sp);
        registerReceiver(mBroadcastReceiver, new IntentFilter("intent.screenoff.sendBinder"));

        super.onCreate(savedInstanceState);

    }


    private void showNet() {
        String[] i = new String[]{"wlan: ", "eth: ", "usb: ", "p2p: ", "lo: ", "unknown: "};
        int i2;
        boolean avalible = false;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface nextElement = networkInterfaces.nextElement();
                String name = nextElement.getName().toLowerCase(Locale.US);
                if (name.contains("wlan"))
                    i2 = 0;
                else if (name.contains("eth"))
                    i2 = 1;
                else if (name.contains("usb"))
                    i2 = 2;
                else if (name.contains("p2p"))
                    i2 = 3;
                else if (name.contains("lo"))
                    i2 = 4;
                else
                    i2 = 5;
                Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress nextElement2 = inetAddresses.nextElement();
                    if (!nextElement2.isLoopbackAddress() && nextElement2 instanceof Inet4Address) {
                        i[i2] += nextElement2.getHostAddress() + ":" + GlobalService.port + " ";
                        avalible = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        int j = 0;
        StringBuilder sb = new StringBuilder();
        while (j < 5) {
            if (Pattern.compile(": ").split(i[j]).length > 1) sb.append(i[j]);
            j++;
        }
        TextView textView = findViewById(R.id.title_text);
        textView.setOnClickListener(null);
        textView.setText(avalible ? sb.toString() : "no network avalible");
    }

    private void setButtonsOnclick(boolean isNight, SharedPreferences sp) {

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.left).setVisibility(View.VISIBLE);
            findViewById(R.id.right).setVisibility(View.VISIBLE);
        }
        LinearLayout linearLayout = findViewById(R.id.ll);
        EditText e1 = findViewById(R.id.e1);
        EditText e2 = findViewById(R.id.e2);
        Switch s1, s2, s3, s4, s5, s6, s7, s8;
        s1 = findViewById(R.id.s1);
        s2 = findViewById(R.id.s2);
        s3 = findViewById(R.id.s3);
        s4 = findViewById(R.id.s4);
        s5 = findViewById(R.id.s5);
        s6 = findViewById(R.id.s6);
        s7 = findViewById(R.id.s7);
        s8 = findViewById(R.id.s8);
        final String setting = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        s1.setChecked(setting != null && setting.contains(getPackageName()));
        s2.setChecked(sp.getBoolean("float", true));
        s3.setChecked(sp.getBoolean("land", false));
        s4.setChecked(!sp.getBoolean("canmove", true));
        s5.setChecked(sp.getBoolean("doubleTap", false));
        s6.setChecked(sp.getBoolean("shake", false));
        s7.setChecked(sp.getBoolean("volume", false));
        s8.setChecked(sp.getBoolean("net", false));
        s3.setEnabled(s2.isChecked());
        s4.setEnabled(s2.isChecked());
        s5.setEnabled(s2.isChecked());
        SeekBar sb = findViewById(R.id.sb);
        sb.setProgress(sp.getInt("size", 50));
        EditText eb = findViewById(R.id.eb);
        eb.setText(String.valueOf(sp.getInt("size", 50)));
        SeekBar sc = findViewById(R.id.sc);
        sc.setProgress(sp.getInt("tran", 90));
        EditText ec = findViewById(R.id.ec);
        ec.setText(String.valueOf(sp.getInt("tran", 90)));
        SeekBar sd = findViewById(R.id.sd);
        sd.setProgress(sp.getInt("sensity", 10));
        EditText ed = findViewById(R.id.ed);
        ed.setText(String.valueOf(sp.getInt("sensity", 10)));
        sb.setEnabled(s2.isChecked());
        sc.setEnabled(s2.isChecked());
        sd.setEnabled(s2.isChecked());
        s1.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !((PowerManager) getSystemService(Service.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()))
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));

            if (!isServiceOK) {
                compoundButton.setChecked(false);
                Toast.makeText(MainActivity.this, R.string.active_first, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isChecked) {
                final String serviceName = new ComponentName(getPackageName(), GlobalService.class.getName()).flattenToString();
                final String oldSetting = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                final String newSetting = oldSetting == null ? serviceName : serviceName + ":" + oldSetting;
                try {
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                    Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newSetting);
                } catch (Exception e) {
                    compoundButton.setChecked(false);
                    Toast.makeText(MainActivity.this, R.string.mannually_open, Toast.LENGTH_SHORT).show();
                    Bundle bundle = new Bundle();
                    bundle.putString(":settings:fragment_args_key", serviceName);
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).putExtra(":settings:fragment_args_key", serviceName).putExtra(":settings:show_fragment_args", bundle));
                }
                if (s8.isChecked()) showNet();
            } else {
                ((TextView) findViewById(R.id.title_text)).setText(R.string.shortcutoff);
                sendBroadcast(new Intent("intent.screenoff.exit"));
            }

        });
        s2.setOnCheckedChangeListener((compoundButton, b) -> {
            sp.edit().putBoolean("float", b).apply();
            s3.setEnabled(b);
            s4.setEnabled(b);
            s5.setEnabled(b);
            sb.setEnabled(b);
            sc.setEnabled(b);
            sd.setEnabled(b);
        });
        s3.setOnCheckedChangeListener((compoundButton, b) -> sp.edit().putBoolean("land", b).apply());
        s4.setOnCheckedChangeListener((compoundButton, b) -> sp.edit().putBoolean("canmove", !b).apply());
        s5.setOnCheckedChangeListener((compoundButton, b) -> sp.edit().putBoolean("doubleTap", b).apply());
        s6.setOnCheckedChangeListener((compoundButton, b) -> sp.edit().putBoolean("shake", b).apply());
        s7.setOnCheckedChangeListener((compoundButton, b) -> {
            sp.edit().putBoolean("volume", b).apply();
            e1.setEnabled(b);
            e2.setEnabled(b);
        });
        s8.setOnCheckedChangeListener((compoundButton, b) -> {
            if (s1.isChecked()) {
                if (b) showNet();
                else ((TextView) findViewById(R.id.title_text)).setText(R.string.shortcutoff);
            }
            sp.edit().putBoolean("net", b).apply();
        });
        if (s1.isChecked() && s8.isChecked()) showNet();
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("size", i).apply();
                eb.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        eb.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && eb.getText().length() > 0) {
                int value = Integer.parseInt(eb.getText().toString());
                if (value >= 0 && value <= 100) {
                    sp.edit().putInt("size", value).apply();
                    sb.setProgress(value);
                }
            }
            return false;
        });

        sc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("tran", i).apply();
                ec.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        ec.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && ec.getText().length() > 0) {
                int value = Integer.parseInt(ec.getText().toString());
                if (value >= 0 && value <= 100) {
                    sp.edit().putInt("tran", value).apply();
                    sc.setProgress(value);
                }
            }
            return false;
        });

        sd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("sensity", i).apply();
                ed.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 1) {
                    seekBar.setProgress(1);
                    Toast.makeText(MainActivity.this, R.string.toosmall, Toast.LENGTH_SHORT).show();
                }

            }
        });
        ed.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && ed.getText().length() > 0) {
                int value = Integer.parseInt(ed.getText().toString());
                if (value >= 0 && value <= 30) {
                    sp.edit().putInt("sensity", value).apply();
                    sd.setProgress(value);
                }
            }
            return false;
        });
        e1.setEnabled(s7.isChecked());
        e2.setEnabled(s7.isChecked());
        scrOffKey = sp.getInt("scrOffKey", 25);
        scrOnKey = sp.getInt("scrOnKey", 24);
        e1.setText(String.valueOf(scrOffKey));
        e2.setText(String.valueOf(scrOnKey));
        e1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    scrOffKey = Integer.parseInt(String.valueOf(charSequence));
                    sp.edit().putInt("scrOffKey", scrOffKey).apply();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        e2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    scrOnKey = Integer.parseInt(String.valueOf(charSequence));
                    sp.edit().putInt("scrOnKey", scrOnKey).apply();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        findViewById(R.id.title_text).setOnClickListener(view -> help());
        float density = getResources().getDisplayMetrics().density;
        findViewById(R.id.activate_button).setOnClickListener(view -> showActivate());
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{30 * density, 30 * density, 30 * density, 30 * density, 0, 0, 0, 0}, null, null));
        oval.getPaint().setColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getColor(isNight ? R.color.bgBlack : R.color.bgWhite) : (isNight ? 0xff303034 : 0xffe4e2e6));
        linearLayout.setBackground(oval);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(400L);
        ObjectAnimator animator = ObjectAnimator.ofFloat(null, "scaleX", 0.0f, 1.0f);
        transition.setAnimator(2, animator);
        linearLayout.setLayoutTransition(transition);
        ScrollView scrollView = findViewById(R.id.sv);
        Switch aSwitch = findViewById(R.id.screenoff_switch);
        aSwitch.setOnCheckedChangeListener((compoundButton, b) ->
        {
            if (!isServiceOK) return;
            try {
                iScreenOff.setPowerMode(!b);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
        ImageView imageView = findViewById(R.id.iv);
        final View.OnClickListener onClickListener = view -> {
            if (isExpand) {
                linearLayout.removeView(scrollView);
                ObjectAnimator a2 = ObjectAnimator.ofFloat(imageView, "rotation", 180f, 360f);
                a2.setDuration(800).setInterpolator(new AccelerateDecelerateInterpolator());
                a2.start();
            } else {
                linearLayout.addView(scrollView);
                ObjectAnimator a2 = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 180f);
                a2.setDuration(800).setInterpolator(new AccelerateDecelerateInterpolator());
                a2.start();
            }
            isExpand = !isExpand;
        };
        imageView.setOnClickListener(onClickListener);
        linearLayout.setOnClickListener(onClickListener);
        findViewById(R.id.lll).setOnClickListener(onClickListener);
        linearLayout.removeView(scrollView);

    }


    public void enableScreenOffFunctions() {
        Button button = findViewById(R.id.activate_button);
        isServiceOK = true;
        button.setText(getString(R.string.all_ok));
        button.setTextColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getColor(R.color.right) : 0x00000000);
        button.setOnClickListener(null);
        button.setOnLongClickListener(view -> {
            try {
                iScreenOff.closeAndExit();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, R.string.service_closed, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        });
        Switch aSwitch = findViewById(R.id.screenoff_switch);
        aSwitch.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (isExpand) {
            findViewById(R.id.iv).performClick();
        } else
            finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (isExpand) {
            Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.key_pressed), KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", ""), keyCode), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (!isServiceOK) return true;
        Switch aSwitch = findViewById(R.id.screenoff_switch);
        if (keyCode == scrOffKey) aSwitch.setChecked(true);
        if (keyCode == scrOnKey) aSwitch.setChecked(false);
        return true;
    }


    private final Shizuku.OnRequestPermissionResultListener RL = (requestCode, grantResult) -> check();


    //检查Shizuku权限，申请Shizuku权限的函数
    private void check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        if (!isPermissionResultListenerRegistered) {
            Shizuku.addRequestPermissionResultListener(RL);
            isPermissionResultListenerRegistered = true;
        }
        boolean b = true, c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else c = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, "shizuku未运行", Toast.LENGTH_SHORT).show();
            }
        }
        if (b && c) {
            try {
                Process p = Shizuku.newProcess(new String[]{"sh"}, null, null);
                OutputStream out = p.getOutputStream();
                out.write(("sh " + getExternalFilesDir(null).getPath() + "/starter.sh\nexit\n").getBytes());
                out.flush();
                out.close();
            } catch (IOException ioException) {
                Toast.makeText(this, "激活失败", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.left).setVisibility(View.VISIBLE);
            findViewById(R.id.right).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.left).setVisibility(View.GONE);
            findViewById(R.id.right).setVisibility(View.GONE);
        }
        super.onConfigurationChanged(newConfig);
    }

    //一些收尾工作，取消注册监听器什么的
    @Override
    protected void onDestroy() {
        if (isPermissionResultListenerRegistered) Shizuku.removeRequestPermissionResultListener(RL);
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    public void finish(View view) {
        finish();
    }

    public void help() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.help_title)
                .setMessage(R.string.help_conntent)
                .setNegativeButton(R.string.understand, null)
                .show();
    }

    public void showActivate() {
        unzipFiles();
        final String command = "sh " + getExternalFilesDir(null).getPath() + "/starter.sh";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(String.format(getString(R.string.active_steps), command))
                .setTitle(R.string.need_active)
                .setNeutralButton(R.string.copy_cmd, (dialogInterface, i) -> {
                    ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", "adb shell " + command));
                    Toast.makeText(MainActivity.this, String.format(getString(R.string.cmd_copy_finish), command), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.by_root, (dialoginterface, i) -> {
                    Process p;
                    try {
                        p = Runtime.getRuntime().exec("su");
                        DataOutputStream o = new DataOutputStream(p.getOutputStream());
                        o.writeBytes(command);
                        o.flush();
                        o.close();
                    } catch (IOException ignored) {
                        Toast.makeText(MainActivity.this, R.string.active_failed, Toast.LENGTH_SHORT).show();
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            builder.setPositiveButton(R.string.by_shizuku, (dialogInterface, i) -> check());
        builder.show();
    }

    private void unzipFiles() {

        String file1 = getExternalFilesDir(null).getPath() + "/starter.sh";
        try {
            InputStream is = getAssets().open("starter.sh");
            FileOutputStream fileOutputStream = new FileOutputStream(file1);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = is.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            is.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ignored) {
        }
        String file2 = getExternalFilesDir(null).getPath() + "/ScreenController.dex";
        try {
            ZipFile zipFile = new ZipFile(getPackageResourcePath());
            // 遍历zip文件中的所有条目
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // 如果条目名称为classes.dex，则解压该条目到指定目录
                if (entry.getName().equals("classes.dex")) {
                    InputStream inputStream = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(file2);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    break;
                }
            }

            // 关闭ZipFile对象
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}