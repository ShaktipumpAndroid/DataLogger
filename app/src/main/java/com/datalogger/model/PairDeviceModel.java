package com.datalogger.model;

public class PairDeviceModel {
    String deviceName, deviceAddress;
    boolean isConnected;

    public PairDeviceModel(String deviceName, String deviceAddress, boolean isConnected) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.isConnected = isConnected;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
