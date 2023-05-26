package com.tile.screenoff;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class BinderContainer implements Parcelable { // implements Parcelable 之后，这个类就可以作为intent的附加参数了。
    private final IBinder binder;

    public BinderContainer(IBinder binder) {
        this.binder = binder;
    }


    public IBinder getBinder() { //用这个函数来取出binder。其他函数都是自动生成的，只有这个函数是我自己写的。足以见得这个函数是多么的重要、多么的有技术含量
        return binder;
    }

    protected BinderContainer(Parcel in) {
        binder = in.readStrongBinder();
    }

    public static final Creator<BinderContainer> CREATOR = new Creator<BinderContainer>() {
        @Override
        public BinderContainer createFromParcel(Parcel in) {
            return new BinderContainer(in);
        }

        @Override
        public BinderContainer[] newArray(int size) {
            return new BinderContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(binder);
    }
}