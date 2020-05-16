package com.deanlib.alarm;

public class DownCount {

    private long num;
    private long loopTimes;//重复次数
    private int position;//列表的第几项

    public DownCount(long num, int position) {
        this.num = num;
        this.position = position;
    }

    public DownCount(long num, long loopTimes, int position) {
        this.num = num;
        this.loopTimes = loopTimes;
        this.position = position;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }

    public long getLoopTimes() {
        return loopTimes;
    }

    public void setLoopTimes(long loopTimes) {
        this.loopTimes = loopTimes;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
