package com.tile.screenoff;

import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

public class ScreenController {


    //发送广播需要的三个东西
    static boolean isBroadcastSent = false;
    static Intent intent = null;
    static Object iActivityManagerObj = null;


    public enum ScreenState {
        STATE_OFF, STATE_ON, STATE_SPECIAL
    }

    static ScreenState screenState = ScreenState.STATE_ON;


    private static final Class<?> CLASS;
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_NORMAL = 2;

    static {
        try {
            CLASS = Class.forName("android.view.SurfaceControl");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
    public static boolean useDisplayControl =
            Build.VERSION.SDK_INT >= 34;
    private static Method getBuiltInDisplayMethod;
    private static Method setDisplayPowerModeMethod;
    private static boolean listenVolumeKey = false;
    static Thread h1;
    static Process listenVolumeKeyProcess;

    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null)
            getBuiltInDisplayMethod = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? CLASS.getMethod("getBuiltInDisplay", int.class) : CLASS.getMethod("getInternalDisplayToken");
        return getBuiltInDisplayMethod;
    }

    public static IBinder getBuiltInDisplay() {
        try {
            // Change the power mode for all physical displays
            Log.e("getBuiltInDisplay","Android 14" + useDisplayControl);
            if (useDisplayControl) {
                long[] physicalDisplayIds = DisplayControl.getPhysicalDisplayIds();
                if (physicalDisplayIds == null) {
                    Log.e("getBuiltInDisplay", "Could not get physical display ids");
                    return null;
                }

                for (long physicalDisplayId : physicalDisplayIds) {
                    return DisplayControl.getPhysicalDisplayToken(
                            physicalDisplayId);

                }
            } else {
                Method method = getGetBuiltInDisplayMethod();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // call getBuiltInDisplay(0)
                    return (IBinder) method.invoke(null, 0);
                }

                // call getInternalDisplayToken()
                return (IBinder) method.invoke(null);
            }
        } catch(InvocationTargetException | IllegalAccessException | NoSuchMethodException e){
            Log.e("Could not invoke method", String.valueOf(e));
            return null;
        }
        return null;
    }

    private static Method getSetDisplayPowerModeMethod() throws NoSuchMethodException {
        if (setDisplayPowerModeMethod == null)
            setDisplayPowerModeMethod = CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class);
        return setDisplayPowerModeMethod;
    }

    public static void setDisplayPowerMode(IBinder displayToken, int mode) {
        try {
            Method method = getSetDisplayPowerModeMethod();
            method.invoke(null, displayToken, mode);
        } catch (InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException ignored) {
        }
    }

    public static void main(String[] args) {

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        Log.e("getBuiltInDisplay", "Android 14" + useDisplayControl);
        //检查权限
        int uid = android.os.Process.myUid();
        if (uid != 0 && uid != 2000) {
            System.err.printf("Insufficient permission! Need to be launched by adb (uid 2000) or root (uid 0), but your uid is %d \n", uid);
            System.exit(-1);
            return;
        }

        System.out.println("Start ScreenController Service.");

        isBroadcastSent = sendBinderToAppByStickyBroadcast();//发送广播，将binder传给APP
        if (!isBroadcastSent) {
            System.err.println("Failed to send broadcast!");
            System.exit(-1);
            return;
        }

        //加入JVM异常关闭时的处理程序
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (isBroadcastSent) isBroadcastSent = !removeStickyBroadcast();
            }
        });

