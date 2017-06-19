package com.sample.hrv.sensor;

/**
 * Created by user on 19/06/2017.
 */

public class HRData {
    final float HR;
    final float HRI;

    HRData(float HR, float HRI){
        this.HR = HR;
        this.HRI = HRI;
    }

    public float getHR() {
        return HR;
    }

    public float getHRI() {
        return HRI;
    }
}
