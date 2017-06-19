/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.hrv;


import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.sample.hrv.R;
import com.sample.hrv.adapters.BleDevicesAdapter;

import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private BleDevicesAdapter leDeviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_scan, menu);
        if (bluetoothLeScanner == null) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).
                    setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                leDeviceListAdapter.clear();
                if (bluetoothLeScanner == null) {
                    bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    bluetoothLeScanner.startScan(mLeScanCallback);

                    invalidateOptionsMenu();
                }
                break;
            case R.id.menu_stop:
                if (bluetoothLeScanner != null) {
                    bluetoothLeScanner.stopScan(mLeScanCallback);
                    bluetoothLeScanner = null;

                    invalidateOptionsMenu();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                init();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(mLeScanCallback);
            bluetoothLeScanner = null;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = leDeviceListAdapter.getDevice(position);
        if (device == null)
            return;

        final Intent intent = new Intent(this, DeviceServicesActivity.class);
        intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivity(intent);
    }

    private void init() {
        if (leDeviceListAdapter == null) {
            leDeviceListAdapter = new BleDevicesAdapter(getBaseContext());
            setListAdapter(leDeviceListAdapter);
        }

        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(mLeScanCallback);
        }

        invalidateOptionsMenu();
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    leDeviceListAdapter.addDevice(result.getDevice(),
                                                  result.getRssi());
                    leDeviceListAdapter.notifyDataSetChanged();
                }
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult result : results) {
                        leDeviceListAdapter.addDevice(result.getDevice(),
                                                      result.getRssi());
                        leDeviceListAdapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onScanFailed(int errorCode) {
                    Context context = getApplicationContext();
                    CharSequence text = "Error during BLE scan!";
                    int duration = Toast.LENGTH_SHORT;

                    Toast.makeText(context, text, duration).show();
                }
            };
//
//    private static class Scanner extends Thread {
//        private final BluetoothAdapter bluetoothAdapter;
//        private final BluetoothLeScanner bluetoothLeScanner;
//        private final ScanCallback mLeScanCallback;
//
//        private volatile boolean isScanning = false;
//
//        Scanner(BluetoothAdapter adapter,
//                BluetoothLeScanner scanner,
//                BluetoothAdapter.LeScanCallback callback) {
//            bluetoothAdapter = adapter;
//            bluetoothLeScanner = scanner;
//            mLeScanCallback = callback;
//        }
//
//        boolean isScanning() {
//            return isScanning;
//        }
//
//        void startScanning() {
//            synchronized (this) {
//                isScanning = true;
//                start();
//            }
//        }
//
//        void stopScanning() {
//            synchronized (this) {
//                isScanning = false;
//                bluetoothLeScanner.stopScan(mLeScanCallback);
//            }
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    synchronized (this) {
//                        if (!isScanning)
//                            break;
//
//                        bluetoothAdapter.startLeScan(mLeScanCallback);
//                    }
//
//                    sleep(SCAN_PERIOD);
//
//                    synchronized (this) {
//                        bluetoothAdapter.stopLeScan(mLeScanCallback);
//                    }
//                }
//            } catch (InterruptedException ignore) {
//            } finally {
//                bluetoothAdapter.stopLeScan(mLeScanCallback);
//            }
//        }
//    }
}