//        try {
//            Scanner scanner = new Scanner(System.in);
//            //用来保持进程不退出，同时如果用户输入exit则程序退出
//            String inline;
//            while ((inline = scanner.nextLine()) != null) {
//                if (inline.equals("exit"))
//                    break;
//            }
//            scanner.close();
//        } catch (Exception unused) {
//            //用户使用nohup命令启动，scanner捕捉不到任何输入,会抛出异常。
//            while (true) ;
//        }

        Looper.loop();


        if (isBroadcastSent) isBroadcastSent = !removeStickyBroadcast();
        System.out.println("Stop ScreenController Service.\n");
        System.exit(0);
    }

    private static void stopListenVolumeKey() {
        listenVolumeKey = false;
        if (Build.VERSION.SDK_INT >= 26) {
            listenVolumeKeyProcess.destroyForcibly();
        } else {
            listenVolumeKeyProcess.destroy();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                h1.interrupt();
            }
        }, 2000);
    }

    private static void startListenVolumeKey() {
        if (listenVolumeKey) return;
        listenVolumeKey = true;
        h1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listenVolumeKeyProcess = Runtime.getRuntime().exec("getevent");
                    BufferedReader mReader = new BufferedReader(new InputStreamReader(listenVolumeKeyProcess.getInputStream()));
                    String inline;
                    while ((inline = mReader.readLine()) != null) {
                        if (!listenVolumeKey) break;
                        if (inline.endsWith("0000 0000 00000000")) continue;
                        if (inline.endsWith("0001 0072 00000001") || inline.endsWith("0001 0073 00000001")) {

                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_NORMAL);
                        }
                    }
                    mReader.close();
                    listenVolumeKeyProcess.waitFor();
                    if (Build.VERSION.SDK_INT >= 26) {
                        listenVolumeKeyProcess.destroyForcibly();
                    } else {
                        listenVolumeKeyProcess.destroy();
                    }
                } catch (Exception ignored) {
                }
            }
        });
        h1.start();
    }

    private static boolean sendBinderToAppByStickyBroadcast() {

        try {
            //生成binder
            IBinder binder = new IScreenOff.Stub() {
                @Override
                public void setPowerMode(boolean turnOff) throws RemoteException {
                    if (turnOff) {
                        if (screenState == ScreenState.STATE_ON) {
                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_OFF);
//                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_NORMAL);
                            screenState = ScreenState.STATE_SPECIAL;
                        }
                    } else {
                        if (screenState == ScreenState.STATE_SPECIAL) {
                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_NORMAL);
                            screenState = ScreenState.STATE_ON;
                        }
                    }
                }

                @Override
                public int getNowScreenState() throws RemoteException {
                    return screenState.ordinal();
                }

                @Override
                public void updateNowScreenState(boolean isScreenOn) throws RemoteException {
                    screenState = isScreenOn ? ScreenState.STATE_ON : ScreenState.STATE_OFF;
                }

                @Override
                public void closeAndExit() throws RemoteException {
                    if (isBroadcastSent) isBroadcastSent = !removeStickyBroadcast();
                    System.out.println("Stop ScreenController Service.\n");
                    System.exit(0);
                }

            };

            //把binder填到一个可以用Intent来传递的容器中
            BinderContainer binderContainer = new BinderContainer(binder);
            // 创建 Intent 对象，并将binder作为附加参数
            intent = new Intent("intent.screenoff.sendBinder");
            intent.putExtra("binder", binderContainer);

            // 获取 IActivityManager 类
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                iActivityManagerObj = Class.forName("android.app.IActivityManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class).invoke(null, "activity"));
            } else {
                Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
                Method getDefaultMethod = activityManagerNativeClass.getMethod("getDefault");
                iActivityManagerObj = getDefaultMethod.invoke(activityManagerNativeClass);
            }

            // 获取 broadcastIntent 方法
            Method broadcastIntentMethod = Class.forName("android.app.IActivityManager").getDeclaredMethod(
                    "broadcastIntent",
                    IApplicationThread.class,
                    Intent.class,
                    String.class,
                    IIntentReceiver.class,
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
            );
            // 调用 broadcastIntent 方法，发送粘性广播
            broadcastIntentMethod.invoke(
                    iActivityManagerObj,
                    null,
                    intent,
                    null,
                    null,
                    -1,
                    null,
                    null,
                    null,
                    0,
                    null,
                    false,
                    true,
                    -1
            );

        } catch (Exception e) {

            return false;
        }
        return true;
    }

    private static boolean removeStickyBroadcast() {
        if (intent == null || iActivityManagerObj == null) return true;

        try {
            // 获取 unbroadcastIntent 方法
            Method unbroadcastIntentMethod = Class.forName("android.app.IActivityManager").getDeclaredMethod("unbroadcastIntent", IApplicationThread.class, Intent.class, int.class);
            // 调用 broadcastIntent 方法，发送粘性广播
            unbroadcastIntentMethod.invoke(iActivityManagerObj, null, intent, -1);
            intent = null;
            iActivityManagerObj = null;
        } catch (Exception e) {
            System.err.println("Failed to remove broadcast!");
            return false;
        }
        return true;
    }
}
