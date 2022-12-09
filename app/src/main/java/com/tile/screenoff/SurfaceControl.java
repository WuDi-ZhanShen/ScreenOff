package com.tile.screenoff;

import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SurfaceControl {

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

    private static Method getBuiltInDisplayMethod;
    private static Method setDisplayPowerModeMethod;



    private  SurfaceControl() {

    }

    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null)
            getBuiltInDisplayMethod = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? CLASS.getMethod("getBuiltInDisplay", int.class) : CLASS.getMethod("getInternalDisplayToken");
        return getBuiltInDisplayMethod;
    }

    public static IBinder getBuiltInDisplay() {

        try {
            Method method = getGetBuiltInDisplayMethod();
            // call getBuiltInDisplay(0)
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? (IBinder) method.invoke(null, 0) : (IBinder) method.invoke(null);
            // call getInternalDisplayToken()
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            return null;
        }
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
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
        }
    }



}