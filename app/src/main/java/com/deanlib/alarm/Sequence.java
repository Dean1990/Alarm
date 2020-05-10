package com.deanlib.alarm;

import android.os.Parcel;
import android.os.Parcelable;

public class Sequence implements Parcelable {

    private long[] data;//存一个序列，单位是秒
    private boolean isLoop;
    private boolean isRing;
    private boolean isVibration;

    public long[] getData() {
        return data;
    }

    public void setData(long[] data) {
        this.data = data;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean loop) {
        isLoop = loop;
    }

    public boolean isRing() {
        return isRing;
    }

    public void setRing(boolean ring) {
        isRing = ring;
    }

    public boolean isVibration() {
        return isVibration;
    }

    public void setVibration(boolean vibration) {
        isVibration = vibration;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLongArray(this.data);
        dest.writeByte(this.isLoop ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isRing ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isVibration ? (byte) 1 : (byte) 0);
    }

    public Sequence() {
    }

    protected Sequence(Parcel in) {
        this.data = in.createLongArray();
        this.isLoop = in.readByte() != 0;
        this.isRing = in.readByte() != 0;
        this.isVibration = in.readByte() != 0;
    }

    public static final Creator<Sequence> CREATOR = new Creator<Sequence>() {
        @Override
        public Sequence createFromParcel(Parcel source) {
            return new Sequence(source);
        }

        @Override
        public Sequence[] newArray(int size) {
            return new Sequence[size];
        }
    };
}
