package com.sample.hrv;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import com.sample.hrv.sensor.BleSensor;


/**
 * Created by steven on 9/3/13.
 * Modified by olli on 3/28/2014.
 */
public class BluetoothGattExecutor extends BluetoothGattCallback {

    public interface ServiceAction {
        ServiceAction NULL = new ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                // it is null action. do nothing.
                return true;
            }
        };

        /***
         * Executes action.
         * @param bluetoothGatt - Android BLE Support
         * @return true - if action was executed instantly. false if action is waiting for
         *         feedback.
         */
        boolean execute(BluetoothGatt bluetoothGatt);
    }

    private final LinkedList<BluetoothGattExecutor.ServiceAction> queue = new LinkedList<>();
    private volatile ServiceAction currentAction;

    void update(final BleSensor sensor) {
        queue.add(sensor.update());
    }

    void enable(BleSensor sensor, boolean enable) {
        final ServiceAction[] actions = sensor.enable(enable);
        Collections.addAll(this.queue, actions);
    }

    void execute(BluetoothGatt gatt) {
        if (currentAction != null)
            return;

        boolean next = !queue.isEmpty();
        while (next) {
            final BluetoothGattExecutor.ServiceAction action = queue.pop();
            currentAction = action;
            if (!action.execute(gatt))
                break;

            currentAction = null;
            next = !queue.isEmpty();
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            queue.clear();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        currentAction = null;
        execute(gatt);
    }
}
