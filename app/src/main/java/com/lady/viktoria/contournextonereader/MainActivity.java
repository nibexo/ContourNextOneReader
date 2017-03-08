package com.lady.viktoria.contournextonereader;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.lady.viktoria.contournextonereader.services.BGMeterGattService;
import com.lady.viktoria.contournextonereader.services.GattAttributes;

import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    Button btnAct;
    Button btnGet;
    TextView bgmac;
    Bundle b;
    public String deviceBTMAC;
    String mDeviceAddress = "00:00:00:00:00:00";
    BGMeterGattService mBGMeterGattService;
    private TextView mConnectionState;
    private boolean mConnected = false;
    private TextView mDataField;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBGMeterGattService = ((BGMeterGattService.LocalBinder) service).getService();
            if (!mBGMeterGattService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBGMeterGattService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBGMeterGattService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BGMeterGattService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BGMeterGattService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BGMeterGattService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BGMeterGattService.EXTRA_DATA));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAct = (Button) findViewById(R.id.listpaireddevices);
        btnGet = (Button) findViewById(R.id.buttonGet);
        bgmac = (TextView)findViewById(R.id.bgmac);
        btnAct.setOnClickListener(this);
        btnGet.setOnClickListener(this);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        Intent gattServiceIntent = new Intent(this, BGMeterGattService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mDataField = (TextView) findViewById(R.id.bgreading);

        try {
            b = getIntent().getExtras();
            deviceBTMAC = b.getString("BG Meter MAC Address");
            Log.v("deviceBTMAC", deviceBTMAC);
            bgmac.setText("BGMeter MAC: " + deviceBTMAC);
            mDeviceAddress = deviceBTMAC;
        }
        catch (Exception e) {
            Log.v("try_catch", "Error " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBGMeterGattService != null) {
            final boolean result = mBGMeterGattService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBGMeterGattService = null;
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BGMeterGattService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BGMeterGattService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BGMeterGattService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.listpaireddevices:
                Intent intent = new Intent (this, BGMeterActivity.class);
                startActivity (intent);
                break;
            case R.id.buttonGet:
                Log.e(TAG,"Result: \n\n\n\n");
                List<BluetoothGattCharacteristic> readCharacteristics =
                        mBGMeterGattService.readCharacteristics();

                Log.e(TAG,"Result2: \n\n\n\n" + readCharacteristics.size());
                for (BluetoothGattCharacteristic characteristic : readCharacteristics) {
                    if(characteristic.getUuid().equals(UUID.fromString(GattAttributes.BG_MEASUREMENT))) {
                        Log.e(TAG,"ResultChar: \n\n\n\n" + characteristic.getUuid());
                        GlucoseReadingRx gtb = new GlucoseReadingRx(characteristic.getValue());
                        Log.d(TAG,"Result: "+gtb.toString());

                    } else {
                        Log.w(TAG,"ResultChar: \n\n\n\n" + characteristic.getUuid());
                    }
                }

//                Log.e(TAG,"Result2: \n\n\n\n"+readCharacteristics.get(1).getValue());
//                readCharacteristics.get(1).getValue();
//                GlucoseReadingRx gtb = new GlucoseReadingRx(readCharacteristics.get(0).getValue());
//                Log.d(TAG,"Result: "+gtb.toString());


//                intent.putExtra(EXTRA_DATA, gtb.toStringFormatted());
            default:
                break;
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }
}