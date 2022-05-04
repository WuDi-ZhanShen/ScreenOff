package com.tile.screenoff;

public class UserService extends IUserService.Stub {

    @Override
    public void destroy() {
    }

    @Override
    public void exit() {
    }

    @Override
    public void ScreenOff(boolean B) {
        SurfaceControl.setDisplayPowerMode(SurfaceControl.getBuiltInDisplay(), B ? SurfaceControl.POWER_MODE_OFF : SurfaceControl.POWER_MODE_NORMAL);
    }

}
