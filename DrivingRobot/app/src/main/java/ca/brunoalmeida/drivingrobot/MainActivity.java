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
import android.os.AsyncTask;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /** The default UUID for communicating with the Bluetooth module. */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // For requesting permissions
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;

    /** The name of the Bluetooth module. */
    private static final String EXPECTED_BLUETOOTH_DEVICE_NAME = "HC-05";

    /**
     * The prefix used for received Bluetooth messages intended for this Android device.
     * This is used to disregard the other messages Arduino sends to serial output for logging purposes.
     */
    private static final String bluetoothReadMessagePrefix = "BL: ";

    /** Whether Bluetooth is set up and ready to communicate. */
    private boolean isBluetoothAvailable = false;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice hc05 = null;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream bluetoothOutputStream = null;
    private InputStream bluetoothInputStream = null;

    // Interface elements
    TextView mainText;
    TextView distance;


    /**
     * If in debug mode, asserts that the expression is true.
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

    private void showToast(String message, int length) {
        Toast.makeText(this, message, length).show();
    }

    private void showShortToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    private void showLongToast(String message) {
        showToast(message, Toast.LENGTH_LONG);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get interface elements
        mainText = (TextView) findViewById(R.id.main_text);
        distance = (TextView) findViewById(R.id.distance);

        // Register for Bluetooth-related broadcasts
        registerBluetoothBroadcastReceiver();

        // Set up Bluetooth
        checkBluetoothAvailability();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();

        // Unregister from Bluetooth-related broadcasts
        unregisterReceiver(bluetoothBroadcastReceiver);

        // Close the Bluetooth socket
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.v(TAG, "Closed Bluetooth socket");
            } catch (IOException exception) {
                Log.e(TAG, "Failed to close Bluetooth socket");
                Log.e(TAG, exception.toString());
            }
        }
    }

    /**
     * Registers {@link #bluetoothBroadcastReceiver} for Bluetooth-related broadcasts.
     */
    private void registerBluetoothBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothBroadcastReceiver, filter);
    }

    /**
     * Receives Bluetooth-related broadcasts.
     */
    BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BroadcastReceiver.onReceive()");

            Log.v(TAG, intent.toString());

            // If a Bluetooth device has been found
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {

                // Get the device information
                final BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                Log.v(TAG, "Device: " + device.getName() + ", " + device.getAddress());

                // If the device is the Bluetooth module we are searching for
                if (device.getName().equals(EXPECTED_BLUETOOTH_DEVICE_NAME)) {
                    hc05 = device;

                    // Establish the Bluetooth connections and input/output streams
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                bluetoothSocket = hc05.createRfcommSocketToServiceRecord(MY_UUID);
                                Log.v(TAG, "Created Bluetooth socket");

                                bluetoothAdapter.cancelDiscovery();
                                Log.v(TAG, "Cancelled discovery");

                                bluetoothSocket.connect();
                                Log.v(TAG, "Connected to Bluetooth socket");

                                bluetoothOutputStream = bluetoothSocket.getOutputStream();
                                Log.v(TAG, "Obtained Bluetooth output stream");

                                bluetoothInputStream = bluetoothSocket.getInputStream();
                                Log.v(TAG, "Obtained Bluetooth input stream");

                                // Start receiving thread for input stream
                                readFromBluetoothThread.start();

                                // Write a test message to the output stream
                                writeToBluetooth("Hello World from Android!");

                            } catch (IOException exception) {
                                Log.e(TAG, "Failed to connect to Bluetooth device");
                                Log.e(TAG, exception.toString());

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainText.setText(
                                                "Failed to connect. Try restarting the Arduino.");
                                    }
                                });
                            }
                        }
                    });
                }

            // If successfully connected to a Bluetooth device
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainText.setText(
                                "Connected to Arduino" +
                                "\nName: " + hc05.getName() +
                                "\nAddress: " + hc05.getAddress());
                    }
                });

            // If disconnected from a Bluetooth device
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainText.setText("Disconnected from Arduino");
                    }
                });
            }
        }
    };


    /** Writes a message to the Bluetooth output stream. */
    private void writeToBluetooth(final String message) {
        if (bluetoothOutputStream != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Arduino will receive the message one character at a time,
                        // so add the "\0" delimiter to indicate the end of the message
                        bluetoothOutputStream.write((message + "\0").getBytes());
                        Log.v(TAG, "Bluetooth Write: " + message);
                    } catch (IOException exception) {
                        Log.e(TAG, "Failed to write to Bluetooth output");
                        Log.e(TAG, exception.toString());
                    }
                }
            });
        } else {
            Log.e(TAG, "Bluetooth output stream unavailable");
        }
    }


    /** A thread that runs forever to receive Bluetooth messages from the input stream. */
    Thread readFromBluetoothThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    // The complete message
                    String message = "";

                    // Message buffer
                    byte[] b = new byte[1];

                    // Read bytes until the end of the message (newline character)
                    while (b[0] != '\n') {
                        bluetoothInputStream.read(b);
                        if (b[0] != '\n') {
                            message += (char) b[0];
                        }
                    }

                    // If the message is intended for this device, handle it (removing the prefix)
                    if (message.startsWith(bluetoothReadMessagePrefix)) {
                        handleBluetoothRead(message.replaceFirst(bluetoothReadMessagePrefix, ""));
                    }

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    });

    /** Handles a received Bluetooth message and performs the appropriate action.
     * Currently, the application only reads distance measurements from Bluetooth input.
     */
    private void handleBluetoothRead(final String message) {
        Log.i(TAG, "Bluetooth Read: " + message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                distance.setText(message + "cm");
            }
        });
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
            mainText.setText("This device does not have Bluetooth hardware.");
        }
    }



    // Handles the enable Bluetooth request callback
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


    // Handles the coarse location permission request callback
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


    /** "Connect" button event handler */
    public void connectButtonTapped(View view) {
        Log.v(TAG, "connectButtonTapped()");

        if (isBluetoothAvailable) {
            startBluetoothDiscovery();
        } else {
            mainText.setText("Bluetooth is not available.");
            checkBluetoothAvailability();
        }
    }


    // Directional control event handlers
    public void stopButtonTapped(View view) {
        writeToBluetooth("S");
    }
    public void forwardButtonTapped(View view) {
        writeToBluetooth("F");
    }
    public void backwardButtonTapped(View view) {
        writeToBluetooth("B");
    }
    public void leftButtonTapped(View view) {
        writeToBluetooth("L");
    }
    public void rightButtonTapped(View view) {
        writeToBluetooth("R");
    }


}
