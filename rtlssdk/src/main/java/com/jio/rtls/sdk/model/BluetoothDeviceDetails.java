package com.jio.rtls.sdk.model;

public class BluetoothDeviceDetails {

    private String deviceName;
    private String deviceAddress;
    private int deviceRSSI;

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

    public int getDeviceRSSI() {
        return deviceRSSI;
    }

    public void setDeviceRSSI(int deviceRSSI) {
        this.deviceRSSI = deviceRSSI;
    }
}
