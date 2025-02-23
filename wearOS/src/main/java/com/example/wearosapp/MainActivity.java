package com.example.wearosapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener {
    private static final String TAG = "WearOSSensorApp";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int MAX_RETRIES = 3;  // Maximum number of retries
    private int retryCount = 0;

    private boolean isBluetoothConnected = false;

    private TextView statusText;
    private TextView sensorDataTextView;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor accelerometerSensor;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private Handler mainHandler;


//    Request Permissions Based on the Android Version Application Runs on
    private final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? new String[]{ // For Android 12 and above
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BODY_SENSORS
    }
            : new String[]{ // For Android 11 and below
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BODY_SENSORS
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        statusText = findViewById(R.id.statusText);
        sensorDataTextView = findViewById(R.id.sensorDataTextView);

        if (statusText == null || sensorDataTextView == null) {
            Log.e(TAG, "TextViews are null. Check layout XML.");
            return;
        }

        showStatus("Starting application...");

        if (checkAndRequestPermissions()) {
            showStatus("Permissions granted, initializing...");
            initializeApp();
        } else {
            showStatus("Requesting permissions...");
        }
    }

    private boolean checkAndRequestPermissions() {
        boolean allPermissionsGranted = true;

        // Loop through the permissions and check if each one is granted
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                Log.d(TAG, "Permission not granted: " + permission);
                break;
            }
        }

        if (!allPermissionsGranted) {
            // Request permissions dynamically
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d(TAG, "Permission denied: " + permissions[i]);
                    break;
                }
            }

            if (allGranted) {
                showStatus("All permissions granted, initializing...");
                initializeApp();
            } else {
                showStatus("Required permissions not granted!");
                finish();
            }
        }
    }

    private void initializeApp() {
        showStatus("Initializing sensors...");
        initializeSensors();

        showStatus("Initializing Bluetooth...");
        initializeBluetooth();
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (heartRateSensor == null) {
            showStatus("Heart rate sensor not available!");
        } else {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor == null) {
            showStatus("Accelerometer not available!");
        } else {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        showStatus("Sensors initialized successfully");
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showStatus("Bluetooth not supported on this device!");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showStatus("Please enable Bluetooth!");
            return;
        }

        showStatus("Starting Bluetooth connection...");
        new Thread(this::connectToDevice).start();
    }

    private void connectToDevice() {
        if (retryCount >= MAX_RETRIES) {
            showStatus("Max retries reached. Connection failed.");
            return; // Stop trying after max retries
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                showStatus("Bluetooth connect permission not granted!");
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.isEmpty()) {
                showStatus("No paired devices found! Please pair a device first.");
                return;
            }

            showStatus("Found " + pairedDevices.size() + " paired devices.");

            BluetoothDevice device = pairedDevices.iterator().next(); // Pick the first device
            showStatus("Connecting to: " + device.getName());

            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();

            showStatus("Connected to: " + device.getName());
            isBluetoothConnected = true;

        } catch (IOException e) {
            String errorMsg = "Connection failed: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showStatus(errorMsg);

            retryCount++;  // Increment retry count
            // Retry after a delay (2 seconds)
            new Handler(Looper.getMainLooper()).postDelayed(this::connectToDevice, 2000);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        try {
            String data = "";
            String displayText = "";

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                data = String.format("HR:%.1f", event.values[0]);
                displayText = "Heart Rate: " + event.values[0] + " BPM";
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                data += String.format("ACC:%.2f,%.2f,%.2f",
                        event.values[0], event.values[1], event.values[2]);
                displayText += String.format("\nAccelerometer: X=%.2f, Y=%.2f, Z=%.2f",
                        event.values[0], event.values[1], event.values[2]);
            } else {
                return;
            }

            updateSensorDataOnUI(displayText);

            if (outputStream == null) {
            showStatus("Bluetooth connection not established. Data not sent.");
                return;
            }

            outputStream.write(data.getBytes());
            outputStream.flush();

        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
            showStatus("Error sending data: " + e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed: " + sensor.getName() + " - " + accuracy);
    }

    private void showStatus(final String message) {
        Log.d(TAG, message);
        mainHandler.post(() -> { statusText.setText(message);
//            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSensorDataOnUI(final String displayText) {
        mainHandler.post(() -> sensorDataTextView.setText(displayText));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showStatus("Cleaning up...");

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }
}
