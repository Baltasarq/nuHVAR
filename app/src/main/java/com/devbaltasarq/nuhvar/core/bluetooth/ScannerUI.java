package com.devbaltasarq.nuhvar.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

public interface ScannerUI {
    void startScanning();
    void stopScanning();
    void onDeviceFound(BluetoothDevice btDevice);
    void addDeviceToListView(BluetoothDevice btDevice);
    void denyAdditionToList(BluetoothDevice btDevice);
    void filteringFinished();
    Context getContext();
}
