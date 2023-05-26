package com.tile.screenoff;

interface IScreenOff {

    void setPowerMode(boolean turnOff);

    void updateNowScreenState(boolean isScreenOn);

    int getNowScreenState();

    void closeAndExit();

}