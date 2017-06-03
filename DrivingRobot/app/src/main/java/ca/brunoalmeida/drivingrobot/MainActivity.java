package ca.brunoalmeida.drivingrobot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ENABLE_BLUETOOTH_REQUEST = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;

    BluetoothAdapter bluetoothAdapter;



    private static void debugAssert(boolean result) {
        if (BuildConfig.DEBUG && !result) {
            throw new AssertionError();
        }
    }

    private static void debugAssertFail() {
        debugAssert(false);
    }






    protected void checkLocationPermission() {
        Log.v(TAG, "checkLocationPermission()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //proceedDiscovery(); // --->
                    Log.v(TAG, "Success");
                } else {
                    //TODO re-request
                    Log.v(TAG, "Fail");
                }
                break;
            }
        }
    }






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        checkLocationPermission();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            bluetoothAdapter = this.getSystemService(Context.BLUETOOTH_SERVICE);
//        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(R.layout.activity_main);


        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothStuff();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
            }
        } else {
            Log.e(TAG, "Bluetooth not available");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult()");

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                bluetoothStuff();
            } else {
                Log.e(TAG, "User declined to enable Bluetooth");
            }
        } else {
            debugAssertFail();
        }
    }

    private void bluetoothStuff() {
        Log.v(TAG, "bluetoothStuff()");

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
//            BluetoothProfile profile = null;
//            BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
//                @Override
//                public void onServiceConnected(int profile, BluetoothProfile proxy) {
//                    Log.v(TAG, "onServiceConnected()");
//                    Log.v(TAG, "profile = " + profile + "proxy = " + proxy);
//                }
//
//                @Override
//                public void onServiceDisconnected(int profile) {
//                    Log.v(TAG, "onServiceDisconnected()");
//                    Log.v(TAG, "profile = " + profile);
//                }
//            };
//
//            bluetoothAdapter.getProfileProxy(this, listener, BluetoothProfile.GATT);




            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v(TAG, "BroadcastReceiver.onReceive()");
                    Log.v(TAG, context.toString());
                    Log.v(TAG, intent.toString());

                    if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.v(TAG, device.getName());
                        Log.v(TAG, device.getAddress());
                        Log.v(TAG, device.toString());
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(broadcastReceiver, filter);






            Log.i(TAG, Boolean.toString(bluetoothAdapter.startDiscovery()));
            //bluetoothAdapter.getRemoteDevice()


        } else {
            // does it show it stops here?
            debugAssertFail();
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");

        super.onDestroy();

        //unregisterReceiver(broadcastReceiver);
    }
}
