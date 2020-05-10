package com.deanlib.alarm;

public class AlarmStatus {

    boolean isWorking;

    public AlarmStatus(boolean isWorking) {
        this.isWorking = isWorking;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public void setWorking(boolean working) {
        isWorking = working;
    }
}
