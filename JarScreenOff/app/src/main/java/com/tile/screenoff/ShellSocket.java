package com.tile.screenoff;

import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.system.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ShellSocket {


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

    public static void main(String[] args) {
        int UID = Os.getuid();
        if (UID != 2000 && UID != 0) {
            System.err.println("Current User " + UID + " has no permission!");
            System.exit(90);
            return;
        }

        int PORT = 8090;
        List<String> array = Arrays.asList(args);
        if (array.contains("-p"))
            PORT = Integer.parseInt(array.get(array.indexOf("-p") + 1));
        if (array.contains("-port"))
            PORT = Integer.parseInt(array.get(array.indexOf("-port") + 1));


        try {
            boolean keep = true;
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Start Server(port " + PORT + ")!");
            while (keep) {
                Socket socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String inline1;
                while ((inline1 = bufferedReader.readLine()) != null) {
                    switch (inline1) {
                        case "off":
                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_OFF);
                            break;
                        case "on":
                            setDisplayPowerMode(getBuiltInDisplay(), POWER_MODE_NORMAL);
                            break;
                        case "exit":
                            keep = false;
                            break;
                    }
                }
                bufferedReader.close();
            }
            System.out.println("Stop Server!");
            serverSocket.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
