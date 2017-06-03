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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


// TODO - make socket.connect() non-blocking on a background thread
// TODO - use socket.getOutputStream().write() on a background thread, check on Arduino serial monitor
// TODO - implement some sort of string terminating character, e.g. \0 or \n
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean isBluetoothAvailable = false;

    TextView mainText;


    /**
     * In debug mode, asserts that the expression is true.
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

    private void showShortToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mainText = (TextView) findViewById(R.id.main_text);
        mainText.setText("DrivingRobot");

        registerBluetoothBroadcastReceiver();
        checkBluetoothAvailability();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();

        unregisterReceiver(bluetoothBroadcastReceiver);
    }

    /**
     * Receives Bluetooth-related broadcasts.
     */
    BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BroadcastReceiver.onReceive()");

            Log.v(TAG, intent.toString());

            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v(TAG, device.getName() + ", " + device.getAddress());
                mainText.setText(
                        "Found Bluetooth device:" +
                                "\nName: " + device.getName() +
                                "\nAddress: " + device.getAddress());

                try {
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    Log.v(TAG, "Created Bluetooth socket");

                    bluetoothAdapter.cancelDiscovery();
                    Log.v(TAG, "Cancelled discovery");

                    socket.connect();
                    Log.v(TAG, "Connected to Bluetooth socket");

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write("hello world".getBytes());
                    Log.v(TAG, "Wrote to output stream");

                    socket.close();
                    Log.v(TAG, "Closed Bluetooth socket");

                } catch (IOException exception) {
                    Log.e(TAG, "Failed to use Bluetooth socket");
                    Log.e(TAG, exception.toString());
                }
            }
        }
    };

    /**
     * Registers {@link #bluetoothBroadcastReceiver} for Bluetooth-related broadcasts.
     */
    private void registerBluetoothBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothBroadcastReceiver, filter);
    }

    /**
     * Updates {@link #isBluetoothAvailable} if the Bluetooth conditions are met.
     * Asks to enable Bluetooth or the coarse location permission if necessary.
     */
    private void checkBluetoothAvailability() {
        isBluetoothAvailable = false;

        // If Bluetooth hardware is available on the device
        if (bluetoothAdapter != null) {

            // If Bluetooth is enabled on the device
            if (bluetoothAdapter.isEnabled()) {

                // If the coarse location permission is granted
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    Log.v(TAG, "Bluetooth ready");
                    isBluetoothAvailable = true;

                } else {
                    Log.v(TAG, "Requesting coarse location permission");
                    ActivityCompat.requestPermissions(
                            this,
                            new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                            REQUEST_COARSE_LOCATION);
                }

            } else {
                Log.v(TAG, "Requesting to enable Bluetooth");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }

        } else {
            Log.e(TAG, "Bluetooth hardware unavailable");
            showShortToast("This device does not have Bluetooth hardware");
        }
    }



    // Enable Bluetooth request callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "User enabled Bluetooth");
                checkBluetoothAvailability();
            } else {
                Log.v(TAG, "User declined to enable Bluetooth");
            }
        } else {
            Log.v(TAG, "Unknown request code: " + requestCode + ", " + resultCode + ", " + data);
        }
    }


    // Coarse location permission request callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult()");

        if (requestCode == REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.v(TAG, "Coarse location permission granted");
                checkBluetoothAvailability();

            } else {
                Log.v(TAG, "Coarse location permission denied");
            }

        } else {
            Log.v(TAG, "Unknown request code: " + requestCode
                    + ", " + permissions + ", " + grantResults);
        }
    }



    /**
     * Starts the Bluetooth discovery process.
     */
    private void startBluetoothDiscovery() {
        Log.v(TAG, "startBluetooth()");

        if (isBluetoothAvailable) {
            if (bluetoothAdapter.startDiscovery()) {
                Log.v(TAG, "Starting Bluetooth discovery");
            } else {
                Log.v(TAG, "Failed to start Bluetooth discovery");
            }
        } else {
            Log.e(TAG, "Bluetooth not available");
        }
    }


    public void connectButtonTapped(View view) {
        Log.v(TAG, "connectButtonTapped()");

        if (isBluetoothAvailable) {
            startBluetoothDiscovery();
        } else {
            showShortToast("Bluetooth not available");
            checkBluetoothAvailability();
        }
    }

}
