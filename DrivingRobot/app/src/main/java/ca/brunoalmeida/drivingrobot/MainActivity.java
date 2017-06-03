package ca.brunoalmeida.drivingrobot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * Receives Bluetooth action broadcasts.
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BroadcastReceiver.onReceive()");

            Log.v(TAG, intent.toString());

            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v(TAG, device.getName() + ", " + device.getAddress());
                mainText.setText("Found Bluetooth device:" + "\nName: " + device.getName() + "\nAddress: " + device.getAddress());

                try {
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    Log.v(TAG, "Created Bluetooth socket");

                    bluetoothAdapter.cancelDiscovery();
                    socket.connect();
                    Log.v(TAG, "Connected to Bluetooth socket");

                    socket.close();
                    Log.v(TAG, "Closed Bluetooth socket");

                } catch (IOException exception) {
                    Log.e(TAG, "Failed to use Bluetooth socket");
                    Log.e(TAG, exception.toString());
                }
            }
        }
    };


    TextView mainText;


    /**
     * In debug mode, asserts that the expression is true.
     * @param expression Expected to be true
     */
    private static void debugAssert(boolean expression) {
        if (BuildConfig.DEBUG && !expression) {
            throw new AssertionError();
        }
    }

    /**
     * Asserts a failure.
     */
    private static void debugAssertFail() {
        debugAssert(false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mainText = (TextView) findViewById(R.id.main_text);
        mainText.setText("DrivingRobot");

        // Check permission
        checkCoarseLocationPermission();

        // Register receiver for Bluetooth broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);

        // Check if Bluetooth is available and enabled
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                // Available and enabled
                startBluetooth();
            } else {
                // Available but not enabled
                // Request the user to turn on Bluetooth
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
            }
        } else {
            Log.e(TAG, "Bluetooth unavailable");
        }
    }

    /**
     * Checks if the app has the coarse location permission, and asks the user if it does not.
     */
    protected void checkCoarseLocationPermission() {
        Log.v(TAG, "checkCoarseLocationPermission()");

        // If the permission is not granted, request it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_COARSE_LOCATION);
        }
    }

    // Coarse location permission request callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {

                // If the permission was granted
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBluetooth();
                    Log.v(TAG, "Success");

                } else {
                    Log.v(TAG, "Fail");
                }

                break;
            }
        }
    }

    // Enable Bluetooth request callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult()");

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            // If Bluetooth is enabled
            if (resultCode == RESULT_OK) {
                startBluetooth();
            } else {
                Log.v(TAG, "User declined to enable Bluetooth");
            }
        } else {
            debugAssertFail();
        }
    }

    /**
     * Starts the Bluetooth discovery process.
     */
    private void startBluetooth() {
        Log.v(TAG, "startBluetooth()");

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.startDiscovery()) {
                Log.v(TAG, "Starting Bluetooth discovery");
            } else {
                Log.v(TAG, "Failed to start Bluetooth discovery");
            }
        } else {
            debugAssertFail();
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");

        super.onDestroy();

        unregisterReceiver(broadcastReceiver);
    }

}
